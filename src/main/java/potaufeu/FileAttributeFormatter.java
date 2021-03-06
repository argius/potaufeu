package potaufeu;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.security.*;
import java.util.*;
import java.util.function.*;

public final class FileAttributeFormatter {

    private static final Log log = Log.logger(FileAttributeFormatter.class);

    private static volatile boolean unixViewNotAvailableChecked = false;

    private final Path path;
    private final BasicFileAttributes attr;

    private Function<Long, String> fileSizeFormatter;
    private Function<FileTime, String> fileTimeFormatter;

    public FileAttributeFormatter(Path path) {
        this(path, readBasicAttributes(path));
    }

    public FileAttributeFormatter(Path path, BasicFileAttributes attributes) {
        this.path = path;
        this.attr = attributes;
        this.fileSizeFormatter = String::valueOf;
        this.fileTimeFormatter = String::valueOf;
    }

    public Path getPath() {
        return path;
    }

    public BasicFileAttributes getAttribute() {
        return attr;
    }

    public Function<Long, String> getFileSizeFormatter() {
        return fileSizeFormatter;
    }

    public void setFileSizeFormatter(Function<Long, String> fileSizeFormatter) {
        this.fileSizeFormatter = fileSizeFormatter;
    }

    public Function<FileTime, String> getFileTimeFormatter() {
        return fileTimeFormatter;
    }

    public void setFileTimeFormatter(Function<FileTime, String> fileTimeFormatter) {
        this.fileTimeFormatter = fileTimeFormatter;
    }

    public String name() {
        return name(path);
    }

    public static String name(File file) {
        return file.getName();
    }

    public static String name(Path path) {
        Path n = path.getFileName();
        if (n == null)
            // the drive root on Windows returns null
            return "";
        return n.toString();
    }

    public String formattedSize() {
        return fileSizeFormatter.apply(attr.size());
    }

    public long size() {
        return size(path);
    }

    public static long size(File file) {
        return file.length();
    }

    public static long size(Path path) {
        return readBasicAttributes(path).size();
    }

    public String formattedCtime() {
        return fileTimeFormatter.apply(attr.creationTime());
    }

    public static long ctime(File file) {
        return ctime(file.toPath());
    }

    public static long ctime(Path path) {
        return readBasicAttributes(path).creationTime().toMillis();
    }

    public String formattedMtime() {
        return fileTimeFormatter.apply(attr.lastModifiedTime());
    }

    public static long mtime(File file) {
        return file.lastModified();
    }

    public static long mtime(Path path) {
        return mtime(path.toFile());
    }

    public String formattedAtime() {
        return fileTimeFormatter.apply(attr.lastAccessTime());
    }

    public static long atime(File file) {
        return atime(file.toPath());
    }

    public static long atime(Path path) {
        return readBasicAttributes(path).lastAccessTime().toMillis();
    }

    public char entryType() {
        return getEntryType(path);
    }

    public static char entryType(Path path) {
        return getEntryType(path);
    }

    public static char getEntryType(Path path) {
        if (Files.isDirectory(path))
            return 'd';
        if (Files.isSymbolicLink(path))
            return 'l';
        if (Files.isRegularFile(path))
            return '-';
        return '?';
    }

    @Deprecated // Use getEntryType(Path) instead
    public static char getEntryType(BasicFileAttributes attr) {
        if (attr.isDirectory())
            return 'd';
        if (attr.isSymbolicLink())
            return 'l';
        if (attr.isRegularFile())
            return '-';
        if (attr.isOther())
            return ':';
        return '?';
    }

    public String formattedPermissions() {
        return permissions().map(PosixFilePermissions::toString).orElseGet(
            () -> formatPermissionsAsBools(Files.isReadable(path), Files.isWritable(path), Files.isExecutable(path)));
    }

    static String formatPermissionsAsBools(boolean readable, boolean writable, boolean executable) {
        char r = (readable) ? 'r' : '-';
        char w = (writable) ? 'w' : '-';
        char x = (executable) ? 'x' : '-';
        return String.valueOf(new char[] { r, w, x, r, w, x, r, '-', x });
    }

