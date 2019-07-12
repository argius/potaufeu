package potaufeu;

import static org.junit.Assert.*;
import static potaufeu.FileAttributeFormatter.*;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.function.*;
import org.junit.*;
import org.junit.rules.*;

public final class FileAttributeFormatterTest {

    @Rule
    public TemporaryFolder tmpFolder = new TemporaryFolder();

    private File file;
    private Path path;

    @Before
    public void createFile() throws IOException {
        this.file = tmpFolder.newFile("test.txt");
        this.path = file.toPath();
    }

    @Test
    public void testNameFile() {
        assertEquals("test.txt", name(file));
    }

    @Test
    public void testNamePath() {
        assertEquals("test.txt", name(path));
    }

    @Test
    public void testToLongLambda() {
        assertEquals(Long.valueOf(file.lastModified()), toLongLambda("mtime").apply(path));
    }

    @Test
    public void testSizeFile() {
        assertEquals(file.length(), size(file));
    }

    @Test
    public void testSizePath() {
        assertEquals(file.length(), size(path));
    }

    @Test
    public void testCtimeFile() {
        assertEquals(readBasicAttributes(path).creationTime().toMillis(), ctime(file));
    }

    @Test
    public void testCtimePath() {
        assertEquals(readBasicAttributes(path).creationTime().toMillis(), ctime(path));
    }

    @Test
    public void testMtimeFile() {
        assertEquals(file.lastModified(), mtime(file));
    }

    @Test
    public void testMtimePath() {
        assertEquals(file.lastModified(), mtime(path));
    }

    @Test
    public void testAtimeFile() {
        assertEquals(readBasicAttributes(path).lastAccessTime().toMillis(), atime(file));
    }

    @Test
    public void testAtimePath() {
        assertEquals(readBasicAttributes(path).lastAccessTime().toMillis(), atime(path));
    }

    @Test
    public void testGetEntryTypePath() {
        assertEquals('-', entryType(path));
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testGetEntryTypeBasicFileAttributes() {
        assertEquals('-', getEntryType(readBasicAttributes(path)));
    }

    @Test
    public void testFormattedPermission() {
        FileAttributeFormatter o = new FileAttributeFormatter(path);
        _assertThat(o.formattedPermissions(), x -> x.matches("[r-][w-][x-][r-][w-][x-][r-][w-][x-]"));
    }

    @Test
    public void testFormatPermissionsAsBools() {
        assertEquals("---------", formatPermissionsAsBools(false, false, false));
        assertEquals("r--r--r--", formatPermissionsAsBools(true, false, false));
        assertEquals("-w--w----", formatPermissionsAsBools(false, true, false));
        assertEquals("rw-rw-r--", formatPermissionsAsBools(true, true, false));
        assertEquals("--x--x--x", formatPermissionsAsBools(false, false, true));
        assertEquals("r-xr-xr-x", formatPermissionsAsBools(true, false, true));
        assertEquals("-wx-wx--x", formatPermissionsAsBools(false, true, true));
        assertEquals("rwxrwxr-x", formatPermissionsAsBools(true, true, true));
    }

    @Test
    public void testGetAclSign() {
        _assertThat(aclSign(path), x -> x == '+' || x == ' ');
    }

    @Ignore // environment‐dependent
    @Test
    public void testNLink() {
        FileAttributeFormatter o = new FileAttributeFormatter(path);
        assertEquals("1", o.nLink());
        assertEquals("1", o.nLink());
    }

    @Test
    public void testGetOwnerString() {
        _assertThat(ownerString(path, readBasicAttributes(path)), x -> x.length() > 0);
    }

    @Test
    public void testReadBasicAttributes() {
        assertTrue(readBasicAttributes(path) instanceof BasicFileAttributes);
    }

    static <T> void _assertThat(T actual, Predicate<T> matcher) {
        assertTrue("actual[" + actual + "] does not match", matcher.test(actual));
    }

}
