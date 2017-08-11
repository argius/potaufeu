package potaufeu;

import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.time.*;
import java.time.format.*;
import java.util.*;
import java.util.function.*;

public final class FileAttributePrinter {

    private static final Log log = Log.logger(FileAttributePrinter.class);

    private static final DateTimeFormatter dtf1 = DateTimeFormatter.ofPattern("MMM dd HH:mm");
    private static final DateTimeFormatter dtf2 = DateTimeFormatter.ofPattern("MMM dd  uuuu");

    private final PrintWriter out;
    private final String eol;
    private final Function<Path, String> path2s;

    public FileAttributePrinter(PrintWriter out, String eol, Function<Path, String> path2s) {
        this.out = out;
        this.eol = eol;
        this.path2s = path2s;
    }

    public TerminalOperation path() {
        final String fmt = "%s" + eol;
        return x -> out.printf(fmt, path2s.apply(x));
    }

    public TerminalOperation fullPath() {
        final String fmt = "%s" + eol;
        return x -> out.printf(fmt, path2s.apply(x.toAbsolutePath()));
    }

    public TerminalOperation list() {
        final String fmt = "%s%s %s %s" + eol;
        return x -> {
            FileAttributeFormatter u = new FileAttributeFormatter(x);
            u.setFileSizeFormatter(sz -> formatFileSize(sz));
            u.setFileTimeFormatter(ft -> formatFileTime(ft));
            out.printf(fmt, u.entryType(), u.formattedSize(), u.formattedMtime(), path2s.apply(x));
        };
    }

    public TerminalOperation detailList() {
        final String fmt = "%s%s %s, %s, %s %s" + eol;
        return x -> {
            FileAttributeFormatter u = new FileAttributeFormatter(x);
            u.setFileSizeFormatter(sz -> formatFileSize(sz));
            u.setFileTimeFormatter(ft -> formatFileTime(ft));
            out.printf(fmt, u.entryType(), u.formattedSize(), u.formattedCtime(), u.formattedMtime(),
                u.formattedAtime(), path2s.apply(x));
        };
    }

    public TerminalOperation posixLikeList() {
        final String fmt = "%s%s%s%2s %s%s %s %s" + eol;
        return x -> {
            FileAttributeFormatter u = new FileAttributeFormatter(x);
            u.setFileSizeFormatter(sz -> formatPosixLikeFileSize(sz));
            u.setFileTimeFormatter(ft -> formatPosixLikeDateTime(ft));
            final char type = u.entryType();
            final String perms = u.formattedPermissions();
            final char aclSign = u.aclSign();
            final String nlink = u.nLink();
            final String owner = u.ownerString();
            final String size = u.formattedSize();
            final String mtime = u.formattedMtime();
            StringBuilder sbPath = new StringBuilder();
            sbPath.append(path2s.apply(x));
            if (type == 'l')
                try {
                    Path symlink = Files.readSymbolicLink(x);
                    sbPath.append(" -> ").append(path2s.apply(symlink));
                } catch (IOException e) {
                    log.warn(() -> "", e);
                }
            out.printf(fmt, type, perms, aclSign, nlink, owner, size, mtime, sbPath);
        };
    }

    public TerminalOperation linesCountList() {
        return linesCountList(Collections.emptyMap());
    }

    public TerminalOperation linesCountList(Map<Path, List<FileLine>> grepped) {
        final String fmt = "%9s lines %9s bytes %s" + eol;
        return x -> {
            long lineCount = -1;
            try {
                if (grepped.containsKey(x))
                    lineCount = grepped.get(x).size();
                else
                    lineCount = Files.lines(x).count();
            } catch (IOException e) {
                log.warn(() -> "in linesCountList: " + e);
            }
            long fileSize = FileAttributeFormatter.size(x);
            String formattedLineCount = (lineCount == -1) ? "?" : String.format("%,12d", lineCount);
            String formattedFileSize = (fileSize == -1) ? "?" : formatFileSize(fileSize);
            out.printf(fmt, formattedLineCount, formattedFileSize, path2s.apply(x));
        };
    }

    private static String formatFileSize(long size) {
        return String.format("%,10d", size);
    }

    private static String formatFileTime(FileTime ft) {
        return String.format("%1$tF %1$tT", ft.toMillis());
    }

    private static String formatPosixLikeFileSize(long size) {
        return String.format("%8d", size);
    }

    private static String formatPosixLikeDateTime(FileTime ft) {
        LocalDateTime dt = LocalDateTime.ofInstant(ft.toInstant(), ZoneId.systemDefault());
        DateTimeFormatter dtf = (dt.isBefore(LocalDateTime.now().minusMonths(6))) ? dtf2 : dtf1;
        // FIXME adhoc date format
        return String.format("%11s", dt.format(dtf)).replace(" 0", "  ");
    }

}
