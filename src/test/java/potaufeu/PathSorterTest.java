package potaufeu;

import static org.junit.Assert.*;
import static potaufeu.PackagePrivate.asPrintWriter;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.junit.*;
import org.junit.Test;
import org.junit.rules.*;
import junit.framework.*;

public class PathSorterTest {

    static final long baseTime = 1483228800_000L; // 2017-01-01T00:00:00Z
    static final FileAttributePrinter p =
        new FileAttributePrinter(asPrintWriter(System.out), TerminalOperation.getEol(), String::valueOf);

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    @Before
    public void prepareFiles() throws IOException {
        createFile("A", 7, 20);
        createFile("b", 6, 45);
        createFile("c", 9, 15);
        createFile("D", 5, 32);
    }

    @Test
    public void testGetSorter() {
        Optional<Comparator<Path>> x = PathSorter.getSorter(Arrays.asList("mtime", "name"));
        assertTrue(x.isPresent());
    }

    @Test
    public void testCreateComparator() {
        System.out.println("tmp root=" + tmpFolder.getRoot());
        // sort by mtime
        assertEquals("[c, A, D, b]", String.valueOf(sortedFileNames("mtime")));
        assertEquals("[c, A, D, b]", String.valueOf(sortedFileNames("+mtime")));
        assertEquals("[b, D, A, c]", String.valueOf(sortedFileNames("_mtime")));
        assertEquals("[D, b, A, c]", String.valueOf(sortedFileNames("size")));
        assertEquals("[c, A, b, D]", String.valueOf(sortedFileNames("_size")));
        assertEquals("[A, b, c, D]", String.valueOf(sortedFileNames("iname")));
        assertEquals("[D, c, b, A]", String.valueOf(sortedFileNames("_iname")));
        assertEquals("[A, D, b, c]", String.valueOf(sortedFileNames("name")));
        assertEquals("[c, b, D, A]", String.valueOf(sortedFileNames("_name")));
        assertEquals("java.lang.IllegalArgumentException: unknown sortkey: dummy",
            getExceptionAsString(() -> PathSorter.createComparator("dummy")));
    }

    Path createFile(String name, int size, long mtimedelta) {
        try {
            File f = tmpFolder.newFile(name);
            Files.write(f.toPath(), new byte[size]);
            f.setLastModified(baseTime + mtimedelta * 60 * 1_000L);
            return f.toPath();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    List<String> sortedFileNames(String sortExpr) {
        Comparator<Path> cmp = PathSorter.createComparator(sortExpr);
        return PathIterator.streamOf(tmpFolder.getRoot().toPath()).sorted(cmp).map(FileAttributeFormatter::name)
                .filter(x -> !x.startsWith("junit")).collect(Collectors.toList());
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
