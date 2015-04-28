package potaufeu;

import static org.junit.Assert.assertEquals;
import static potaufeu.FileSize.toByteSize;
import org.junit.*;

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
        assertEquals(1_536L, toByteSize("1.5KB"));
        assertEquals(22_355_640L, toByteSize("21.32MB"));
        FileSize o = new FileSize("12KB");
        assertEquals(12_288L, o.toByteSize());
    }

}
