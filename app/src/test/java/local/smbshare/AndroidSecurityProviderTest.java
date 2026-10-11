package local.smbshare;

import org.junit.Test;
import com.hierynomus.security.Mac;
import com.hierynomus.security.bc.BCSecurityProvider;
import static org.junit.Assert.*;

public class AndroidSecurityProviderTest {
 @Test public void rfc4493EmptyMessageMatchesWithPlatformOrFallback()throws Exception{
  byte[] key={(byte)0x2b,0x7e,0x15,0x16,0x28,(byte)0xae,(byte)0xd2,(byte)0xa6,(byte)0xab,(byte)0xf7,0x15,(byte)0x88,0x09,(byte)0xcf,0x4f,0x3c};
  Mac mac=new AndroidSecurityProvider().getMac("AesCmac");mac.init(key);byte[] result=mac.doFinal();StringBuilder hex=new StringBuilder();for(byte b:result)hex.append(String.format("%02x",b&255));assertEquals("bb1d6929e95937287fa37d129b756746",hex.toString());
 }
 @Test public void ccmKeepsTheExistingProviderForParameterCompatibility(){
  assertEquals(new BCSecurityProvider().getAEADBlockCipher("AES/CCM/NoPadding").getClass(),new AndroidSecurityProvider().getAEADBlockCipher("AES/CCM/NoPadding").getClass());
 }
}
