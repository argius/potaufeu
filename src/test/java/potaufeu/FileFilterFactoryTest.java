package potaufeu;

import static org.junit.Assert.*;
import static potaufeu.FileFilterFactory.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;
import junit.framework.*;
import org.junit.*;
import org.junit.Test;
import org.junit.rules.*;
import potaufeu.OptionSet.Parser;

public final class FileFilterFactoryTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private File file1;
    private File file2;

    @Before
    public void createFile() throws IOException {
        this.file1 = tmpFolder.newFile("test1.txt");
        this.file2 = tmpFolder.newFile("test2.xml");
    }

    @Test
    public void testFileFilterFactory() throws Exception {
        Constructor<?> ctor = FileFilterFactory.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ctor.newInstance();
    }

    @Test
    public void testToFileFilters() {
        List<String> a = Arrays.asList("aaa", "bbb");
        assertEquals(a.size(), toFileFilters(a, x -> f -> true).size());
    }

    @Test
    public void testExclusionFilters() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, exclusionFilters(parser.parse("aaa")).size());
        assertEquals(1, exclusionFilters(parser.parse("--exclude", "aaa")).size());
        assertEquals(2, exclusionFilters(parser.parse("--exclude", "aaa", "--exclude", "bbb")).size());
    }

    @Test
    public void testExclusionFilter() {
        FileFilter f = exclusionFilter("test2");
        assertTrue(f.accept(file1));
        assertFalse(f.accept(file2));
    }

    @Test
    public void testNameFilters() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, nameFilters(parser.parse("aaa")).size());
        assertEquals(1, nameFilters(parser.parse("--name", "aaa")).size());
        assertEquals(2, nameFilters(parser.parse("--name", "aaa", "--name", "bbb")).size());
    }

    @Test
    public void testNameFilter() {
        FileFilter f = nameFilter("test1");
        assertTrue(f.accept(file1));
        assertFalse(f.accept(file2));
    }

    @Test
    public void testExtensionFilters() throws Exception {
        Parser parser = new Parser();
        assertFalse(extensionFilters(parser.parse("java")).isPresent());
        assertTrue(extensionFilters(parser.parse(".java")).isPresent());
        assertTrue(extensionFilters(parser.parse(".java", ".xml")).isPresent());
        assertTrue(extensionFilters(parser.parse(".java,xml")).isPresent());
    }

    @Test
    public void testExtensionFilter() {
        FileFilter f = extensionFilter("txt").get();
        assertTrue(f.accept(file1));
        assertFalse(f.accept(file2));
    }

    @Test
    public void testPathFilters() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, pathFilters(parser.parse()).size());
        assertEquals(1, pathFilters(parser.parse("aaa")).size());
        assertEquals(2, pathFilters(parser.parse("aaa", "bbb")).size());
    }

    @Test
    public void testPathFilter() {
        final String slash = Paths.get("/").toString().replaceFirst("^.*(.)$", "$1");
        FileFilter f = pathFilter(slash + "test1");
        assertTrue(f.accept(file1.getAbsoluteFile()));
        assertFalse(f.accept(file2.getAbsoluteFile()));
    }

    @Test
    public void testFileTypeFilters() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, fileTypeFilters(parser.parse()).size());
        assertEquals(0, fileTypeFilters(parser.parse("aaa")).size());
        assertEquals(1, fileTypeFilters(parser.parse("--file")).size());
        assertEquals(1, fileTypeFilters(parser.parse("-F")).size());
        FileFilter f = fileTypeFilters(parser.parse("-F")).get(0);
        assertTrue(f.accept(file1));
        assertTrue(f.accept(file2));
    }

    @Test
    public void testFileSizeFilters() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, fileSizeFilters(parser.parse("size")).size());
        assertEquals(1, fileSizeFilters(parser.parse("--size", "111")).size());
        assertEquals(2, fileSizeFilters(parser.parse("--size", "111", "--size", "222KB")).size());
    }

    @Test
    public void testFileSizeFilter() throws IOException {
        Files.write(file2.toPath(), Arrays.asList("<xml>", "</xml>"), StandardOpenOption.WRITE);
        FileFilter f1 = fileSizeFilter("5");
        assertFalse(f1.accept(file1));
        assertTrue(f1.accept(file2));
        FileFilter f2 = fileSizeFilter("-5");
        assertTrue(f2.accept(file1));
        assertFalse(f2.accept(file2));
        FileFilter f3 = fileSizeFilter("12-");
        assertFalse(f3.accept(file1));
        assertTrue(f3.accept(file2));
        FileFilter f4a = fileSizeFilter("13-15");
        assertFalse(f4a.accept(file1));
        assertTrue(f4a.accept(file2));
        FileFilter f4b = fileSizeFilter("0-15");
        assertTrue(f4b.accept(file1));
        assertTrue(f4b.accept(file2));
        FileFilter f4c = fileSizeFilter("0-0");
        assertTrue(f4c.accept(file1));
        assertFalse(f4c.accept(file2));
        assertEquals("java.lang.IllegalArgumentException: min > max: 3-2",
                getExceptionAsString(() -> fileSizeFilter("3-2")));
    }

    @Test
    public void testCtimeFilters() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, ctimeFilters(parser.parse("ctime")).size());
        assertEquals(1, ctimeFilters(parser.parse("--ctime", "2015")).size());
        assertEquals(2, ctimeFilters(parser.parse("--ctime", "2015-", "--ctime", "-2017")).size());
    }

    @Test
    public void testMtimeFilters() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, ctimeFilters(parser.parse("mtime")).size());
        assertEquals(1, mtimeFilters(parser.parse("--mtime", "2015")).size());
        assertEquals(2, mtimeFilters(parser.parse("--mtime", "2015-", "--mtime", "-2017")).size());
    }

    @Test
    public void testAtimeFilters() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, atimeFilters(parser.parse("atime")).size());
        assertEquals(1, atimeFilters(parser.parse("--atime", "2015")).size());
        assertEquals(2, atimeFilters(parser.parse("--atime", "2015-", "--atime", "-2017")).size());
    }

    @Test
    public void testFileTimeFilter() throws Exception {
        file1.setLastModified(TimePoint.millis("201304030000"));
        file2.setLastModified(TimePoint.millis("201305120000"));
        Parser parser = new Parser();
        OptionSet opts = parser.parse("aaa");
        final long now = opts.createdTime;
        final ToLongFunction<File> toLong = x -> x.lastModified();
        FileFilter f1a = fileTimeFilter("2012", toLong, now);
        assertFalse(f1a.accept(file1));
        assertFalse(f1a.accept(file2));
        FileFilter f1b = fileTimeFilter("2013", toLong, now);
        assertTrue(f1b.accept(file1));
        assertTrue(f1b.accept(file2));
        FileFilter f1c = fileTimeFilter("2014", toLong, now);
        assertFalse(f1c.accept(file1));
        assertFalse(f1c.accept(file2));
        FileFilter f2 = fileTimeFilter("201305-", toLong, now);
        assertFalse(f2.accept(file1));
        assertTrue(f2.accept(file2));
        FileFilter f3 = fileTimeFilter("-201304", toLong, now);
        assertTrue(f3.accept(file1));
        assertFalse(f3.accept(file2));
        FileFilter f4a = fileTimeFilter("20130403-20130511", toLong, now);
        assertTrue(f4a.accept(file1));
        assertFalse(f4a.accept(file2));
        FileFilter f4b = fileTimeFilter("20130403-20130512", toLong, now);
        assertTrue(f4b.accept(file1));
        assertTrue(f4b.accept(file2));
        FileFilter f4c = fileTimeFilter("2013040312-201305", toLong, now);
        assertFalse(f4c.accept(file1));
        assertTrue(f4c.accept(file2));
        assertEquals("java.lang.IllegalArgumentException: min > max: 2015-2014",
                getExceptionAsString(() -> fileTimeFilter("2015-2014", toLong, now)));
    }

    @FunctionalInterface
    interface ActionWithThrowsException {
        void perform() throws Exception;
    }

    static String getExceptionAsString(ActionWithThrowsException action) {
        try {
            action.perform();
            throw new AssertionFailedError();
        } catch (Exception e) {
            return e.toString();
        }
    }

}