    public Optional<Set<PosixFilePermission>> permissions() {
        return permissions(attr);
    }

    public static Optional<Set<PosixFilePermission>> permissions(Path path) {
        return permissions(readBasicAttributes(path));
    }

    public static Optional<Set<PosixFilePermission>> permissions(BasicFileAttributes attr) {
        if (attr instanceof PosixFileAttributes) {
            PosixFileAttributes posixAttr = (PosixFileAttributes) attr;
            return Optional.of(posixAttr.permissions());
        }
        return Optional.empty();
    }

    public char aclSign() {
        return aclSign(path);
    }

    public static char aclSign(Path path) {
        AclFileAttributeView view = Files.getFileAttributeView(path, AclFileAttributeView.class);
        if (view != null)
            try {
                List<AclEntry> x = view.getAcl();
                if (!x.isEmpty())
                    return '+';
            } catch (IOException e) {
                log.warn(() -> "", e);
            }
        return ' ';
    }

    public String nLink() {
        // XXX bad performance ?
        if (!unixViewNotAvailableChecked)
            try {
                final Object attr = Files.getAttribute(path, "unix:nlink");
                if (attr != null)
                    return attr.toString();
            } catch (IOException | UnsupportedOperationException e) {
                log.debug(() -> String.format("[%s] at accessing unix:nlink", e));
                unixViewNotAvailableChecked = true;
            }
        return "";
    }

    public String ownerString() {
        return ownerString(path, attr);
    }

    public static String ownerString(Path path, BasicFileAttributes attr) {
        if (attr instanceof PosixFileAttributes) {
            PosixFileAttributes posixAttr = (PosixFileAttributes) attr;
            return String.format("%-8s %-8s", posixAttr.owner(), posixAttr.group());
        }
        try {
            UserPrincipal principal = Files.getOwner(path);
            if (principal != null)
                return principal.getName();
        } catch (IOException e) {
            log.warn(() -> "in ownerString", e);
        }
        return "???      ???     "; // 17 (8+1+8)
    }

    public Optional<UserPrincipal> getUserPrincipalOrEmpty() {
        try {
            UserPrincipal up;
            if (attr instanceof PosixFileAttributes) {
                PosixFileAttributes posixAttr = (PosixFileAttributes) attr;
                up = posixAttr.owner();
            }
            else
                up = Files.getOwner(path);
            return Optional.ofNullable(up);
        } catch (IOException e) {
            log.warn(() -> "in getUserPrincipalOrEmpty", e);
        }
        return Optional.empty();
    }

    public String getUserPrincipalName() {
        return toPrincipalNameOrEmpty(getUserPrincipalOrEmpty());
    }

    public Optional<GroupPrincipal> getGroupPrincipalOrEmpty() {
        if (attr instanceof PosixFileAttributes) {
            PosixFileAttributes posixAttr = (PosixFileAttributes) attr;
            return Optional.ofNullable(posixAttr.group());
        }
        return Optional.empty();
    }

    public String getGroupPrincipalName() {
        return toPrincipalNameOrEmpty(getGroupPrincipalOrEmpty());
    }

    private static String toPrincipalNameOrEmpty(Optional<? extends Principal> pr) {
        return pr.isPresent() ? pr.get().getName() : "";
    }

    public static <T extends Path> Function<T, Long> toLongLambda(String attrName) {
        switch (attrName) {
            case "size":
                return x -> size(x);
            case "ctime":
                return x -> ctime(x);
            case "mtime":
                return x -> mtime(x);
            case "atime":
                return x -> atime(x);
            default:
                throw new IllegalArgumentException("bad attrName: " + attrName);
        }
    }

    static BasicFileAttributes readBasicAttributes(Path path) {
        try {
            try {
                return Files.readAttributes(path, PosixFileAttributes.class);
            } catch (UnsupportedOperationException e) {
                // ignore
            }
            return Files.readAttributes(path, BasicFileAttributes.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

}
