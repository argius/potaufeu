package potaufeu;

import static potaufeu.Messages.message;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;
import java.util.stream.*;
import jline.console.*;
import org.apache.commons.cli.*;
import org.apache.commons.io.*;
import org.apache.commons.lang3.builder.*;
import potaufeu.StreamOperation.Sampler;

public final class App {

    private static final Log log = Log.logger(App.class);

    private State state;
    private PrintWriter out;

    public App() {
        this.state = new State();
        this.out = new PrintWriter(System.out, true);
    }

    void find(OptionSet opts, boolean inLoop) {
        final String methodName = "find";
        log.info(() -> methodName + " start");
        final boolean verbose = opts.isVerbose();
        if (verbose)
            out.println(message("i.showCondition", VerboseMessages.patterns(opts), VerboseMessages.options(opts)));
        log.info(() -> "preparation");
        LongAdder count = new LongAdder();
        Predicate<Path> filter = integratedFilter(opts);
        Stream<Path> stream = createStream(opts, inLoop, count).filter(filter);
        log.info(() -> "running");
        final long startTime = System.currentTimeMillis();
        final long matchedCount;
        if (opts.isCollectsExtension())
            matchedCount = collectExtensions(stream, opts);
        else if (!opts.getGrepPatterns().isEmpty())
            matchedCount = filterPathsAndLines(stream, opts);
        else
            matchedCount = filterPaths(stream, opts);
        if (verbose)
            out.println(VerboseMessages.end(matchedCount, count.longValue(), System.currentTimeMillis() - startTime));
        log.info(() -> methodName + " end");
    }

