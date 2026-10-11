package local.smbshare;

import java.io.*;
import java.util.ArrayDeque;
import java.util.concurrent.*;
import java.util.function.BooleanSupplier;
import java.util.function.LongConsumer;

/** Fixed buffer window, explicit offsets, and progress only for acknowledged writes. */
final class WindowedUploader {
 interface Writer {Future<Long> write(byte[] data,long offset,int length)throws IOException;}
 static final class Stats {long readNs,submitNs,waitNs,confirmed;int peakInFlight;}
 private static final class Pending {
  final byte[] data;final int length;final Future<Long> future;final long deadline;
  Pending(byte[] b,int n,Future<Long> f,long timeout){data=b;length=n;future=f;deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeout);}
 }
 static long copy(InputStream in,Writer writer,int chunk,int window,BooleanSupplier cancelled,LongConsumer progress,Stats stats)throws IOException{
  return copy(in,writer,chunk,window,cancelled,progress,stats,30000);
 }
 static long copy(InputStream in,Writer writer,int chunk,int window,BooleanSupplier cancelled,LongConsumer progress,Stats stats,long timeoutMs)throws IOException{
  if(chunk<=0||chunk>1024*1024||window<1||window>4||timeoutMs<=0)throw new IllegalArgumentException("Invalid upload window");
  ArrayDeque<byte[]> free=new ArrayDeque<>();ArrayDeque<Pending> pending=new ArrayDeque<>();
  int allocated=0;
  long submitted=0;boolean completed=false;
  try{
   boolean eof=false;
   while(!eof){
    checkCancelled(cancelled);
    if(free.isEmpty()&&allocated<window){free.add(new byte[chunk]);allocated++;}
    if(free.isEmpty())confirm(pending,free,cancelled,progress,stats);
    byte[] data=free.removeFirst();int used=0;
    while(used<data.length){
     checkCancelled(cancelled);long started=System.nanoTime();int n;
     try{n=in.read(data,used,data.length-used);}finally{stats.readNs+=System.nanoTime()-started;}
     if(n<0){eof=true;break;}
     if(n==0){started=System.nanoTime();int one;try{one=in.read();}finally{stats.readNs+=System.nanoTime()-started;}if(one<0){eof=true;break;}data[used++]=(byte)one;}
     else used+=n;
    }
    if(used>0){
     checkCancelled(cancelled);long started=System.nanoTime();Future<Long> future;
     try{future=writer.write(data,submitted,used);}finally{stats.submitNs+=System.nanoTime()-started;}
     pending.addLast(new Pending(data,used,future,timeoutMs));submitted+=used;stats.peakInFlight=Math.max(stats.peakInFlight,pending.size());
    }else free.addLast(data);
   }
   while(!pending.isEmpty())confirm(pending,free,cancelled,progress,stats);
   checkCancelled(cancelled);completed=true;return stats.confirmed;
  }finally{
   if(!completed)drain(pending,Math.min(timeoutMs,5000));
  }
 }
 private static void checkCancelled(BooleanSupplier cancelled)throws InterruptedIOException{
  if(cancelled.getAsBoolean()||Thread.currentThread().isInterrupted())throw new InterruptedIOException("Upload cancelled");
 }
 private static void confirm(ArrayDeque<Pending> pending,ArrayDeque<byte[]> free,BooleanSupplier cancelled,LongConsumer progress,Stats stats)throws IOException{
  Pending p=pending.getFirst();long started=System.nanoTime();
  try{
   for(;;){
    checkCancelled(cancelled);long remaining=p.deadline-System.nanoTime();if(remaining<=0)throw new IOException("SMB write acknowledgement timed out");
    try{
     long count=p.future.get(Math.min(remaining,TimeUnit.MILLISECONDS.toNanos(250)),TimeUnit.NANOSECONDS);
     if(count!=p.length)throw new IOException("Server acknowledged an incomplete write");
     pending.removeFirst();free.addLast(p.data);stats.confirmed+=count;progress.accept(stats.confirmed);return;
    }catch(TimeoutException e){/* Check cancellation and the total request deadline again. */}
    catch(ExecutionException e){throw new IOException("SMB async write failed",e.getCause());}
    catch(CancellationException e){throw new IOException("SMB async write cancelled",e);}
    catch(InterruptedException e){Thread.currentThread().interrupt();throw new InterruptedIOException("Upload interrupted");}
   }
  }finally{stats.waitNs+=System.nanoTime()-started;}
 }
 private static void drain(ArrayDeque<Pending> pending,long timeoutMs){
  // Settle requests before the caller closes/deletes its temporary file. Never rename here.
  long deadline=System.nanoTime()+TimeUnit.MILLISECONDS.toNanos(timeoutMs);
  for(Pending p:pending){
   try{long left=deadline-System.nanoTime();if(left>0&&!Thread.currentThread().isInterrupted())p.future.get(left,TimeUnit.NANOSECONDS);}
   catch(InterruptedException e){Thread.currentThread().interrupt();}
   catch(ExecutionException|TimeoutException|CancellationException ignored){}
   finally{try{if(!p.future.isDone())p.future.cancel(false);}catch(RuntimeException ignored){/* Preserve the original failure while closing the connection. */}}
  }
 }
}
