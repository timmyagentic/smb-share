package local.smbshare;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.*;import android.database.Cursor;import android.provider.OpenableColumns;
import java.io.*;import java.util.*;import java.util.concurrent.TimeUnit;
import com.hierynomus.smbj.*;import com.hierynomus.smbj.connection.Connection;import com.hierynomus.smbj.auth.AuthenticationContext;import com.hierynomus.smbj.session.Session;import com.hierynomus.smbj.share.DiskShare;import com.hierynomus.msdtyp.AccessMask;import com.hierynomus.msfscc.FileAttributes;import com.hierynomus.mssmb2.*;
public class UploadService extends Service {
 static final String UPDATE="local.smbshare.UPDATE";
 static volatile boolean running=false,cancelled=false;
 static volatile String message="", detail="";
 static volatile int percent=0;
 static final RetryHistory completed=new RetryHistory(8);
 static final class Snapshot {
  final String message,detail;final int percent;final boolean active,finished;
  Snapshot(String m,String d,int p,boolean a,boolean f){message=m;detail=d;percent=p;active=a;finished=f;}
 }
 private static final LinkedHashMap<String,Snapshot> snapshots=new LinkedHashMap<>();
 static synchronized Snapshot snapshot(String batch){return snapshots.get(batch);}
 static synchronized void remember(String batch,Snapshot state){
  snapshots.put(batch,state);
  // Never evict queued or active transfers.
  if(snapshots.size()>16){Iterator<Map.Entry<String,Snapshot>> it=snapshots.entrySet().iterator();while(it.hasNext()&&snapshots.size()>16)if(it.next().getValue().finished)it.remove();}
 }
 String activeBatch;
 NotificationManager nm;
 final ArrayDeque<Intent> queue=new ArrayDeque<>();
 @Override public IBinder onBind(Intent i){return null;}
 @Override public void onCreate(){super.onCreate();nm=getSystemService(NotificationManager.class);nm.createNotificationChannel(new NotificationChannel("upload","文件传输",NotificationManager.IMPORTANCE_LOW));nm.createNotificationChannel(new NotificationChannel("result","传输结果",NotificationManager.IMPORTANCE_DEFAULT));}
 Notification notification(){return new Notification.Builder(this,"upload").setSmallIcon(local.smbshare.R.drawable.ic_share).setContentTitle("SMB 分享 · "+message).setContentText(detail).setOngoing(running).setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,TransferActivity.class).putExtra("batch",activeBatch),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).build();}
 void update(String m,String d,int p){message=m;detail=d;percent=p;remember(activeBatch,new Snapshot(m,d,p,true,false));sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()));nm.notify(1,notification());}
 static Intent request(Context context,ArrayList<Uri> files,String batch){
  Intent i=new Intent(context,UploadService.class).putParcelableArrayListExtra("uris",files).putExtra("batch",batch).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
  ClipData clips=ClipData.newRawUri("files",files.get(0));for(int n=1;n<files.size();n++)clips.addItem(new ClipData.Item(files.get(n)));i.setClipData(clips);return i;
 }
 @Override public int onStartCommand(Intent i,int flags,int id){
  if(i==null)return START_NOT_STICKY;
  remember(i.getStringExtra("batch"),new Snapshot("等待传输","前面的文件完成后自动开始",0,false,false));queue.addLast(new Intent(i));if(!running)startNext();return START_NOT_STICKY;
 }
 void startNext(){
  Intent i=queue.removeFirst();activeBatch=i.getStringExtra("batch");
  running=true;cancelled=false;message="准备连接";detail="";percent=0;
  remember(activeBatch,new Snapshot(message,detail,0,true,false));startForeground(1,notification());
  ArrayList<Uri> uris=i.getParcelableArrayListExtra("uris");String batch=i.getStringExtra("batch");
  new Thread(()->upload(uris,batch),"smb-upload").start();
 }
 void resultNotification(){
  String title=percent==100?"已发送到 Mac":message;
  String text=percent==100?detail:detail+" 点击检查设置后重新分享。";
  Notification n=new Notification.Builder(this,"result").setSmallIcon(local.smbshare.R.drawable.ic_share)
   .setContentTitle(title).setContentText(text).setStyle(new Notification.BigTextStyle().bigText(text)).setAutoCancel(true)
   .setContentIntent(PendingIntent.getActivity(this,1,new Intent(this,MainActivity.class).setAction(Intent.ACTION_MAIN),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).build();
  nm.notify(2,n);
 }
 void upload(ArrayList<Uri> uris,String batch){
  try {
   if(uris==null||uris.isEmpty())throw new IOException("没有可上传的文件");
   android.content.SharedPreferences p=Vault.prefs(this);
   char[] password=Vault.password(this).toCharArray();
   SmbConfig config=SmbConfig.builder().withTimeout(30,TimeUnit.SECONDS).withSoTimeout(30,TimeUnit.SECONDS).build();
   try(SMBClient client=new SMBClient(config);Connection c=client.connect(p.getString("host",""))){
    Session session;
    try{session=c.authenticate(new AuthenticationContext(p.getString("user",""),password,""));}finally{Arrays.fill(password,'\0');}
    try(DiskShare share=(DiskShare)session.connectShare(p.getString("share",""))){
     int count=0;
     byte[] buf=new byte[128*1024]; // Reuse one bounded buffer for the whole batch.
     for(int fileIndex=0;fileIndex<uris.size();fileIndex++){
      Uri uri=uris.get(fileIndex);
      if(completed.contains(batch,fileIndex)){count++;continue;}
      if(cancelled)throw new InterruptedIOException();
      String name="file";long size=-1;
      try(Cursor cur=getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME,OpenableColumns.SIZE},null,null,null)){
       if(cur!=null&&cur.moveToFirst()){name=cur.getString(0);if(!cur.isNull(1))size=cur.getLong(1);}
      }
      name=Names.safe(name);final String temp=".smbshare-"+UUID.randomUUID()+".part";
      boolean renamed=false;
      try {
       try(InputStream in=getContentResolver().openInputStream(uri);
        com.hierynomus.smbj.share.File out=share.openFile(temp,EnumSet.of(AccessMask.GENERIC_WRITE,AccessMask.DELETE),EnumSet.of(FileAttributes.FILE_ATTRIBUTE_NORMAL),SMB2ShareAccess.ALL,SMB2CreateDisposition.FILE_CREATE,EnumSet.of(SMB2CreateOptions.FILE_NON_DIRECTORY_FILE))){
        if(in==null)throw new IOException("无法读取文件");
        long offset=0,last=0;int n;
        update("上传 "+(count+1)+" / "+uris.size(),name,0);
        while((n=in.read(buf))!=-1){
         if(cancelled)throw new InterruptedIOException();
         int done=0;while(done<n){long wrote=out.write(buf,offset,done,n-done);if(wrote<=0||wrote>n-done)throw new IOException("写入中断");done+=(int)wrote;offset+=wrote;}
         if(SystemClock.elapsedRealtime()-last>250){last=SystemClock.elapsedRealtime();update("上传 "+(count+1)+" / "+uris.size(),name+" · "+android.text.format.Formatter.formatShortFileSize(this,offset),size>0?(int)Math.min(99,offset*100/size):0);}
        }
        if(cancelled)throw new InterruptedIOException();
        for(int suffix=0;suffix<10000;suffix++){
         try{out.rename(Names.numbered(name,suffix),false);renamed=true;break;}
         catch(SMBApiException e){if(e.getStatusCode()!=0xC0000035L)throw e;}
        }
        if(!renamed)throw new IOException("同名文件过多");
       }
      }finally{if(!renamed)try{share.rm(temp);}catch(Exception ignored){}}
      completed.add(batch,fileIndex);count++;
     }
     update("传输完成",count+" 个文件已保存到 "+p.getString("share",""),100);
    }
   }finally{Arrays.fill(password,'\0');}
  }catch(Exception e){
   Throwable cause=e;for(int depth=0;cause!=null&&depth<6;depth++,cause=cause.getCause()){
    android.util.Log.e("SmbShare", "Failure type: "+cause.getClass().getName());
    for(StackTraceElement frame:cause.getStackTrace())android.util.Log.e("SmbShare", " at "+frame.toString());
   }
   String reason;
   if(cancelled||e instanceof InterruptedIOException)reason="已取消，可重试未完成的文件";
   else if(e instanceof SMBApiException)reason="SMB 状态："+((SMBApiException)e).getStatus()+"。请检查账户和共享权限。";
   else reason="请检查 Wi-Fi、Mac 是否唤醒、地址与密码；然后重试。";
   update(cancelled?"已取消":"传输失败",reason,0);
  }finally{
   // Publish idle only after resources are closed, serialized with onStartCommand.
   new Handler(getMainLooper()).post(()->{remember(activeBatch,new Snapshot(message,detail,percent,false,true));running=false;stopForeground(STOP_FOREGROUND_REMOVE);resultNotification();if(queue.isEmpty())stopSelf();else startNext();sendBroadcast(new Intent(UPDATE).setPackage(getPackageName()));});
  }
 }
 @Override public void onTimeout(int startId,int fgsType){cancelled=true;stopSelf();}
}
