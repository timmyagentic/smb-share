package local.smbshare;

final class UploadPolicy {
 static int chunk(long size,int limit){return size<0?limit:(int)Math.max(64*1024,Math.min(limit,size));}
 static int window(long size,int chunk,int limit){return size<=0?size<0?limit:1:(int)Math.min(limit,1+(size-1)/chunk);}
}
