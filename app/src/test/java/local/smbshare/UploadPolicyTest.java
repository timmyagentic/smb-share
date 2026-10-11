package local.smbshare;
import org.junit.Test;
import static org.junit.Assert.*;
public class UploadPolicyTest {
 @Test public void smallFilesUseOneSmallBuffer(){assertEquals(65536,UploadPolicy.chunk(44,1048576));assertEquals(1,UploadPolicy.window(44,65536,4));assertEquals(1,UploadPolicy.window(0,65536,4));}
 @Test public void serverLimitAlwaysWins(){assertEquals(32768,UploadPolicy.chunk(44,32768));assertEquals(32768,UploadPolicy.chunk(-1,32768));}
 @Test public void largeAndUnknownFilesStayWithinFourMiB(){for(long size:new long[]{16*1048576L,-1,Long.MAX_VALUE}){int chunk=UploadPolicy.chunk(size,1048576);int window=UploadPolicy.window(size,chunk,4);assertTrue((long)chunk*window<=4*1048576);assertEquals(4,window);}}
}
