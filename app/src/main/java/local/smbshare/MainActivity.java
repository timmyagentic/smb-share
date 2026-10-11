package local.smbshare;
import android.app.*;import android.content.*;import android.net.Uri;import android.os.*;import android.view.*;import android.view.inputmethod.InputMethodManager;import android.widget.*;import android.graphics.Color;import android.graphics.Typeface;import android.graphics.drawable.GradientDrawable;import java.util.*;
public class MainActivity extends Activity {
 final ArrayList<Uri> files=new ArrayList<>();String batch=UUID.randomUUID().toString();
 LinearLayout root;TextView status,summary;ProgressBar progress;Button send,cancel;boolean registered=false,autoPending=false;
 final BroadcastReceiver receiver=new BroadcastReceiver(){public void onReceive(Context c,Intent i){refresh();maybeAutoUpload();}};
 @Override public void onCreate(Bundle b){
  super.onCreate(b);
  if(b==null){
   readIntent(getIntent());
   String action=getIntent().getAction();
   autoPending=!files.isEmpty()&&(Intent.ACTION_SEND.equals(action)||Intent.ACTION_SEND_MULTIPLE.equals(action));
  }else{
   ArrayList<Uri> restored=b.getParcelableArrayList("files");if(restored!=null)files.addAll(restored);
   batch=b.getString("batch",batch);autoPending=b.getBoolean("autoPending",false);
  }
  build();
 }
 @Override protected void onSaveInstanceState(Bundle state){
  super.onSaveInstanceState(state);state.putParcelableArrayList("files",files);state.putString("batch",batch);state.putBoolean("autoPending",autoPending);
 }
 @Override protected void onPostResume(){super.onPostResume();maybeAutoUpload();}
 void maybeAutoUpload(){
  if(autoPending&&!files.isEmpty()&&Vault.prefs(this).contains("secret")&&!UploadService.running){
   autoPending=false;startUpload();
  }
 }
 void readIntent(Intent i){
  files.clear();batch=UUID.randomUUID().toString();
  if(Intent.ACTION_SEND.equals(i.getAction()))add(i.getParcelableExtra(Intent.EXTRA_STREAM));
  if(Intent.ACTION_SEND_MULTIPLE.equals(i.getAction())){ArrayList<Uri> list=i.getParcelableArrayListExtra(Intent.EXTRA_STREAM);if(list!=null)for(Uri u:list)add(u);}
  if(files.isEmpty()&&i.getClipData()!=null)for(int n=0;n<i.getClipData().getItemCount();n++)add(i.getClipData().getItemAt(n).getUri());
  if(!files.isEmpty()&&!UploadService.running){UploadService.message="";UploadService.detail="";UploadService.percent=0;}
 }
 void add(Uri u){if(u!=null&&"content".equals(u.getScheme())&&!files.contains(u))files.add(u);}
 int dp(int x){return (int)(x*getResources().getDisplayMetrics().density);}
 TextView label(String text,int size,boolean bold){TextView t=new TextView(this);t.setText(text);t.setTextSize(size);t.setTextColor(Color.rgb(28,48,42));if(bold)t.setTypeface(null,Typeface.BOLD);t.setPadding(0,dp(8),0,dp(8));root.addView(t);return t;}
 Button button(String text,Runnable action){Button b=new Button(this);b.setText(text);b.setAllCaps(false);root.addView(b,new LinearLayout.LayoutParams(-1,dp(54)));b.setOnClickListener(v->action.run());return b;}
 void build(){
  ScrollView scroll=new ScrollView(this);root=new LinearLayout(this);root.setOrientation(LinearLayout.VERTICAL);root.setPadding(dp(24),dp(24),dp(24),dp(24));root.setBackgroundColor(Color.rgb(245,247,245));scroll.addView(root);setContentView(scroll);
  scroll.setOnApplyWindowInsetsListener((v,in)->{if(Build.VERSION.SDK_INT>=30){android.graphics.Insets x=in.getInsets(WindowInsets.Type.systemBars());v.setPadding(x.left,x.top,x.right,x.bottom);}else{v.setPadding(in.getSystemWindowInsetLeft(),in.getSystemWindowInsetTop(),in.getSystemWindowInsetRight(),in.getSystemWindowInsetBottom());}return in;});
  label("SMB 分享",30,true);label("把文件，直接送到 Mac。",16,false);
  summary=label("",16,true);status=label("",16,false);progress=new ProgressBar(this,null,android.R.attr.progressBarStyleHorizontal);root.addView(progress,new LinearLayout.LayoutParams(-1,dp(6)));
  send=button("发送到 Mac",()->startUpload());cancel=button("取消传输",()->{UploadService.cancelled=true;status.setText("正在取消…");});
  button("选择文件",()->{if(UploadService.running)return;Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);startActivityForResult(i,8);});
  button("连接设置",()->settings());
  label("文件通过局域网直传 · 同名自动改名\n不会覆盖已有文件 · 不需要电脑客户端",13,false);
  refresh();if(!Vault.prefs(this).contains("secret"))settings();
 }
 void refresh(){if(summary==null)return;summary.setText(files.isEmpty()?"在其他 App 中选择文件 → 分享 → SMB 分享":"已选择 "+files.size()+" 个文件\n目标："+Vault.prefs(this).getString("share","尚未设置"));status.setText(UploadService.message.isEmpty()?"准备就绪":UploadService.message+"\n"+UploadService.detail);progress.setProgress(UploadService.percent);send.setEnabled(!files.isEmpty()&&!UploadService.running);cancel.setVisibility(UploadService.running?View.VISIBLE:View.GONE);}
 // Older Android uses a signature permission instead of API 33 receiver flags.
 @android.annotation.SuppressLint("UnspecifiedRegisterReceiverFlag")
 @Override protected void onStart(){super.onStart();if(Build.VERSION.SDK_INT>=33)registerReceiver(receiver,new IntentFilter(UploadService.UPDATE),Context.RECEIVER_NOT_EXPORTED);else registerReceiver(receiver,new IntentFilter(UploadService.UPDATE),"local.smbshare.INTERNAL",null);registered=true;refresh();}
 @Override protected void onStop(){if(registered){unregisterReceiver(receiver);registered=false;}super.onStop();}
 @Override protected void onActivityResult(int req,int result,Intent data){super.onActivityResult(req,result,data);if(req==8&&result==RESULT_OK&&data!=null){autoPending=false;files.clear();batch=UUID.randomUUID().toString();add(data.getData());if(data.getClipData()!=null)for(int n=0;n<data.getClipData().getItemCount();n++)add(data.getClipData().getItemAt(n).getUri());UploadService.message="";refresh();}}
 EditText field(LinearLayout form,String hint,String value,boolean secret){TextView title=new TextView(this);title.setText(hint);form.addView(title);EditText e=new EditText(this);e.setSingleLine(true);e.setText(value);e.setInputType(secret?129:1);e.setImportantForAutofill(View.IMPORTANT_FOR_AUTOFILL_NO);form.addView(e,new LinearLayout.LayoutParams(-1,dp(52)));return e;}
 void settings(){
  if(UploadService.running){Toast.makeText(this,"请等待传输完成",Toast.LENGTH_SHORT).show();return;}
  LinearLayout form=new LinearLayout(this);form.setOrientation(LinearLayout.VERTICAL);form.setPadding(dp(24),dp(8),dp(24),dp(8));
  android.content.SharedPreferences p=Vault.prefs(this);
  EditText host=field(form,"Mac 地址（不含 smb://）",p.getString("host",""),false);
  EditText share=field(form,"共享名称",p.getString("share","PhoneInbox"),false);
  EditText user=field(form,"用户名",p.getString("user",""),false);
  EditText password=field(form,"Mac 登录密码（加密保存在手机）","",true);password.setHint(p.contains("secret")?"留空保留已有密码":"请在手机上输入");
  password.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
  CheckBox showPassword=new CheckBox(this);showPassword.setText("显示密码");showPassword.setChecked(false);form.addView(showPassword);
  showPassword.setOnCheckedChangeListener((button,checked)->{
   int cursor=password.getSelectionEnd();
   password.setTransformationMethod(checked?null:android.text.method.PasswordTransformationMethod.getInstance());
   password.setSelection(Math.max(0,Math.min(cursor,password.length())));
  });
  AlertDialog d=new AlertDialog.Builder(this).setTitle("连接 Mac").setView(form).setNegativeButton("取消",null).setPositiveButton("保存",null).create();
  final EditText enteredPassword=password;
  d.setOnDismissListener(dialog->{enteredPassword.setText("");enteredPassword.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());});
  d.getWindow();d.setOnShowListener(v->{d.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);d.getButton(-1).setOnClickListener(w->{
   try{String h=host.getText().toString().trim(),s=share.getText().toString().trim(),u=user.getText().toString().trim();
    if(h.isEmpty()||h.contains("/")||h.contains(":")||s.isEmpty()||s.contains("/")||s.contains("\\")||u.isEmpty())throw new IllegalArgumentException();
    String pw=password.getText().toString();if(pw.isEmpty())pw=Vault.password(this);if(pw.isEmpty()){password.setError("请输入密码");return;}
    Vault.save(this,h,s,u,pw);password.setText("");d.dismiss();UploadService.message="";refresh();maybeAutoUpload();
   }catch(Exception e){Toast.makeText(this,"设置未保存，请检查输入",Toast.LENGTH_LONG).show();}
  });});d.show();
 }
 void startUpload(){
  if(files.isEmpty()||UploadService.running)return;
  autoPending=false;
  if(!Vault.prefs(this).contains("secret")){settings();return;}
  if(Build.VERSION.SDK_INT>=33&&checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)!=android.content.pm.PackageManager.PERMISSION_GRANTED)requestPermissions(new String[]{android.Manifest.permission.POST_NOTIFICATIONS},9);
  Intent request=UploadService.request(this,files,batch);
  if(ShareActivity.needsPage(this,files)){
   Intent page=new Intent(this,TransferActivity.class).putParcelableArrayListExtra("uris",files).putExtra("batch",batch).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
   page.setClipData(request.getClipData());startActivity(page);
  }else startForegroundService(request);
  send.setEnabled(false);
 }
}
