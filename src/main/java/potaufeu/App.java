package potaufeu;

import static potaufeu.Messages.message;
import static potaufeu.PackagePrivate.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;
import org.apache.commons.cli.*;
import jline.console.*;
import potaufeu.StreamOperation.*;

public final class App {

    private static final Log log = Log.logger(App.class);

    final ResultList results;

    private PrintWriter out;
    private boolean interactive;

    public App() {
        this.results = new ResultList();
        this.out = asPrintWriter(System.out);
    }

    void find(OptionSet opts) {
        final String methodName = "find";
        log.info(() -> methodName + " start");
        final boolean verbose = opts.isVerbose();
        if (verbose)
            out.println(message("i.showCondition", VerboseMessages.patterns(opts), VerboseMessages.options(opts)));
        log.info(() -> "preparation");
        LongAdder count = new LongAdder();
        Predicate<Path> filter = integratedFilter(opts);
        Stream<Path> stream = createStream(opts, count).filter(filter);
        log.info(() -> "running");
        final long startTime = System.currentTimeMillis();
        final long matchedCount;
        if (!opts.getGrepPatterns().isEmpty())
            matchedCount = filterPathsAndLines(stream, opts);
        else if (opts.isCollectsExtension())
            matchedCount = collectExtensions(stream, opts);
        else
            matchedCount = filterPaths(stream, opts);
        if (verbose)
            out.println(VerboseMessages.end(matchedCount, count.longValue(), System.currentTimeMillis() - startTime));
        log.info(() -> methodName + " end");
    }

    long filterPaths(Stream<Path> stream, OptionSet opts) {
        final boolean createsResult = opts.isInteractive() || interactive;
        final boolean verbose = opts.isVerbose();
        Sampler sampler = new Sampler(createsResult, verbose);
        StreamOperation.of(stream).verbose(verbose).sorted(PathSorter.getSorter(opts.getSortKeys())).sequential()
                .peek(sampler).head(opts.getHeadCount()).tail(opts.getTailCount()).getStream()
                .forEachOrdered(TerminalOperation.with(out, opts));
        if (sampler.isResultRecorded)
            if (sampler.getResult().matchedCount() == 0)
                out.println(message("i.notFound"));
            else if (createsResult && (results.isEmpty() || !opts.getDirectories().isEmpty()
                                       || sampler.getResult().matchedCount() != results.getFirst().matchedCount())) {
                results.push(sampler.getResult());
                out.println(results.summary());
            }
        if (sampler.isCounted)
            return sampler.getCount().longValue();
        else if (sampler.isResultRecorded)
            return sampler.getResult().matchedCount();
        return -1L;
    }

    long collectExtensions(Stream<Path> stream, OptionSet opts) {
        LongAdder count = new LongAdder();
        Stream<String> stExt = stream.map(StreamOperation::pathToExtension).distinct();
        // the sorting support has been removed on 2017-04-21
        StreamOperation.of(stExt).peek(x -> count.increment()).head(opts.getHeadCount()).tail(opts.getTailCount())
                .getStream().forEach(out::println);
        return count.longValue();
    }

    long filterPathsAndLines(Stream<Path> stream, OptionSet opts) {
        Map<Path, List<FileLine>> grepped = new HashMap<>();
        Predicate<Path> grepFilter = createGrepFilter(opts.getGrepPatterns(), grepped);
        if (opts.isCollectsExtension())
            return collectExtensions(stream.filter(grepFilter), opts);
        TerminalOperation action = TerminalOperation.with(out, opts);
        if (action == TerminalOperation.NOT_FOR_PATH) {
            Function<Path, String> path2s = TerminalOperation.path2s(opts);
            action = path -> {
                for (FileLine line : grepped.get(path))
                    out.printf("%s:%d:%s%n", path2s.apply(path), line.number, line.text);
            };
        }
        if (opts.isInteractive()) {
            Result r = new Result();
            stream.filter(grepFilter).peek(r::addPath).forEachOrdered(action);
            if (grepped.isEmpty())
                out.print(message("i.notFound"));
            else if (results.isEmpty() || grepped.size() != results.getFirst().getLineCount()) {
                results.push(r);
                out.println(results.summary());
            }
            return r.matchedCount();
        }
        else {
            LongAdder count = new LongAdder();
            stream.peek(x -> count.increment()).filter(grepFilter).forEachOrdered(action);
            return count.longValue();
        }
    }

    private Predicate<Path> integratedFilter(OptionSet opts) {
        List<PathMatcher> a = new ArrayList<>();
        a.addAll(PathMatcherFactory.nameMatchers(opts));
        PathMatcherFactory.extensionMatchers(opts).ifPresent(a::add);
        a.addAll(PathMatcherFactory.exclusionMatchers(opts));
        a.addAll(PathMatcherFactory.pathMatchers(opts));
        a.addAll(PathMatcherFactory.fileTypeMatchers(opts));
        a.addAll(PathMatcherFactory.fileSizeMatchers(opts));
        a.addAll(PathMatcherFactory.ctimeMatchers(opts));
        a.addAll(PathMatcherFactory.mtimeMatchers(opts));
        a.addAll(PathMatcherFactory.atimeMatchers(opts));
        a.addAll(PathMatcherFactory.fileContentTypeMatchers(opts));
        return x -> {
            // XXX why path stream contains null ?
            if (x == null)
                return false;
            for (PathMatcher matcher : a)
                if (!matcher.matches(x))
                    return false;
            return true;
        };
    }

