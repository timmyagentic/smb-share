package local.smbshare;

import com.hierynomus.security.AEADBlockCipher;
import com.hierynomus.security.Mac;
import com.hierynomus.security.SecurityException;
import com.hierynomus.security.bc.BCSecurityProvider;
import com.hierynomus.security.jce.JceSecurityProvider;
import java.util.concurrent.ConcurrentHashMap;

/** Prefer the platform's implementation of the same algorithm; retain BC compatibility. */
public final class AndroidSecurityProvider extends BCSecurityProvider {
 private final JceSecurityProvider platform=new JceSecurityProvider();
 private final ConcurrentHashMap<String,Boolean> available=new ConcurrentHashMap<>();
 @Override public Mac getMac(String algorithm){
  String key="mac:"+algorithm;
  if(!Boolean.FALSE.equals(available.get(key))){
   try{Mac mac=platform.getMac(algorithm);available.put(key,true);return mac;}
   catch(SecurityException unsupported){available.put(key,false);}
  }
  return super.getMac(algorithm);
 }
 @Override public AEADBlockCipher getAEADBlockCipher(String algorithm){
  if(!"AES/GCM/NoPadding".equals(algorithm))return super.getAEADBlockCipher(algorithm);
  String key="aead:"+algorithm;
  if(!Boolean.FALSE.equals(available.get(key))){
   try{AEADBlockCipher cipher=platform.getAEADBlockCipher(algorithm);available.put(key,true);return cipher;}
   catch(SecurityException unsupported){available.put(key,false);}
  }
  return super.getAEADBlockCipher(algorithm);
 }
}
