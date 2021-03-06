package potaufeu;

import java.io.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.function.*;
import java.util.stream.*;

/**
 * The factory class for this app's path matchers.
 * These path matchers are also file filters.
 * @see {java.nio.file.PathMatcher}
 */
public final class PathMatcherFactory {

    private static final Log log = Log.logger(PathMatcherFactory.class);

    private PathMatcherFactory() {
    }

    public static Optional<PathMatcher> createFromStringPatterns(List<String> patterns, Function<Path, String> extr) {
        if (patterns.stream().filter(x -> !x.isEmpty()).count() > 0) {
            StringMatchingPredicate matcher = StringMatchingPredicate.create(patterns);
            return Optional.of(path -> matcher.matches(extr.apply(path)));
        }
        return Optional.empty();
    }

    public static Optional<PathMatcher> createMatcherByPath(OptionSet opts) {
        log.debug(() -> "createMatcherByPath: patterns=<" + opts.getPathPatterns() + ">");
        return createFromStringPatterns(opts.getPathPatterns(), Object::toString);
    }

    public static Optional<PathMatcher> createMatcherByName(OptionSet opts) {
        log.debug(() -> "createMatcherByName: patterns=<" + opts.getNamePatterns() + ">");
        return createFromStringPatterns(opts.getNamePatterns(), FileAttributeFormatter::name);
    }

    public static Optional<PathMatcher> createMatcherByExclusion(OptionSet opts) {
        log.debug(() -> "createMatcherByExclusion: patterns=<" + opts.getExclusionPatterns() + ">");
        // @formatter:off
        return opts.getExclusionPatterns().stream()
                .map(StringMatchingPredicate::create)
                .reduce(StringMatchingPredicate::or)
                .map(x -> path -> !x.matches(path.toString()));
        // @formatter:on
    }

    public static List<PathMatcher> toPathMatchers(List<String> patterns, Function<String, PathMatcher> mapper) {
        return patterns.stream().map(mapper).collect(Collectors.toList());
    }

    @Deprecated
    public static List<PathMatcher> exclusionMatchers(OptionSet opts) {
        log.debug(() -> "exclusionMatchers: pattern=<" + opts.getExclusionPatterns() + ">");
        return toPathMatchers(opts.getExclusionPatterns(), x -> exclusionMatcher(x));
    }

    @Deprecated
    public static PathMatcher exclusionMatcher(String pattern) {
        log.debug(() -> "exclusionMatcher: pattern=<" + pattern + ">");
        StringMatchingPredicate matcher = StringMatchingPredicate.create(pattern);
        return path -> !matcher.matches(path.toString());
    }

    @Deprecated
    public static List<PathMatcher> nameMatchers(OptionSet opts) {
        log.debug(() -> "nameMatchers: pattern=<" + opts.getNamePatterns() + ">");
        return toPathMatchers(opts.getNamePatterns(), x -> nameMatcher(x));
    }

    @Deprecated
    public static PathMatcher nameMatcher(String pattern) {
        log.debug(() -> "nameMatcher: pattern=<" + pattern + ">");
        StringMatchingPredicate matcher = StringMatchingPredicate.create(pattern);
        return path -> matcher.matches(FileAttributeFormatter.name(path));
    }

    public static Optional<PathMatcher> extensionMatchers(OptionSet opts) {
        return extensionMatcher(opts.getExtensionPatterns());
    }

    public static Optional<PathMatcher> extensionMatcher(String... patterns) {
        return extensionMatcher(Arrays.asList(patterns));
    }

    static Optional<PathMatcher> extensionMatcher(List<String> patterns) {
        log.debug(() -> "extensionMatcher(plural): pattern=<" + patterns + ">");
        if (patterns.isEmpty())
            return Optional.empty();
        final String pattern;
        if (patterns.size() == 1)
            pattern = "(?i).*\\." + patterns.get(0);
        else
            pattern = String.format("(?i).*\\.(%s)", String.join("|", patterns));
        return Optional.of(path -> FileAttributeFormatter.name(path).matches(pattern));
    }

    @Deprecated
    public static List<PathMatcher> pathMatchers(OptionSet opts) {
        log.debug(() -> "PathMatchers: patterns=<" + opts.getPathPatterns() + ">");
        return toPathMatchers(opts.getPathPatterns(), x -> pathMatcher(x));
    }

