package local.smbshare;

import com.hierynomus.protocol.transport.PacketSerializer;
import com.hierynomus.smb.SMBBuffer;
import com.hierynomus.smb.SMBPacket;
import com.hierynomus.mssmb2.SMB2Packet;
import com.hierynomus.mssmb2.messages.SMB2WriteRequest;
import com.hierynomus.smbj.connection.SMBPacketSerializer;

/** Reserve the negotiated write payload once; keep a distinct buffer for each request. */
final class WritePacketSerializer implements PacketSerializer<SMBPacket<?,?>,SMBBuffer> {
 private final SMBPacketSerializer defaults=new SMBPacketSerializer();
 @Override public SMBBuffer write(SMBPacket<?,?> packet){
  if(packet instanceof SMB2Packet){
   SMB2Packet original=((SMB2Packet)packet).getPacket();
   if(original instanceof SMB2WriteRequest){
    int payload=Math.min(original.getMaxPayloadSize(),original.getCreditsAssigned()*65536);
    // Unexpectedly larger packets still grow through SMBJ's normal Buffer behavior.
    SMBBuffer buffer=new SMBBuffer(new byte[Math.min(Math.max(0,payload),1024*1024)+256]);
    buffer.clear();packet.write(buffer);return buffer;
   }
  }
  return defaults.write(packet);
 }
}
