package potaufeu;

import static org.junit.Assert.*;
import static potaufeu.PathMatcherFactory.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.function.*;
import org.junit.*;
import org.junit.Test;
import org.junit.rules.*;
import junit.framework.*;
import potaufeu.OptionSet.*;

public final class PathMatcherFactoryTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private Path path1;
    private Path path2;

    @Before
    public void createFile() throws IOException {
        this.path1 = tmpFolder.newFile("test1.txt").toPath();
        this.path2 = tmpFolder.newFile("test2.xml").toPath();
    }

    @Test
    public void testPathMatcherFactory() throws Exception {
        Constructor<?> ctor = PathMatcherFactory.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        ctor.newInstance();
    }

    @Test
    public void testToPathMatchers() {
        List<String> a = Arrays.asList("aaa", "bbb");
        assertEquals(a.size(), toPathMatchers(a, x -> f -> true).size());
    }

    @Test
    public void testExclusionMatchers() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, exclusionMatchers(parser.parse("aaa")).size());
        assertEquals(1, exclusionMatchers(parser.parse("--exclude", "aaa")).size());
        assertEquals(2, exclusionMatchers(parser.parse("--exclude", "aaa", "--exclude", "bbb")).size());
    }

    @Test
    public void testExclusionMatcher() {
        PathMatcher f = exclusionMatcher("test2");
        assertTrue(f.matches(path1));
        assertFalse(f.matches(path2));
    }

    @Test
    public void testNameMatchers() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, nameMatchers(parser.parse("aaa")).size());
        assertEquals(1, nameMatchers(parser.parse("--name", "aaa")).size());
        assertEquals(2, nameMatchers(parser.parse("--name", "aaa", "--name", "bbb")).size());
    }

    @Test
    public void testNameMatcher() {
        PathMatcher f = nameMatcher("test1");
        assertTrue(f.matches(path1));
        assertFalse(f.matches(path2));
    }

    @Test
    public void testExtensionMatchers() throws Exception {
        Parser parser = new Parser();
        assertFalse(extensionMatchers(parser.parse("java")).isPresent());
        assertTrue(extensionMatchers(parser.parse(".java")).isPresent());
        assertTrue(extensionMatchers(parser.parse(".java", ".xml")).isPresent());
        assertTrue(extensionMatchers(parser.parse(".java,xml")).isPresent());
    }

    @Test
    public void testExtensionMatcher() {
        PathMatcher f = extensionMatcher("txt").get();
        assertTrue(f.matches(path1));
        assertFalse(f.matches(path2));
    }

    @Test
    public void testPathMatchers() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, pathMatchers(parser.parse()).size());
        assertEquals(1, pathMatchers(parser.parse("aaa")).size());
        assertEquals(2, pathMatchers(parser.parse("aaa", "bbb")).size());
    }

    @Test
    public void testPathMatcher() {
        final String slash = Paths.get("/").toString().replaceFirst("^.*(.)$", "$1");
        PathMatcher f = pathMatcher(slash + "test1");
        assertTrue(f.matches(path1.toAbsolutePath()));
        assertFalse(f.matches(path2.toAbsolutePath()));
    }

    @Test
    public void testFileTypeMatchers() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, fileTypeMatchers(parser.parse()).size());
        assertEquals(0, fileTypeMatchers(parser.parse("aaa")).size());
        assertEquals(1, fileTypeMatchers(parser.parse("--file")).size());
        assertEquals(1, fileTypeMatchers(parser.parse("-F")).size());
        PathMatcher f = fileTypeMatchers(parser.parse("-F")).get(0);
        assertTrue(f.matches(path1));
        assertTrue(f.matches(path2));
    }

    @Test
    public void testFileSizeMatchers() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, fileSizeMatchers(parser.parse("size")).size());
        assertEquals(1, fileSizeMatchers(parser.parse("--size", "111")).size());
        assertEquals(2, fileSizeMatchers(parser.parse("--size", "111", "--size", "222KB")).size());
    }

    @Test
    public void testFileSizeMatcher() throws IOException {
        Files.write(path2, Arrays.asList("<xml>", "</xml>"), StandardOpenOption.WRITE);
        PathMatcher f1 = fileSizeMatcher("5");
        assertFalse(f1.matches(path1));
        assertTrue(f1.matches(path2));
        PathMatcher f2 = fileSizeMatcher("-5");
        assertTrue(f2.matches(path1));
        assertFalse(f2.matches(path2));
        PathMatcher f3 = fileSizeMatcher("12-");
        assertFalse(f3.matches(path1));
        assertTrue(f3.matches(path2));
        PathMatcher f4a = fileSizeMatcher("13-15");
        assertFalse(f4a.matches(path1));
        assertTrue(f4a.matches(path2));
        PathMatcher f4b = fileSizeMatcher("0-15");
        assertTrue(f4b.matches(path1));
        assertTrue(f4b.matches(path2));
        PathMatcher f4c = fileSizeMatcher("0-0");
        assertTrue(f4c.matches(path1));
        assertFalse(f4c.matches(path2));
        assertEquals("java.lang.IllegalArgumentException: min > max: 3-2",
            getExceptionAsString(() -> fileSizeMatcher("3-2")));
    }

    @Test
    public void testCtimeMatchers() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, ctimeMatchers(parser.parse("ctime")).size());
        assertEquals(1, ctimeMatchers(parser.parse("--ctime", "2015")).size());
        assertEquals(2, ctimeMatchers(parser.parse("--ctime", "2015-", "--ctime", "-2017")).size());
    }

    @Test
    public void testMtimeMatchers() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, ctimeMatchers(parser.parse("mtime")).size());
        assertEquals(1, mtimeMatchers(parser.parse("--mtime", "2015")).size());
        assertEquals(2, mtimeMatchers(parser.parse("--mtime", "2015-", "--mtime", "-2017")).size());
    }

    @Test
    public void testAtimeMatchers() throws Exception {
        Parser parser = new Parser();
        assertEquals(0, atimeMatchers(parser.parse("atime")).size());
        assertEquals(1, atimeMatchers(parser.parse("--atime", "2015")).size());
        assertEquals(2, atimeMatchers(parser.parse("--atime", "2015-", "--atime", "-2017")).size());
    }

    @Test
    public void testFileTimeMatcher() throws Exception {
        Files.setLastModifiedTime(path1, FileTime.fromMillis(TimePoint.millis("201304030000")));
        Files.setLastModifiedTime(path2, FileTime.fromMillis(TimePoint.millis("201305120000")));
        Parser parser = new Parser();
        OptionSet opts = parser.parse("aaa");
        final long now = opts.createdTime;
        final ToLongFunction<Path> toLong = FileAttributeFormatter::mtime;
        PathMatcher f1a = fileTimeMatcher("2012", toLong, now);
        assertFalse(f1a.matches(path1));
        assertFalse(f1a.matches(path2));
        PathMatcher f1b = fileTimeMatcher("2013", toLong, now);
        assertTrue(f1b.matches(path1));
        assertTrue(f1b.matches(path2));
        PathMatcher f1c = fileTimeMatcher("2014", toLong, now);
        assertFalse(f1c.matches(path1));
        assertFalse(f1c.matches(path2));
        PathMatcher f2 = fileTimeMatcher("201305-", toLong, now);
        assertFalse(f2.matches(path1));
        assertTrue(f2.matches(path2));
        PathMatcher f3 = fileTimeMatcher("-201304", toLong, now);
        assertTrue(f3.matches(path1));
        assertFalse(f3.matches(path2));
        PathMatcher f4a = fileTimeMatcher("20130403-20130511", toLong, now);
        assertTrue(f4a.matches(path1));
        assertFalse(f4a.matches(path2));
        PathMatcher f4b = fileTimeMatcher("20130403-20130512", toLong, now);
        assertTrue(f4b.matches(path1));
        assertTrue(f4b.matches(path2));
        PathMatcher f4c = fileTimeMatcher("2013040312-201305", toLong, now);
        assertFalse(f4c.matches(path1));
        assertTrue(f4c.matches(path2));
        assertEquals("java.lang.IllegalArgumentException: min > max: 2015-2014",
            getExceptionAsString(() -> fileTimeMatcher("2015-2014", toLong, now)));
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
