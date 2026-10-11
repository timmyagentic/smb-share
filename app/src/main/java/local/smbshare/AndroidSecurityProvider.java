package local.smbshare;

import com.hierynomus.security.Mac;
import com.hierynomus.security.SecurityException;
import com.hierynomus.security.bc.BCSecurityProvider;
import com.hierynomus.security.jce.JceSecurityProvider;

/** Use the platform for the measured AES-CMAC hotspot; preserve other algorithms. */
public final class AndroidSecurityProvider extends BCSecurityProvider {
 private final JceSecurityProvider platform=new JceSecurityProvider();
 private volatile boolean cmacUnavailable;
 @Override public Mac getMac(String algorithm){
  if("AesCmac".equals(algorithm)&&!cmacUnavailable){
   try{return platform.getMac(algorithm);}
   catch(SecurityException unsupported){cmacUnavailable=true;}
  }
  return super.getMac(algorithm);
 }
}
