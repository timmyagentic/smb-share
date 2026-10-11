package local.smbshare;
import android.content.*;
import android.security.keystore.*;
import android.util.Base64;
import java.security.KeyStore;
import javax.crypto.*;
import javax.crypto.spec.GCMParameterSpec;
public class Vault {
 static javax.crypto.SecretKey key() throws Exception {
  KeyStore ks=KeyStore.getInstance("AndroidKeyStore"); ks.load(null);
  if(!ks.containsAlias("smb-password")){
   KeyGenerator g=KeyGenerator.getInstance("AES","AndroidKeyStore");
   g.init(new KeyGenParameterSpec.Builder("smb-password",KeyProperties.PURPOSE_ENCRYPT|KeyProperties.PURPOSE_DECRYPT).setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build());g.generateKey();
  }
  return (javax.crypto.SecretKey)ks.getKey("smb-password",null);
 }
 static SharedPreferences prefs(Context c){return c.getSharedPreferences("settings",0);}
 // Confirm persistence before reporting that connection settings are saved.
 @android.annotation.SuppressLint("ApplySharedPref")
 static void save(Context c,String host,String share,String user,String pass) throws Exception {
  Cipher x=Cipher.getInstance("AES/GCM/NoPadding");x.init(Cipher.ENCRYPT_MODE,key());
  if(!prefs(c).edit().putString("host",host).putString("share",share).putString("user",user).putString("iv",Base64.encodeToString(x.getIV(),2)).putString("secret",Base64.encodeToString(x.doFinal(pass.getBytes(java.nio.charset.StandardCharsets.UTF_8)),2)).commit())throw new java.io.IOException("Settings could not be saved");
 }
 static String password(Context c) throws Exception {
  SharedPreferences p=prefs(c);if(!p.contains("secret"))return "";
  Cipher x=Cipher.getInstance("AES/GCM/NoPadding");x.init(Cipher.DECRYPT_MODE,key(),new GCMParameterSpec(128,Base64.decode(p.getString("iv",""),2)));
  return new String(x.doFinal(Base64.decode(p.getString("secret",""),2)),java.nio.charset.StandardCharsets.UTF_8);
 }
}
