package local.smbshare;

import android.app.Instrumentation;
import android.app.Activity;
import android.os.Bundle;
import com.hierynomus.security.*;
import com.hierynomus.security.bc.BCSecurityProvider;
import javax.crypto.spec.GCMParameterSpec;
import java.util.Arrays;
import java.util.Random;

/** Public RFC vectors and synthetic data only; no application configuration is read. */
public class CryptoInstrumentation extends Instrumentation {
 @Override public void onCreate(Bundle arguments){super.onCreate(arguments);start();}
 @Override public void onStart(){
  Bundle result=new Bundle();
  try{runChecks();result.putString("result","PASS RFC4493 four vectors, segmented CMAC cross-provider, AES-GCM interoperability and corrupt-tag rejection");finish(Activity.RESULT_OK,result);}
  catch(Throwable error){result.putString("result","FAIL "+error.getClass().getSimpleName()+": "+error.getMessage());finish(Activity.RESULT_CANCELED,result);}
 }
 static byte[] hex(String value){byte[] b=new byte[value.length()/2];for(int i=0;i<b.length;i++)b[i]=(byte)Integer.parseInt(value.substring(i*2,i*2+2),16);return b;}
 static void equal(byte[] a,byte[] b){if(!Arrays.equals(a,b))throw new AssertionError("Cryptographic output mismatch");}
 static byte[] cmac(SecurityProvider provider,byte[] key,byte[] message,int segment)throws Exception{
  Mac mac=provider.getMac("AesCmac");mac.init(key);
  for(int pos=0;pos<message.length;pos+=segment)mac.update(message,pos,Math.min(segment,message.length-pos));return mac.doFinal();
 }
 static byte[] aead(SecurityProvider provider,Cipher.CryptMode mode,byte[] key,byte[] nonce,byte[] aad,byte[] input)throws Exception{
  AEADBlockCipher cipher=provider.getAEADBlockCipher("AES/GCM/NoPadding");cipher.init(mode,key,new GCMParameterSpec(128,nonce));cipher.updateAAD(aad,0,aad.length);return cipher.doFinal(input,0,input.length);
 }
 static void runChecks()throws Exception{
  SecurityProvider nativeProvider=new AndroidSecurityProvider(),bc=new BCSecurityProvider();
  byte[] key=hex("2b7e151628aed2a6abf7158809cf4f3c");
  byte[] message=hex("6bc1bee22e409f96e93d7e117393172aae2d8a571e03ac9c9eb76fac45af8e5130c81c46a35ce411e5fbc1191a0a52eff69f2445df4f9b17ad2b417be66c3710");
  int[] lengths={0,16,40,64};String[] expected={"bb1d6929e95937287fa37d129b756746","070a16b46b4d4144f79bdd9dd04a287c","dfa66747de9ae63030ca32611497c827","51f0bebf7e3b9d92fc49741779363cfe"};
  for(int i=0;i<lengths.length;i++)equal(hex(expected[i]),cmac(nativeProvider,key,Arrays.copyOf(message,lengths[i]),7));
  for(int length:new int[]{1,15,17,65537,1048576}){
   byte[] data=new byte[length];new Random(length).nextBytes(data);equal(cmac(bc,key,data,131072),cmac(nativeProvider,key,data,3333));
   byte[] nonce=new byte[12],aad={1,2,3,4,5};new Random(length+1).nextBytes(nonce);
   byte[] encrypted=aead(nativeProvider,Cipher.CryptMode.ENCRYPT,key,nonce,aad,data);equal(data,aead(bc,Cipher.CryptMode.DECRYPT,key,nonce,aad,encrypted));
   equal(encrypted,aead(bc,Cipher.CryptMode.ENCRYPT,key,nonce,aad,data));equal(data,aead(nativeProvider,Cipher.CryptMode.DECRYPT,key,nonce,aad,encrypted));
   encrypted[encrypted.length-1]^=1;boolean rejected=false;
   try{aead(nativeProvider,Cipher.CryptMode.DECRYPT,key,nonce,aad,encrypted);}catch(com.hierynomus.security.SecurityException invalid){rejected=true;}
   if(!rejected)throw new AssertionError("Corrupt authentication tag was accepted");
  }
 }
}
