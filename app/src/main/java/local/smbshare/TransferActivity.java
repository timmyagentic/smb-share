package local.smbshare;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.ArrayList;

/** Visible companion to the service. It never restarts a transfer on recreation. */
public class TransferActivity extends Activity {
 String batch;
 TextView title,percentage,detail,notice;
 ProgressBar progress;
 Button cancel,close;
 boolean registered,failedToStart;
 final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){refresh();}};
 @Override public void onCreate(Bundle saved){
  super.onCreate(saved);
  batch=getIntent().getStringExtra("batch");
  failedToStart=saved!=null&&saved.getBoolean("failedToStart");
  if(saved!=null&&UploadService.snapshot(batch)==null&&!UploadService.running)failedToStart=true;
  build();
  if(saved==null){
   ArrayList<Uri> files=getIntent().getParcelableArrayListExtra("uris");
   if(files!=null&&!files.isEmpty()&&batch!=null&&UploadService.snapshot(batch)==null){
    try{startForegroundService(UploadService.request(this,files,batch));}
    catch(RuntimeException e){failedToStart=true;}
   }
  }
  if(Build.VERSION.SDK_INT>=33)getOnBackInvokedDispatcher().registerOnBackInvokedCallback(android.window.OnBackInvokedDispatcher.PRIORITY_DEFAULT,()->handleBack());
  refresh();
 }
 @Override protected void onSaveInstanceState(Bundle b){super.onSaveInstanceState(b);b.putBoolean("failedToStart",failedToStart);}
 int dp(int value){return (int)(value*getResources().getDisplayMetrics().density);}
 TextView text(LinearLayout parent,String value,int size,boolean bold){
  TextView v=new TextView(this);v.setText(value);v.setTextSize(size);v.setTextColor(Color.rgb(28,48,42));if(bold)v.setTypeface(null,Typeface.BOLD);v.setPadding(0,dp(10),0,dp(10));parent.addView(v);return v;
 }
 Button button(LinearLayout parent,String label){Button b=new Button(this);b.setText(label);b.setAllCaps(false);parent.addView(b,new LinearLayout.LayoutParams(-1,dp(56)));return b;}
 void build(){
  ScrollView scroll=new ScrollView(this);scroll.setFillViewport(true);
  LinearLayout root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(28),dp(28),dp(28),dp(24));root.setBackgroundColor(Color.rgb(245,247,245));scroll.addView(root);setContentView(scroll);
  scroll.setOnApplyWindowInsetsListener((v,in)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets x=in.getInsets(WindowInsets.Type.systemBars());v.setPadding(x.left,x.top,x.right,x.bottom);}else v.setPadding(in.getSystemWindowInsetLeft(),in.getSystemWindowInsetTop(),in.getSystemWindowInsetRight(),in.getSystemWindowInsetBottom());return in;});
  text(root,"SMB 分享 / 大文件传输",14,true);
  title=text(root,"正在准备传输",28,true);
  text(root,"当前文件进度",13,false);
  percentage=text(root,"连接中…",48,true);
  progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);progress.setMax(100);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(8)));
  detail=text(root,"正在读取文件并连接 Mac",16,false);
  notice=text(root,"请保持此页面在前台，不要锁屏\n传输期间此页面会自动保持屏幕亮起。切换应用或锁屏可能导致传输中断。",16,true);
  notice.setBackgroundColor(Color.rgb(227,239,230));notice.setPadding(dp(18),dp(18),dp(18),dp(18));
  text(root,"文件通过局域网直接发送到 Mac\n完成后即可离开此页面",13,false);
  root.addView(new Space(this),new LinearLayout.LayoutParams(1,0,1));
  cancel=button(root,"取消传输");cancel.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("取消此次传输？").setMessage("已经完成的文件会保留，正在传输的文件不会保存为完整文件。").setNegativeButton("继续传输",null).setPositiveButton("取消传输",(d,w)->{UploadService.Snapshot s=UploadService.snapshot(batch);if(s!=null&&s.active){UploadService.cancelled=true;cancel.setEnabled(false);cancel.setText("正在取消…");}}).show());
  close=button(root,"完成并返回");close.setOnClickListener(v->finish());
 }
 void refresh(){
  UploadService.Snapshot s=UploadService.snapshot(batch);
  boolean missing=s==null;
  boolean finished=failedToStart||(s!=null&&s.finished)||(missing&&!UploadService.running&&getIntent().getParcelableArrayListExtra("uris")==null);
  boolean success=s!=null&&s.finished&&s.percent==100;
  if(finished)getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  else getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
  title.setText(failedToStart?"无法开始传输":missing?(finished?"暂无传输记录":"正在准备传输"):s.message);
  detail.setText(failedToStart?"请返回后重新分享文件。":missing?(finished?"请从文件管理器重新分享文件。":"正在读取文件并连接 Mac"):s.detail);
  progress.setIndeterminate(!finished&&(missing||s.percent==0));progress.setProgress(s==null?0:s.percent);
  percentage.setText(success?"100%":finished?"已结束":missing||s.percent==0?"请稍候…":s.percent+"%");
  notice.setText(finished?(success?"文件已保存到 Mac\n传输已完成，可以锁屏或离开此页面。":"本次传输已结束\n可以离开此页面；如需重试，请重新分享文件。"):"请保持此页面在前台，不要锁屏\n传输期间此页面会自动保持屏幕亮起。切换应用或锁屏可能导致传输中断。");
  cancel.setVisibility(!finished&&s!=null&&s.active?View.VISIBLE:View.GONE);
  close.setVisibility(finished?View.VISIBLE:View.GONE);close.setText(success?"完成并返回":"返回");
 }
 @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
 @Override protected void onStart(){super.onStart();if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,new IntentFilter(UploadService.UPDATE),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,new IntentFilter(UploadService.UPDATE),"local.smbshare.INTERNAL",null);registered=true;refresh();}
 @Override protected void onStop(){if(registered){unregisterReceiver(receiver);registered=false;}super.onStop();}
 @android.annotation.SuppressLint("GestureBackNavigation") // API 33+ uses the native dispatcher registered above.
 @Override public void onBackPressed(){handleBack();}
 void handleBack(){
  UploadService.Snapshot s=UploadService.snapshot(batch);
  if(!failedToStart&&(s==null||!s.finished))new AlertDialog.Builder(this).setTitle("传输尚未完成").setMessage("请保持此页面在前台和屏幕亮起。现在离开可能导致传输中断。").setNegativeButton("继续传输",null).setPositiveButton("仍然离开",(d,w)->finish()).show();
  else finish();
 }
}
