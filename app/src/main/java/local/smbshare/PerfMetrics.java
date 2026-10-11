package local.smbshare;

import android.os.SystemClock;
import android.util.Log;
import com.hierynomus.security.*;
import com.hierynomus.security.SecurityException;
import com.hierynomus.security.bc.BCSecurityProvider;
import com.hierynomus.security.jce.JceSecurityProvider;
import com.hierynomus.smb.*;
import com.hierynomus.mssmb2.*;
import com.hierynomus.mssmb2.messages.SMB2WriteRequest;
import com.hierynomus.protocol.transport.*;
import com.hierynomus.smbj.*;
import com.hierynomus.smbj.transport.*;
import com.hierynomus.smbj.transport.tcp.direct.DirectTcpTransportFactory;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import javax.crypto.spec.GCMParameterSpec;
import org.json.JSONObject;

/** Diagnostic builds only: aggregate sizes/times; never records data or credentials. */
final class PerfMetrics {
 final long allocatedStarted=runtimeStat("art.gc.bytes-allocated"),gcStarted=runtimeStat("art.gc.gc-count"),gcTimeStarted=runtimeStat("art.gc.gc-time");
 final long started=now(),cpuStarted=android.os.Process.getElapsedCpuTime();
 int peakInFlight,signedWrites;long submitNs,waitNs;
 long connectNs,authNs,shareNs,openNs,readNs,writeNs,finishNs,uiNs,bytes,maxJavaBytes;
 String dialect="",cipher="",outcome="failed",macBackend="",macAlgorithm="",jceMacAvailable="";
 int maxWrite;boolean encrypted,serverRequiresSigning;
 final AtomicLong cryptoNs=new AtomicLong(),cryptoBytes=new AtomicLong(),transportNs=new AtomicLong(),macNs=new AtomicLong();
 final Map<String,String> availableMacs=Collections.synchronizedMap(new HashMap<>());
 final Map<Integer,Integer> writes=new TreeMap<>(),credits=new TreeMap<>();
 static long runtimeStat(String key){try{return Long.parseLong(android.os.Debug.getRuntimeStat(key));}catch(Exception ignored){return -1;}}
 static long runtimeDelta(String key,long start){long end=runtimeStat(key);return start<0||end<0?-1:end-start;}
 static long now(){return SystemClock.elapsedRealtimeNanos();}
 void heap(){Runtime r=Runtime.getRuntime();maxJavaBytes=Math.max(maxJavaBytes,r.totalMemory()-r.freeMemory());}
 SecurityProvider provider(){
  SecurityProvider bc=new BCSecurityProvider(),jce=new JceSecurityProvider(),nativeProvider=new AndroidSecurityProvider();
  return new SecurityProvider(){
   public MessageDigest getDigest(String n)throws SecurityException{return bc.getDigest(n);}
   public Mac getMac(String n)throws SecurityException{
    macAlgorithm=n;String available=availableMacs.get(n);
    if(available==null){available="";try{available=javax.crypto.Mac.getInstance(n).getProvider().getName();}catch(java.security.GeneralSecurityException ignored){}availableMacs.put(n,available);}
    jceMacAvailable=available;boolean useJce=BuildConfig.PERF_NATIVE&&!available.isEmpty();Mac selected=useJce?nativeProvider.getMac(n):bc.getMac(n);
    macBackend=useJce?"JCE:"+available:"BC-direct";
    final Mac d=selected;
    return new Mac(){
     public void init(byte[] k)throws SecurityException{long t=now();try{d.init(k);}finally{macNs.addAndGet(now()-t);}}
     public void update(byte b){d.update(b);}
     public void update(byte[] b){update(b,0,b.length);}
     public void update(byte[] b,int o,int n){long t=now();try{d.update(b,o,n);}finally{macNs.addAndGet(now()-t);}}
     public byte[] doFinal(){long t=now();try{return d.doFinal();}finally{macNs.addAndGet(now()-t);}}
     public void reset(){d.reset();}
    };
   }
   public Cipher getCipher(String n)throws SecurityException{return bc.getCipher(n);}
   public DerivationFunction getDerivationFunction(String n)throws SecurityException{return bc.getDerivationFunction(n);}
   public AEADBlockCipher getAEADBlockCipher(String n)throws SecurityException{
    AEADBlockCipher delegate;
    if(BuildConfig.PERF_NATIVE&&n.equals("AES/GCM/NoPadding"))delegate=nativeProvider.getAEADBlockCipher(n);else delegate=bc.getAEADBlockCipher(n);
    final AEADBlockCipher d=delegate;
    return new AEADBlockCipher(){
     public void init(Cipher.CryptMode m,byte[] k,GCMParameterSpec p)throws SecurityException{long t=now();try{d.init(m,k,p);}finally{cryptoNs.addAndGet(now()-t);}}
     public void updateAAD(byte[] b,int o,int n)throws SecurityException{long t=now();try{d.updateAAD(b,o,n);}finally{cryptoNs.addAndGet(now()-t);}}
     public byte[] update(byte[] b,int o,int n)throws SecurityException{long t=now();try{return d.update(b,o,n);}finally{cryptoNs.addAndGet(now()-t);cryptoBytes.addAndGet(n);}}
     public byte[] doFinal(byte[] b,int o,int n)throws SecurityException{long t=now();try{return d.doFinal(b,o,n);}finally{cryptoNs.addAndGet(now()-t);cryptoBytes.addAndGet(n);}}
     public void reset(){d.reset();}
    };
   }
  };
 }
 TransportLayerFactory<SMBPacketData<?>,SMBPacket<?,?>> transport(){
  return (handlers,config)->{
   if(BuildConfig.PERF_PREALLOC)handlers=new PacketHandlers<>(new WritePacketSerializer(),handlers.getReceiver(),handlers.getPacketFactory());
   TransportLayer<SMBPacket<?,?>> delegate=new DirectTcpTransportFactory<SMBPacketData<?>,SMBPacket<?,?>>().createTransportLayer(handlers,config);
   return new TransportLayer<SMBPacket<?,?>>(){
    public void write(SMBPacket<?,?> packet)throws TransportException{
     if(packet instanceof SMB2Packet){SMB2Packet p=((SMB2Packet)packet).getPacket();if(p instanceof SMB2WriteRequest){int credit=p.getCreditsAssigned();int size=Math.min(p.getMaxPayloadSize(),credit*65536);writes.put(size,writes.getOrDefault(size,0)+1);credits.put(credit,credits.getOrDefault(credit,0)+1);}}
     long t=now();try{delegate.write(packet);}finally{transportNs.addAndGet(now()-t);if(packet instanceof SMB2Packet){SMB2Packet p=((SMB2Packet)packet).getPacket();if(p instanceof SMB2WriteRequest&&p.getHeader().isFlagSet(SMB2MessageFlag.SMB2_FLAGS_SIGNED))signedWrites++;}}
    }
    public void connect(InetSocketAddress a)throws IOException{delegate.connect(a);}
    public void disconnect()throws IOException{delegate.disconnect();}
    public boolean isConnected(){return delegate.isConnected();}
   };
  };
 }
 JSONObject mapJson(Map<Integer,Integer> map)throws org.json.JSONException{JSONObject j=new JSONObject();for(Map.Entry<Integer,Integer> e:map.entrySet())j.put(e.getKey().toString(),e.getValue());return j;}
 void report(android.content.Context context){
  try{
   heap();JSONObject j=new JSONObject();
   j.put("run_started_ns",started);j.put("allocated_bytes",runtimeDelta("art.gc.bytes-allocated",allocatedStarted));j.put("gc_count",runtimeDelta("art.gc.gc-count",gcStarted));j.put("gc_time_ms",runtimeDelta("art.gc.gc-time",gcTimeStarted));j.put("preallocated_serialization",BuildConfig.PERF_PREALLOC);j.put("chunk",BuildConfig.PERF_CHUNK);j.put("window",BuildConfig.PERF_WINDOW);j.put("native",BuildConfig.PERF_NATIVE);j.put("outcome",outcome);
   j.put("signed_writes",signedWrites);j.put("submit_ms",submitNs/1e6);j.put("wait_ms",waitNs/1e6);j.put("peak_in_flight",peakInFlight);j.put("total_ms",(now()-started)/1e6);j.put("process_cpu_ms",android.os.Process.getElapsedCpuTime()-cpuStarted);j.put("bytes",bytes);j.put("connect_ms",connectNs/1e6);j.put("auth_ms",authNs/1e6);j.put("share_ms",shareNs/1e6);j.put("open_ms",openNs/1e6);j.put("source_read_ms",readNs/1e6);j.put("smb_write_ms",writeNs/1e6);j.put("finish_ms",finishNs/1e6);j.put("ui_ms",uiNs/1e6);j.put("mac_bulk_ms",macNs.get()/1e6);j.put("mac_backend",macBackend);j.put("mac_algorithm",macAlgorithm);j.put("jce_mac_available",jceMacAvailable);j.put("aead_ms",cryptoNs.get()/1e6);j.put("aead_bytes",cryptoBytes.get());j.put("transport_ms",transportNs.get()/1e6);j.put("max_java_used_bytes",maxJavaBytes);j.put("dialect",dialect);j.put("cipher",cipher);j.put("encrypted",encrypted);j.put("server_requires_signing",serverRequiresSigning);j.put("max_write",maxWrite);j.put("write_sizes",mapJson(writes));j.put("credit_charges",mapJson(credits));
   java.io.File directory=context.getExternalFilesDir(null);
   if(directory!=null)try(java.io.FileWriter writer=new java.io.FileWriter(new java.io.File(directory,"smb-perf.json"))){writer.write(j.toString());}
   Log.i("SmbPerf",j.toString());
  }catch(Exception e){Log.e("SmbPerf","Report error: "+e.getClass().getSimpleName());}
 }
}
