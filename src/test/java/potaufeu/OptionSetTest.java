package potaufeu;

import static org.junit.Assert.*;
import static potaufeu.Messages.message;
import org.apache.commons.cli.*;
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

    @Test
    public void testParserOptIntValue() throws ParseException {
        Parser parser = new Parser();
        Options o = parser.getOptions();
        CommandLineParser clp = new PosixParser();
        CommandLine cl;
        String optionKey = "head";
        String[] args = { "--" + optionKey, "3" };
        cl = clp.parse(o, args);
        assertEquals(3, Parser.optIntValue(cl, optionKey).getAsInt());
        try {
            args[1] = "a";
            cl = clp.parse(o, args);
            Parser.optIntValue(cl, optionKey).getAsInt();
            fail("expects an error: not a number");
        } catch (IllegalArgumentException e) {
            assertEquals(message("e.argOptionMustPositiveNumber", optionKey, args[1]), e.getMessage());
        }
        try {
            args[1] = "-1";
            cl = clp.parse(o, args);
            Parser.optIntValue(cl, optionKey).getAsInt();
            fail("expects an error: negative int");
        } catch (IllegalArgumentException e) {
            assertEquals(message("e.argOptionMustPositiveNumber", optionKey, args[1]), e.getMessage());
        }
    }

}
