package potaufeu;

import java.io.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * The factory class for this app's file filters.
 * @see {java.io.FileFilter}
 */
public final class FileFilterFactory {

    private static final Log log = Log.logger(FileFilterFactory.class);

    public static List<FileFilter> toFileFilters(List<String> patterns, Function<String, FileFilter> mapper) {
        return patterns.stream().map(mapper).collect(Collectors.toList());
    }

    public static List<FileFilter> exclusionFilters(OptionSet opts) {
        log.debug(() -> "exclusionFilters: pattern=<" + opts.getExclusionPatterns() + ">");
        return toFileFilters(opts.getExclusionPatterns(), x -> exclusionFilter(x));
    }

    public static FileFilter exclusionFilter(String pattern) {
        log.debug(() -> "exclusionFilter: pattern=<" + pattern + ">");
        return file -> !file.toString().contains(pattern);
    }

    public static List<FileFilter> nameFilters(OptionSet opts) {
        log.debug(() -> "nameFilters: pattern=<" + opts.getNamePatterns() + ">");
        return toFileFilters(opts.getNamePatterns(), x -> nameFilter(x));
    }

    public static FileFilter nameFilter(String pattern) {
        log.debug(() -> "nameFilter: pattern=<" + pattern + ">");
        return file -> file.getName().contains(pattern);
    }

    public static List<FileFilter> extensionFilters(OptionSet opts) {
        log.debug(() -> "extensionFilters: pattern=<" + opts.getExtensionPatterns() + ">");
        List<FileFilter> a = new ArrayList<>();
        List<String> patterns = opts.getExtensionPatterns();
        final int size = patterns.size();
        if (size > 1) {
            final String pattern = String.format("(?i).*\\.(%s)", String.join("|", patterns));
            a.add(file -> file.getName().matches(pattern));
        }
        else if (size == 1) {
            final String pattern = "(?i).*\\." + patterns.get(0);
            a.add(file -> file.getName().matches(pattern));
        }
        return a;
    }

    public static FileFilter extensionFilter(String pattern) {
        log.debug(() -> "extensionFilter: pattern=<" + pattern + ">");
        final String ptn = "(?i).*\\." + pattern;
        return file -> file.getName().matches(ptn);
    }

    public static List<FileFilter> pathFilters(OptionSet opts) {
        log.debug(() -> "pathFilters: patterns=<" + opts.getPathPatterns() + ">");
        return toFileFilters(opts.getPathPatterns(), x -> pathFilter(x));
    }

    public static FileFilter pathFilter(String pattern) {
        log.debug(() -> "pathFilter: pattern=<" + pattern + ">");
        return file -> file.toString().contains(pattern);
    }

    public static List<FileFilter> fileTypeFilters(OptionSet opts) {
        List<FileFilter> a = new ArrayList<>();
        if (opts.isFile())
            a.add(file -> file.isFile());
        return a;
    }

    public static List<FileFilter> fileSizeFilters(OptionSet opts) {
        return toFileFilters(opts.getFileSizePatterns(), x -> fileSizeFilter(x));
    }

    public static FileFilter fileSizeFilter(String pattern) {
        log.debug(() -> "fileSizeFilter: pattern=<" + pattern + ">");
        if (pattern.startsWith("-")) {
            final long max = FileSize.toByteSize(pattern.substring(1));
            log.debug(() -> "added file size filter: x <= " + max);
            return file -> file.length() <= max;
        }
        else if (!pattern.contains("-") || pattern.endsWith("-")) {
            String ptn0 = (pattern.endsWith("-")) ? pattern.substring(0, pattern.length() - 1) : pattern;
            final long min = FileSize.toByteSize(ptn0);
            log.debug(() -> "added file size filter: " + min + " <= x");
            return file -> min <= file.length();
        }
        else {
            final int index = pattern.indexOf('-');
            assert index >= 1;
            final long min = FileSize.toByteSize(pattern.substring(0, index));
            final long max = FileSize.toByteSize(pattern.substring(index + 1, pattern.length()));
            if (min > max)
                throw new IllegalArgumentException("min > max: " + pattern);
            log.debug(() -> "added file size filter: " + min + " <= x <= " + max);
            return file -> {
                long size = file.length();
                return min <= size && size <= max;
            };
        }
    }

    public static List<FileFilter> ctimeFilters(OptionSet opts) {
        log.debug(() -> "ctimeFilters: patterns=<" + opts.getCtimePatterns() + ">");
        return toFileFilters(opts.getCtimePatterns(),
                x -> fileTimeFilter(x, FileAttributeFormatter::ctime, opts.createdTime));
    }

    public static List<FileFilter> mtimeFilters(OptionSet opts) {
        log.debug(() -> "mtimeFilters: patterns=<" + opts.getMtimePatterns() + ">");
        return toFileFilters(opts.getMtimePatterns(),
                x -> fileTimeFilter(x, FileAttributeFormatter::mtime, opts.createdTime));
    }

    public static List<FileFilter> atimeFilters(OptionSet opts) {
        log.debug(() -> "atimeFilters: patterns=<" + opts.getAtimePatterns() + ">");
        return toFileFilters(opts.getAtimePatterns(),
                x -> fileTimeFilter(x, FileAttributeFormatter::atime, opts.createdTime));
    }

    public static FileFilter fileTimeFilter(String pattern, ToLongFunction<File> f2millis, long now) {
        final String prefix = "fileTimeFilter: ";
        if (!pattern.contains("-")) {
            final long min = TimePoint.millis(pattern, now);
            final long max = TimePoint.millis(pattern, now, true);
            log.debug(() -> prefix + toDateTime(min) + " <= x <= " + toDateTime(max));
            return file -> {
                long t = f2millis.applyAsLong(file);
                return min <= t && t <= max;
            };
        }
        else if (pattern.endsWith("-")) {
            final long min = TimePoint.millis(pattern, now);
            log.debug(() -> prefix + toDateTime(min) + " <= x");
            return file -> min <= f2millis.applyAsLong(file);
        }
        else if (pattern.startsWith("-")) {
            final long max = TimePoint.millis(pattern.substring(1), now, true);
            log.debug(() -> prefix + ": x <= " + toDateTime(max));
            return file -> f2millis.applyAsLong(file) <= max;
        }
        final int index = pattern.indexOf('-');
        assert index >= 1;
        final long min = TimePoint.millis(pattern.substring(0, index), now);
        final long max = TimePoint.millis(pattern.substring(index + 1, pattern.length()), now, true);
        if (min > max)
            throw new IllegalArgumentException("min > max: " + pattern);
        log.debug(() -> prefix + ": " + toDateTime(min) + " <= x <= " + toDateTime(max));
        return file -> {
            long t = f2millis.applyAsLong(file);
            return min <= t && t <= max;
        };
    }

    private static LocalDateTime toDateTime(long millis) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault());
    }

}
