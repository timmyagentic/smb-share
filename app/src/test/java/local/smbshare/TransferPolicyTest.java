package local.smbshare;
import org.junit.Test;
import static org.junit.Assert.*;
public class TransferPolicyTest {
 @Test public void smallSharesStaySilent(){assertFalse(TransferPolicy.needsPage(new long[]{44,16*1024*1024}));}
 @Test public void exactThresholdAndCombinedFilesOpenPage(){assertTrue(TransferPolicy.needsPage(new long[]{TransferPolicy.LIMIT}));assertTrue(TransferPolicy.needsPage(new long[]{TransferPolicy.LIMIT-1,1}));}
 @Test public void unknownAndHugeSizesOpenPage(){assertTrue(TransferPolicy.needsPage(new long[]{-1}));assertTrue(TransferPolicy.needsPage(new long[]{Long.MAX_VALUE}));}
 @Test public void zeroLengthIsValidSmallFile(){assertFalse(TransferPolicy.needsPage(new long[]{0}));}
}
