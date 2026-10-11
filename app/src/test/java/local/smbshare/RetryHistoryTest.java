package local.smbshare;
import org.junit.Test;
import static org.junit.Assert.*;
public class RetryHistoryTest {
 @Test public void retrySkipsOnlyCompletedPositionsOfItsOwnBatch(){RetryHistory h=new RetryHistory(8);h.add("a",0);h.add("a",2);assertTrue(h.contains("a",0));assertFalse(h.contains("a",1));assertFalse(h.contains("b",0));}
 @Test public void historyIsBoundedAndKeepsRecentlyRetriedBatch(){RetryHistory h=new RetryHistory(2);h.add("a",0);h.add("b",1);assertTrue(h.contains("a",0));h.add("c",0);assertEquals(2,h.batchCount());assertTrue(h.contains("a",0));assertFalse(h.contains("b",1));}
 @Test public void largeBatchUsesPositionsWithoutCrossBatchCollisions(){RetryHistory h=new RetryHistory(8);for(int i=0;i<10000;i+=2)h.add("large",i);assertTrue(h.contains("large",9998));assertFalse(h.contains("large",9999));assertFalse(h.contains("other",9998));}
}
