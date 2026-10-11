package local.smbshare;

import org.junit.Test;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

public class WindowedUploaderTest {
 static class Sink implements WindowedUploader.Writer {
  final ByteArrayOutputStream received=new ByteArrayOutputStream();final List<Future<Long>> futures=new ArrayList<>();
  int inFlight,peak,submits;int shortAt=-1,failAt=-1;AtomicBoolean cancelAfterTwo;
  public Future<Long> write(byte[] data,long offset,int length){
   assertEquals(submits*4L,offset);final byte[] original=Arrays.copyOf(data,length);final int index=submits++;inFlight++;peak=Math.max(peak,inFlight);
   Future<Long> future=new Future<Long>(){
    boolean done;
    public Long get()throws ExecutionException{return get(1,TimeUnit.SECONDS);}
    public Long get(long timeout,TimeUnit unit)throws ExecutionException{
     if(!done){assertArrayEquals("In-flight buffer was reused",original,Arrays.copyOf(data,length));done=true;inFlight--;if(index!=failAt)received.write(original,0,original.length);}
     if(index==failAt)throw new ExecutionException(new IOException("Simulated SMB failure"));
     return (long)(index==shortAt?length-1:length);
    }
    public boolean cancel(boolean interrupt){done=true;return true;}public boolean isDone(){return done;}public boolean isCancelled(){return false;}
   };
   futures.add(future);if(submits==2&&cancelAfterTwo!=null)cancelAfterTwo.set(true);return future;
  }
 }
 @Test public void boundedWindowPreservesBuffersOffsetsAndFinalPartialChunk()throws Exception{
  byte[] input=new byte[39];new Random(2).nextBytes(input);Sink sink=new Sink();WindowedUploader.Stats stats=new WindowedUploader.Stats();List<Long> progress=new ArrayList<>();
  assertEquals(39,WindowedUploader.copy(new ByteArrayInputStream(input),sink,4,3,()->false,progress::add,stats));
  assertArrayEquals(input,sink.received.toByteArray());assertEquals(3,sink.peak);assertEquals(3,stats.peakInFlight);assertEquals(Long.valueOf(39),progress.get(progress.size()-1));assertEquals(0,sink.inFlight);
 }
 @Test public void shortAcknowledgementFailsAndSettlesOtherRequests()throws Exception{
  Sink sink=new Sink();sink.shortAt=0;try{WindowedUploader.copy(new ByteArrayInputStream(new byte[20]),sink,4,3,()->false,n->{},new WindowedUploader.Stats());fail();}catch(IOException expected){}
  assertEquals(0,sink.inFlight);assertTrue(sink.futures.stream().allMatch(Future::isDone));
 }
 @Test public void failedFutureNeverReturnsSuccessAndSettlesWindow()throws Exception{
  Sink sink=new Sink();sink.failAt=0;try{WindowedUploader.copy(new ByteArrayInputStream(new byte[20]),sink,4,3,()->false,n->{},new WindowedUploader.Stats());fail();}catch(IOException expected){assertNotNull(expected.getCause());}
  assertTrue(sink.futures.stream().allMatch(Future::isDone));
 }
 @Test public void cancellationStopsNewSubmissionsAndSettlesExisting()throws Exception{
  Sink sink=new Sink();AtomicBoolean cancel=new AtomicBoolean();sink.cancelAfterTwo=cancel;
  try{WindowedUploader.copy(new ByteArrayInputStream(new byte[40]),sink,4,3,cancel::get,n->{},new WindowedUploader.Stats());fail();}catch(InterruptedIOException expected){}
  assertEquals(2,sink.submits);assertEquals(0,sink.inFlight);
 }
 @Test public void sourceFailureDoesNotLeaveUnsettledWrites()throws Exception{
  Sink sink=new Sink();InputStream broken=new InputStream(){int count;public int read()throws IOException{if(count++==7)throw new IOException("Read failed");return 1;}public int read(byte[] b,int o,int n)throws IOException{for(int i=0;i<n;i++)b[o+i]=(byte)read();return n;}};
  try{WindowedUploader.copy(broken,sink,4,2,()->false,n->{},new WindowedUploader.Stats());fail();}catch(IOException expected){}
  assertEquals(0,sink.inFlight);
 }
 @Test public void acknowledgementsHaveDeadline()throws Exception{
  CompletableFuture<Long> never=new CompletableFuture<>();
  try{WindowedUploader.copy(new ByteArrayInputStream(new byte[4]),(b,o,n)->never,4,1,()->false,n->{},new WindowedUploader.Stats(),1);fail();}catch(IOException expected){}
  assertTrue(never.isCancelled());
 }
 @Test public void emptyInputCreatesNoRequests()throws Exception{Sink sink=new Sink();assertEquals(0,WindowedUploader.copy(new ByteArrayInputStream(new byte[0]),sink,4,2,()->false,n->{},new WindowedUploader.Stats()));assertEquals(0,sink.submits);}
}
