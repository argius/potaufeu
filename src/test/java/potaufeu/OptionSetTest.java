package potaufeu;

import static org.junit.Assert.assertEquals;
import org.junit.*;
import potaufeu.OptionSet.Parser;

public class OptionSetTest {

    @Test
    public void testParse() throws Exception {
        Parser parser = new Parser();
        OptionSet o;
        o = parser.parse("--exts");
        assertEquals(true, o.isCollectsExtension());
        o = parser.parse("exts");
        assertEquals(false, o.isCollectsExtension());
        o = parser.parse("hello", "--exts");
        assertEquals("[hello]", o.getPathPatterns().toString());
        assertEquals(true, o.isCollectsExtension());
        o = parser.parse("-n", "abc", "-t", "20150402", "--ctime", "20150413", "--atime", "20150425");
        assertEquals("[abc]", o.getNamePatterns().toString());
        assertEquals("[20150413]", o.getCtimePatterns().toString());
        assertEquals("[20150402]", o.getMtimePatterns().toString());
        assertEquals("[20150425]", o.getAtimePatterns().toString());
    }

}
