package potaufeu;

import static org.junit.Assert.assertEquals;
import static potaufeu.FileSize.toByteSize;
import java.util.function.*;
import junit.framework.*;
import org.junit.Test;

public class FileSizeTest {

    @Test
    public void testToByteSize() {
        assertEquals(10L, toByteSize("10"));
        assertEquals(10_240L, toByteSize("10KB"));
        assertEquals(10_240L, toByteSize("10K"));
        assertEquals(1_048_576L, toByteSize("1MB"));
        assertEquals(1_048_576L, toByteSize("1M"));
        assertEquals(1_073_741_824L, toByteSize("1GB"));
        assertEquals(1_073_741_824L, toByteSize("1G"));
        assertEquals(1_099_511_627_776L, toByteSize("1TB"));
        assertEquals(1_099_511_627_776L, toByteSize("1T"));
        assertEquals(1_125_899_906_842_624L, toByteSize("1PB"));
        assertEquals(1_125_899_906_842_624L, toByteSize("1P"));
        assertEquals(115_292_150_460L, toByteSize("0.0000001EB"));
        assertEquals(115_292_150_460L, toByteSize("0.0000001E"));
        assertEquals(118_059_162_071_741L, toByteSize("0.0000001ZB"));
        assertEquals(118_059_162_071_741L, toByteSize("0.0000001Z"));
        assertEquals(120_892_581_961_462_912L, toByteSize("0.0000001YB"));
        assertEquals(120_892_581_961_462_912L, toByteSize("0.0000001Y"));
        assertEquals(1_536L, toByteSize("1.5KB"));
        assertEquals(22_355_640L, toByteSize("21.32MB"));
        FileSize o = new FileSize("12KB");
        assertEquals(12_288L, o.toByteSize());
    }

    @Test
    public void testToByteSizeArgError() {
        for (String s : new String[] { "", "KA", "1KAA", "1A", "1KA", "1BB", })
            assertEquals("java.lang.IllegalArgumentException: invalid filesize expression: " + s,
                getExceptionAsString(() -> {
                    toByteSize(s);
                }));
    }

    @Test
    public void testToString() {
        Function<String, String> f = x -> {
            return String.valueOf(new FileSize(x));
        };
        assertEquals("FileSize(35)", f.apply("35"));
        assertEquals("FileSize(10KB)", f.apply("10KB"));
    }

    static String getExceptionAsString(Runnable action) {
        try {
            action.run();
            throw new AssertionFailedError();
        } catch (Exception e) {
            return e.toString();
        }
    }

}
