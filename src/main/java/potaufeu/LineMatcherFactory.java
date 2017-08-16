package potaufeu;

import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

public final class LineMatcherFactory {

    private static final Log log = Log.logger(LineMatcherFactory.class);

    private LineMatcherFactory() {
    }

    private static Supplier<List<Charset>> charsetsExceptDefault() {
        List<Charset> a = new ArrayList<>();
        final String k = "potaufeu.appendCharsets";
        String v = Optional.ofNullable(System.getenv(k)).orElseGet(() -> System.getProperty(k, ""));
        if (!v.isEmpty())
            for (String csName : v.split(","))
                try {
                    a.add(Charset.forName(csName));
                } catch (IllegalCharsetNameException | UnsupportedCharsetException e) {
                    throw new RuntimeException(e);
                }
        a.add(0, StandardCharsets.UTF_8);
        a.add(StandardCharsets.ISO_8859_1);
        a.remove(Charset.defaultCharset());
        log.debug(() -> "init charsets: default=" + Charset.defaultCharset() + ", list=" + a);
        return () -> a;
    }

    public static Predicate<Path> createGrepFilter(List<String> patterns, Map<Path, List<FileLine>> grepped) {
        StringMatchingPredicate matcher = StringMatchingPredicate.create(patterns);
        return path -> {
            try {
                if (Files.isDirectory(path))
                    return false;
                List<FileLine> fileLines = doGrep(matcher, path);
                if (fileLines.isEmpty())
                    return false;
                grepped.put(path, fileLines);
                return true;
            } catch (IOException e) {
                log.warn(() -> "at createGrepFilter, " + e);
                System.err.printf("potf: '%s': cannot open file, cause=%s%n", path, e.getMessage());
                return false;
            }
        };
    }

    private static List<FileLine> doGrep(StringMatchingPredicate matcher, Path path) throws IOException {
        try {
            return grep(matcher, path, Charset.defaultCharset());
        } catch (IOException e) {
            log.debug(() -> "at doGrep, charset=default, e=" + e);
            for (Charset charset : charsetsExceptDefault().get())
                try {
                    return grep(matcher, path, charset);
                } catch (IOException e1) {
                    log.debug(() -> "at doGrep, charset=" + charset + ", e=" + e);
                }
            throw e;
        }
    }

    public static List<FileLine> grep(StringMatchingPredicate matcher, Path path, Charset charset) throws IOException {
        List<FileLine> fileLines = new ArrayList<>();
        try (LineNumberReader r = new LineNumberReader(Files.newBufferedReader(path, charset))) {
            while (true) {
                final String line = r.readLine();
                if (line == null)
                    break;
                if (matcher.matches(line))
                    fileLines.add(new FileLine(r.getLineNumber(), line));
            }
        }
        return fileLines;
    }

}