    @Deprecated
    public static PathMatcher pathMatcher(String pattern) {
        log.debug(() -> "PathMatcher: pattern=<" + pattern + ">");
        StringMatchingPredicate matcher = StringMatchingPredicate.create(pattern);
        return path -> matcher.matches(path.toString());
    }

    public static List<PathMatcher> fileTypeMatchers(OptionSet opts) {
        List<PathMatcher> a = new ArrayList<>();
        if (opts.isFile())
            a.add(Files::isRegularFile);
        return a;
    }

    public static List<PathMatcher> fileContentTypeMatchers(OptionSet opts) {
        List<PathMatcher> a = new ArrayList<>();
        if (opts.isText())
            a.add(path -> isText(path));
        return a;
    }

    private static boolean isText(Path path) {
        if (!Files.isRegularFile(path))
            return false;
        // experimental
        try (Stream<String> stream = Files.lines(path)) {
            @SuppressWarnings("unused")
            long n = stream.limit(4096).count();
            return true;
        } catch (UncheckedIOException e) {
            // ignore
        } catch (Exception e) {
            System.err.printf("warning: %s at isText, file=%s%n", e, path);
        }
        return false;
    }

    public static List<PathMatcher> fileSizeMatchers(OptionSet opts) {
        return toPathMatchers(opts.getFileSizePatterns(), x -> fileSizeMatcher(x));
    }

    public static PathMatcher fileSizeMatcher(String pattern) {
        log.debug(() -> "fileSizeMatcher: pattern=<" + pattern + ">");
        if (pattern.startsWith("-")) {
            final long max = FileSize.toByteSize(pattern.substring(1));
            log.debug(() -> "added file size matcher: x <= " + max);
            return path -> getFileSize(path) <= max;
        }
        else if (!pattern.contains("-") || pattern.endsWith("-")) {
            String ptn0 = (pattern.endsWith("-")) ? pattern.substring(0, pattern.length() - 1) : pattern;
            final long min = FileSize.toByteSize(ptn0);
            log.debug(() -> "added file size matcher: " + min + " <= x");
            return path -> min <= getFileSize(path);
        }
        final int index = pattern.indexOf('-');
        assert index > 0;
        final long min = FileSize.toByteSize(pattern.substring(0, index));
        final long max = FileSize.toByteSize(pattern.substring(index + 1, pattern.length()));
        if (min > max)
            throw new IllegalArgumentException("min > max: " + pattern);
        log.debug(() -> "added file size matcher: " + min + " <= x <= " + max);
        return path -> {
            long size = getFileSize(path);
            return min <= size && size <= max;
        };
    }

    private static long getFileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException e) {
            return -1L;
        }
    }

    public static List<PathMatcher> ctimeMatchers(OptionSet opts) {
        log.debug(() -> "ctimeMatchers: patterns=<" + opts.getCtimePatterns() + ">");
        return toPathMatchers(opts.getCtimePatterns(),
            x -> fileTimeMatcher(x, FileAttributeFormatter::ctime, opts.createdTime));
    }

    public static List<PathMatcher> mtimeMatchers(OptionSet opts) {
        log.debug(() -> "mtimeMatchers: patterns=<" + opts.getMtimePatterns() + ">");
        return toPathMatchers(opts.getMtimePatterns(),
            x -> fileTimeMatcher(x, FileAttributeFormatter::mtime, opts.createdTime));
    }

    public static List<PathMatcher> atimeMatchers(OptionSet opts) {
        log.debug(() -> "atimeMatchers: patterns=<" + opts.getAtimePatterns() + ">");
        return toPathMatchers(opts.getAtimePatterns(),
            x -> fileTimeMatcher(x, FileAttributeFormatter::atime, opts.createdTime));
    }

    public static PathMatcher fileTimeMatcher(String pattern, ToLongFunction<Path> f2millis, long now) {
        final String prefix = "fileTimeMatcher: ";
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
            final long min = TimePoint.millis(pattern.substring(0, pattern.length() - 1), now);
            log.debug(() -> prefix + toDateTime(min) + " <= x");
            return file -> min <= f2millis.applyAsLong(file);
        }
        else if (pattern.startsWith("-")) {
            final long max = TimePoint.millis(pattern.substring(1), now, true);
            log.debug(() -> prefix + ": x <= " + toDateTime(max));
            return file -> f2millis.applyAsLong(file) <= max;
        }
        final int index = pattern.indexOf('-');
        assert index > 0;
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
