package local.smbshare;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import android.database.Cursor;
import android.provider.OpenableColumns;
import java.util.ArrayList;
import java.util.UUID;

/** Small shares hand off silently; large shares open the transfer page. */
public class ShareActivity extends Activity {
 private boolean handled;
 @Override public void onCreate(Bundle state){
  super.onCreate(state);
  handled=state!=null&&state.getBoolean("handled");
  if(handled){finish();return;}
  // Theme.NoDisplay must finish before onResume. Interactive setup has its own UI.
  if(!Vault.prefs(this).contains("secret")||(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=PackageManager.PERMISSION_GRANTED)){
   openSetup();return;
  }
  dispatch();
 }
 @Override protected void onSaveInstanceState(Bundle state){super.onSaveInstanceState(state);state.putBoolean("handled",handled);}
 private void add(ArrayList<Uri> files,Uri uri){if(uri!=null&&"content".equals(uri.getScheme())&&!files.contains(uri))files.add(uri);}
 private void dispatch(){
  handled=true;Intent source=getIntent();ArrayList<Uri> files=new ArrayList<>();
  if(Intent.ACTION_SEND.equals(source.getAction()))add(files,source.getParcelableExtra(Intent.EXTRA_STREAM));
  else if(Intent.ACTION_SEND_MULTIPLE.equals(source.getAction())){ArrayList<Uri> items=source.getParcelableArrayListExtra(Intent.EXTRA_STREAM);if(items!=null)for(Uri uri:items)add(files,uri);}
  if(files.isEmpty()&&source.getClipData()!=null)for(int n=0;n<source.getClipData().getItemCount();n++)add(files,source.getClipData().getItemAt(n).getUri());
  if(files.isEmpty()){Toast.makeText(this,"没有可发送的文件",Toast.LENGTH_SHORT).show();finish();return;}
  try{
   String batch=UUID.randomUUID().toString();
   if(needsPage(this,files)){
    Intent page=new Intent(source).setClass(this,TransferActivity.class).putParcelableArrayListExtra("uris",files).putExtra("batch",batch);
    page.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);page.setClipData(UploadService.request(this,files,batch).getClipData());startActivity(page);
   }else startForegroundService(UploadService.request(this,files,batch));
  }
  catch(RuntimeException e){Toast.makeText(this,"后台传输暂不可用，请在应用内重试",Toast.LENGTH_LONG).show();startActivity(new Intent(source).setClass(this,MainActivity.class));}
  finish();overridePendingTransition(0,0);
 }
 static boolean needsPage(android.content.Context context,ArrayList<Uri> files){
  long[] sizes=new long[files.size()];
  for(int n=0;n<files.size();n++){
   sizes[n]=-1;
   try(Cursor c=context.getContentResolver().query(files.get(n),new String[]{OpenableColumns.SIZE},null,null,null)){
    if(c!=null&&c.moveToFirst()&&!c.isNull(0))sizes[n]=c.getLong(0);
   }catch(RuntimeException ignored){} // Unknown sizes use the safer visible route.
  }
  return TransferPolicy.needsPage(sizes);
 }
 private void openSetup(){handled=true;startActivity(new Intent(getIntent()).setClass(this,MainActivity.class));finish();}
}
