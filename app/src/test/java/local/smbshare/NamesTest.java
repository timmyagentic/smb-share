package local.smbshare;
import org.junit.Test;
import static org.junit.Assert.*;
public class NamesTest {
 @Test public void traversalAndInvalidNames(){assertEquals(".._.._secret",Names.safe("../../secret"));assertEquals("file",Names.safe("..."));assertEquals("a_b_c",Names.safe("a\\b:c"));}
 @Test public void extensionPreservedForCollisions(){assertEquals("photo (2).jpg",Names.numbered("photo.jpg",2));assertEquals(".hidden (1)",Names.numbered(".hidden",1));}
 @Test public void unicodeFitsServerLimits(){String s=Names.safe("照片😀".repeat(100));assertTrue(s.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=180);assertFalse(Character.isHighSurrogate(s.charAt(s.length()-1)));}
}