    long filterPaths(Stream<Path> stream, OptionSet opts) {
        Sampler sampler = new Sampler(opts);
        Consumer<Path> terminalOp = TerminalOperation.with(out, opts);
        StreamOperation.of(stream).verbose(opts.isVerbose()).sorted(PathSorter.getSorter(opts.getSortKeys()))
                .sequential().peek(sampler).head(opts.getHeadCount()).tail(opts.getTailCount()).forEach(terminalOp);
        if (sampler.isResultRecorded)
            if (sampler.getResult().matchedCount() == 0)
                out.println(message("i.notFound"));
            else if (!state.isStateMode()
                     || sampler.getResult().matchedCount() != state.getFirstResult().matchedCount()) {
                state.pushResult(sampler.getResult());
                out.println(state.resultsSummary());
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
        // TODO check sort keys
        Optional<Comparator<String>> sorter =
            opts.getSortKeys().isEmpty() ? Optional.empty() : Optional.of(String.CASE_INSENSITIVE_ORDER);
        StreamOperation.of(stExt).peek(x -> count.increment()).sorted(sorter).head(opts.getHeadCount())
                .tail(opts.getTailCount()).getStream().forEach(out::println);
        return count.longValue();
    }

    long filterPathsAndLines(Stream<Path> stream, OptionSet opts) {
        Map<Path, List<FileLine>> grepped = new HashMap<>();
        List<String> patterns = opts.getGrepPatterns();
        int patternSize = patterns.size();
        Predicate<Path> grepFilter = path -> {
            try {
                List<String> lines = Files.readAllLines(path);
                List<FileLine> fileLines = new ArrayList<>();
                int i = 0;
                for (String line : lines) {
                    ++i;
                    int c = 0;
                    for (String ptn : patterns)
                        if (line.contains(ptn))
                            ++c;
                    if (c == patternSize)
                        fileLines.add(new FileLine(i, line));
                }
                if (!fileLines.isEmpty()) {
                    grepped.put(path, fileLines);
                    return true;
                }
                return false;
            } catch (IOException e) {
                log.error(() -> "in filterPathsAndLines", e);
                return false;
            }
        };
        Consumer<Path> greppedAction = path -> {
            for (FileLine line : grepped.get(path))
                out.printf("%s:%d:%s%n", path, line.number, line.text);
        };
        if (opts.isState()) {
            Result r = new Result();
            stream.filter(grepFilter).peek(x -> r.addPath(x)).forEach(greppedAction);
            if (grepped.isEmpty())
                out.print(message("i.notFound"));
            else if (!state.existsResult() || grepped.size() != state.getFirstResult().grepped.size()) {
                state.pushResult(r);
                out.println(state.resultsSummary());
            }
            return r.matchedCount();
        }
        else {
            LongAdder count = new LongAdder();
            stream.peek(x -> count.increment()).filter(grepFilter).forEach(greppedAction);
            return count.longValue();
        }
    }

    private Predicate<Path> integratedFilter(OptionSet opts) {
        List<FileFilter> a = new ArrayList<>();
        a.addAll(FileFilterFactory.nameFilters(opts));
        FileFilterFactory.extensionFilters(opts).ifPresent(a::add);
        a.addAll(FileFilterFactory.exclusionFilters(opts));
        a.addAll(FileFilterFactory.pathFilters(opts));
        a.addAll(FileFilterFactory.fileTypeFilters(opts));
        a.addAll(FileFilterFactory.fileSizeFilters(opts));
        a.addAll(FileFilterFactory.ctimeFilters(opts));
        a.addAll(FileFilterFactory.mtimeFilters(opts));
        a.addAll(FileFilterFactory.atimeFilters(opts));
        return x -> {
            // XXX why path stream contains null ?
            if (x == null)
                return false;
            File f = x.toFile();
            for (FileFilter filter : a)
                if (!filter.accept(f))
                    return false;
            return true;
        };
    }

    Stream<Path> createStream(OptionSet opts, boolean inLoop, LongAdder count) {
        if (!opts.isState() & inLoop && state.existsResult()) {
            count.add(state.getFirstResult().matchedCount());
            // TODO add depth filter
            return state.getFirstResult().pathStream().parallel();
        }
        final int maxDepth = opts.getMaxDepth().orElse(Integer.MAX_VALUE);
        return PathIterator.streamOf(opts.getRootPath(), maxDepth).peek(path -> count.increment());
    }

    static String version() {
        StringBuilder sb = new StringBuilder();
        sb.append(message(".productName")).append(" version ");
        try (InputStream is = App.class.getResourceAsStream("version")) {
            if (is == null)
                sb.append("???");
            else
                sb.append(String.join("", IOUtils.readLines(is)));
        } catch (IOException e) {
            log.warn(() -> "App.version", e);
            sb.append("?");
        }
        return sb.toString();
    }

    void loop() {
        try {
            ConsoleReader cr = new ConsoleReader();
            cr.setBellEnabled(false);
            cr.setPrompt("> ");
            this.out = new PrintWriter(cr.getOutput(), true);
            out.println(version());
            while (true) {
                final String line = cr.readLine();
                if (line == null || line.matches("(exit|quit)"))
                    break;
                if (line.equals("cls"))
                    cr.clearScreen();
                else if (line.trim().isEmpty())
                    out.printf("%s %s%n", message("i.usagePrefix"), message("i.usageInLoop"));
                else if (line.matches("\\s*:.*"))
                    state.controlBy(out, line);
                else
                    try {
                        OptionSet opts = new OptionSet.Parser().parse(line.split(" "));
                        find(opts, true);
                    } catch (Exception e) {
                        out.println(message("e.0", e.getMessage()));
                    }
            }
        } catch (IOException e) {
            log.error(() -> "(new ConsoleReader)", e);
            out.println(message("e.0", e.getMessage()));
        }
    }

    public static void main(String[] args) {
        log.info(() -> "start (version: " + version() + ")");
        log.debug(() -> "args=" + Arrays.asList(args));
        try {
            OptionSet.Parser parser = new OptionSet.Parser();
            OptionSet opts = parser.parse(args);
            log.info(() -> "opts=" + ReflectionToStringBuilder.toString(opts, ToStringStyle.SHORT_PREFIX_STYLE));
            if (opts.isShowVersion())
                System.out.println(version());
            else if (opts.isHelp())
                new HelpFormatter().printHelp(message("i.usage"), parser.getOptions());
            else {
                App app = new App();
                app.find(opts, false);
                if (opts.isState())
                    app.loop();
            }
        } catch (Throwable e) {
            log.error(() -> "(main)", e);
            System.out.println(message("e.0", e.getMessage()));
        }
        log.info(() -> "end");
    }

}
