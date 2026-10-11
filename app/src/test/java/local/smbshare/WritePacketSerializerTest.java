package local.smbshare;

import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.mssmb2.*;
import com.hierynomus.mssmb2.messages.SMB2WriteRequest;
import com.hierynomus.smbj.connection.SMBPacketSerializer;
import com.hierynomus.smbj.io.ArrayByteChunkProvider;
import org.junit.Test;
import java.util.*;
import static org.junit.Assert.*;

public class WritePacketSerializerTest {
 SMB2WriteRequest request(byte[] bytes,int credits){
  SMB2WriteRequest p=new SMB2WriteRequest(SMB2Dialect.SMB_3_0_2,new SMB2FileId(new byte[8],new byte[8]),1,2,new ArrayByteChunkProvider(bytes,7),1048576);p.setCreditsAssigned(credits);return p;
 }
 @Test public void exactWireBytesMatchDefaultSerializerAcrossCreditLimits(){
  for(int length:new int[]{1,65536,131072,1048576})for(int credits:new int[]{1,2,16}){
   byte[] data=new byte[length];new Random(length).nextBytes(data);
   SMBBuffer expected=new SMBPacketSerializer().write(request(data,credits));
   SMBBuffer actual=new WritePacketSerializer().write(request(data,credits));
   assertArrayEquals(expected.getCompactData(),actual.getCompactData());assertTrue(actual.array().length<=1048576+256);
  }
 }
 @Test public void eachOutstandingRequestOwnsItsSerializedBuffer(){
  WritePacketSerializer serializer=new WritePacketSerializer();byte[] first=new byte[1048576];Arrays.fill(first,(byte)3);
  SMBBuffer buffer=serializer.write(request(first,16));byte[] snapshot=buffer.getCompactData();
  SMBBuffer second=serializer.write(request(new byte[1048576],16));assertNotSame(buffer.array(),second.array());assertArrayEquals(snapshot,buffer.getCompactData());
 }
}
