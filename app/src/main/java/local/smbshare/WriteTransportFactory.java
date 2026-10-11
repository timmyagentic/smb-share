package local.smbshare;

import com.hierynomus.smb.*;
import com.hierynomus.smbj.SmbConfig;
import com.hierynomus.smbj.transport.TransportLayerFactory;
import com.hierynomus.smbj.transport.tcp.direct.DirectTcpTransportFactory;
import com.hierynomus.protocol.transport.*;

final class WriteTransportFactory implements TransportLayerFactory<SMBPacketData<?>,SMBPacket<?,?>> {
 @Override public TransportLayer<SMBPacket<?,?>> createTransportLayer(PacketHandlers<SMBPacketData<?>,SMBPacket<?,?>> handlers,SmbConfig config){
  PacketHandlers<SMBPacketData<?>,SMBPacket<?,?>> sized=new PacketHandlers<>(new WritePacketSerializer(),handlers.getReceiver(),handlers.getPacketFactory());
  return new DirectTcpTransportFactory<SMBPacketData<?>,SMBPacket<?,?>>().createTransportLayer(sized,config);
 }
}
