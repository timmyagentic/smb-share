package local.smbshare;
public final class Names {
 public static String safe(String input) {
  String s = input == null ? "file" : input.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim().replaceAll("[. ]+$", "");
  if (s.isEmpty() || s.equals(".") || s.equals("..")) s="file";
  // Limit UTF-8 bytes, leaving room for the collision suffix.
  while(s.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>180) s=s.substring(0,s.offsetByCodePoints(s.length(),-1));
  return s;
 }
 public static String numbered(String s,int i) {
  if(i==0)return s;
  int dot=s.lastIndexOf('.'); if(dot<=0)return s+" ("+i+")";
  return s.substring(0,dot)+" ("+i+")"+s.substring(dot);
 }
}
