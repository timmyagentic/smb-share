package local.smbshare;

import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.Map;

/** Bounded retry state: one bit per file, never retains source URI strings. */
final class RetryHistory {
 private final int maxBatches;
 private final LinkedHashMap<String,BitSet> batches=new LinkedHashMap<>(8,0.75f,true);
 RetryHistory(int maxBatches){if(maxBatches<1)throw new IllegalArgumentException();this.maxBatches=maxBatches;}
 boolean contains(String batch,int fileIndex){BitSet done=batches.get(batch);return done!=null&&done.get(fileIndex);}
 void add(String batch,int fileIndex){
  BitSet done=batches.get(batch);
  if(done==null){done=new BitSet();batches.put(batch,done);}
  done.set(fileIndex);
  while(batches.size()>maxBatches){String oldest=batches.keySet().iterator().next();batches.remove(oldest);}
 }
 int batchCount(){return batches.size();}
}