    private static Predicate<Path> createGrepFilter(List<String> patterns, Map<Path, List<FileLine>> grepped) {
        return path -> {
            List<FileLine> fileLines = new ArrayList<>();
            try (LineNumberReader r = new LineNumberReader(Files.newBufferedReader(path))) {
                while (true) {
                    final String line = r.readLine();
                    if (line == null)
                        break;
                    int c = 0;
                    for (String ptn : patterns)
                        if (line.contains(ptn))
                            ++c;
                    if (c == patterns.size())
                        fileLines.add(new FileLine(r.getLineNumber(), line));
                }
            } catch (IOException e) {
                log.warn(() -> "at createGrepFilter, " + e);
                System.err.printf("potf: '%s': cannot open file, cause=%s%n", path, e.getMessage());
                return false;
            }
            if (fileLines.isEmpty())
                return false;
            grepped.put(path, fileLines);
            return true;
        };
    }

    Stream<Path> createStream(OptionSet opts, LongAdder count) {
        final int maxDepth = opts.getMaxDepth().orElse(Integer.MAX_VALUE);
        if (!results.isEmpty() && opts.getDirectories().isEmpty()) {
            // from cached result
            log.debug(() -> "create stream from cached result");
            Result firstResult = results.getFirst();
            count.add(firstResult.matchedCount());
            if (opts.getMaxDepth().isPresent()) {
                final int maxDepthPlus1 = maxDepth + 1;
                return firstResult.pathStream().filter(x -> x.getNameCount() <= maxDepthPlus1).parallel();
            }
            return firstResult.pathStream().parallel();
        }
        if (isStdinAvailable()) {
            // from stdin
            log.debug(() -> "create stream from stdin");
            return createPathStreamFromStdin();
        }
        // new path stream
        log.debug(() -> "create new path stream");
        List<Path> dirs = new ArrayList<>();
        for (String dirString : opts.getDirectories()) {
            Path dir = Paths.get(dirString);
            if (dirs.contains(dir))
                System.err.println(message("w.duplicatedir", dirString));
            else if (!Files.isDirectory(dir))
                throw new IllegalArgumentException(message("e.noSuchDir", dirString));
            else
                dirs.add(dir);
        }
        if (dirs.isEmpty())
            dirs.add(opts.getRootPath());
        log.debug(() -> "stream concatenation, dirs = " + dirs);
        log.debug(() -> "isIgnoreAccessDenied = " + opts.isIgnoreAccessDenied());
        return dirs.stream().map(dir -> PathIterator.streamOf(dir, maxDepth, opts.isIgnoreAccessDenied()))
                .reduce(Stream::concat).orElseGet(Stream::empty).peek(path -> count.increment());
    }

    @SuppressWarnings("resource")
    private Stream<Path> createPathStreamFromStdin() {
        // XXX trick for suppress warnings
        Stream<String> pathStringStream =
            StreamSupport.stream(Spliterators.spliteratorUnknownSize(new Scanner(System.in), 0), false);
        return pathStringStream.map(Paths::get);
    }

    private static boolean isStdinAvailable() {
        try {
            int stdinAvailable = System.in.available();
            log.debug(() -> "stdinAvailable=" + stdinAvailable);
            return stdinAvailable > 0;
        } catch (IOException e) {
            log.warn(() -> "", e);
        }
        return false;
    }

    public void showHelp() {
        HelpFormatter hf = new HelpFormatter();
        hf.setSyntaxPrefix(message("i.usagePrefix"));
        String usage = message("i.usage");
        String header = message("help.header");
        String footer = message("help.footer");
        hf.printHelp(out, 80, usage, header, new OptionSet.Parser().getOptions(), 2, 2, footer, true);
    }

    public static String version() {
        StringBuilder sb = new StringBuilder();
        sb.append(message(".productName")).append(" version ");
        try (InputStream is = App.class.getResourceAsStream("version")) {
            if (is == null)
                sb.append("???");
            else {
                @SuppressWarnings("resource")
                Scanner sc = new Scanner(is);
                sb.append(sc.nextLine());
            }
        } catch (IOException | NoSuchElementException e) {
            log.warn(() -> "App.version", e);
            sb.append("?");
        }
        return sb.toString();
    }

    public void startInteraction() {
        ConsoleReader cr;
        try {
            cr = new ConsoleReader();
        } catch (IOException e) {
            log.error(() -> "(new ConsoleReader)", e);
            out.println(message("e.0", e.getMessage()));
            return;
        }
        cr.setBellEnabled(false);
        cr.setPrompt("> ");
        this.out = new PrintWriter(cr.getOutput(), true);
        out.println();
        out.println(version());
        out.println(message("i.startInteractiveMode"));
        this.interactive = true;
        InteractiveMode.start(this, out, cr);
    }

    public void runCommand(OptionSet opts) {
        if (opts.isShowVersion())
            out.println(version());
        else if (opts.isHelp())
            showHelp();
        else
            find(opts);
    }

    public static void main(String[] args) {
        log.info(() -> "start (version: " + version() + ")");
        log.debug(() -> "args=" + Arrays.asList(args));
        try {
            OptionSet opts = OptionSet.parseArguments(args);
            log.info(() -> "opts=" + toStringWithReflection(opts));
            App app = new App();
            app.runCommand(opts);
            if (opts.isInteractive())
                app.startInteraction();
        } catch (Throwable e) {
            log.error(() -> "(main)", e);
            System.err.println(message("e.0", e.getMessage()));
        }
        log.info(() -> "end");
    }

}
