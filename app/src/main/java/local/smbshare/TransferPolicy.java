package local.smbshare;
final class TransferPolicy {
 static final long LIMIT=32L*1024*1024;
 static boolean needsPage(long[] sizes){
  long total=0;
  for(long size:sizes){if(size<0||size>=LIMIT-total)return true;total+=size;}
  return false;
 }
}
