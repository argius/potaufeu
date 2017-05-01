package potaufeu;

import static potaufeu.Messages.message;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import org.apache.commons.cli.*;

/**
 * A set of patterns and options.
 */
public final class OptionSet {

    public final long createdTime;

    private final Path rootPath;

    private List<String> directories;
    private List<String> pathPatterns;
    private List<String> extensionPatterns;
    private List<String> namePatterns;
    private List<String> exclusionPatterns;
    private List<String> fileSizePatterns;
    private List<String> mtimePatterns;
    private List<String> ctimePatterns;
    private List<String> atimePatterns;
    private List<String> grepPatterns;
    private boolean file;
    private boolean text;
    private boolean quiet;
    private boolean printsFullpath;
    private boolean printsList;
    private boolean printsPosixLikeList;
    private boolean printsDetailList;
    private boolean printsLineCount;
    private OptionalInt maxDepth;
    private OptionalInt headCount;
    private OptionalInt tailCount;
    private boolean collectsExtension;
    private List<String> sortKeys;
    private boolean slash;
    private boolean interactive;
    private boolean verbose;
    private boolean showVersion;
    private boolean help;

    private OptionSet() {
        this.createdTime = System.currentTimeMillis();
        this.rootPath = Paths.get("");
    }

    public static OptionSet parseArguments(String[] args) throws Exception {
        OptionSet.Parser parser = new OptionSet.Parser();
        return parser.parse(args);
    }

    public Path getRootPath() {
        return rootPath;
    }

    public List<String> getDirectories() {
        return directories;
    }

    public List<String> getPathPatterns() {
        return pathPatterns;
    }

    public List<String> getExtensionPatterns() {
        return extensionPatterns;
    }

    public List<String> getNamePatterns() {
        return namePatterns;
    }

    public List<String> getExclusionPatterns() {
        return exclusionPatterns;
    }

    public List<String> getFileSizePatterns() {
        return fileSizePatterns;
    }

    public List<String> getMtimePatterns() {
        return mtimePatterns;
    }

    public List<String> getCtimePatterns() {
        return ctimePatterns;
    }

    public List<String> getAtimePatterns() {
        return atimePatterns;
    }

    public List<String> getGrepPatterns() {
        return grepPatterns;
    }

    public boolean isFile() {
        return file;
    }

    public boolean isText() {
        return text;
    }

    public boolean isQuiet() {
        return quiet;
    }

    public boolean isPrintsFullpath() {
        return printsFullpath;
    }

    public boolean isPrintsList() {
        return printsList;
    }

    public boolean isPrintsPosixLikeList() {
        return printsPosixLikeList;
    }

    public boolean isPrintsDetailList() {
        return printsDetailList;
    }

    public boolean isPrintsLineCount() {
        return printsLineCount;
    }

    public OptionalInt getMaxDepth() {
        return maxDepth;
    }

    public OptionalInt getHeadCount() {
        return headCount;
    }

    public OptionalInt getTailCount() {
        return tailCount;
    }

    public boolean isCollectsExtension() {
        return collectsExtension;
    }

    public List<String> getSortKeys() {
        return sortKeys;
    }

    public boolean isSlash() {
        return slash;
    }

    public boolean isInteractive() {
        return interactive;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public boolean isShowVersion() {
        return showVersion;
    }

    public boolean isHelp() {
        return help;
    }

    /**
     * The parser for OptionSet.
     */
    public static final class Parser {

        private static final Log log = Log.logger(Parser.class);

        // filters
        private static final String OPTION_NAME = "name";
        private static final String OPTION_EXCLUDE = "exclude";
        private static final String OPTION_SIZE = "size";
        private static final String OPTION_CTIME = "ctime";
        private static final String OPTION_MTIME = "mtime";
        private static final String OPTION_ATIME = "atime";
        private static final String OPTION_FILE = "file";
        private static final String OPTION_TEXT = "text";
        private static final String OPTION_GREP = "grep";
        // outputs
        private static final String OPTION_QUIET = "quiet";
        private static final String OPTION_FULLPATH = "fullpath";
        private static final String OPTION_LIST = "list";
        private static final String OPTION_LIST_POSIX = "list-posix";
        private static final String OPTION_LIST_DETAIL = "list-detail";
        private static final String OPTION_LIST_LINES = "list-lines";
        // limitters
        private static final String OPTION_DEPTH = "depth";
        private static final String OPTION_HEAD = "head";
        private static final String OPTION_HEADS = "heads";
        private static final String OPTION_TAIL = "tail";
        private static final String OPTION_TAILS = "tails";
        // others
        private static final String OPTION_DIR = "dir";
        private static final String OPTION_EXTENSIONS = "exts";
        private static final String OPTION_SORT = "sort";
        private static final String OPTION_SLASH = "slash";
        private static final String OPTION_INTERACTIVE = "interactive";
        private static final String OPTION_VERBOSE = "verbose";
        private static final String OPTION_VERSION = "version";
        private static final String OPTION_HELP = "help";

        private final Options options;

        public Parser() {
            this.options = new Options();
            option(OPTION_NAME, "n", true);
            option(OPTION_EXCLUDE, "x", true);
            option(OPTION_SIZE, "s", true);
            option(OPTION_CTIME, true);
            option(OPTION_MTIME, "t", true);
            option(OPTION_ATIME, true);
            option(OPTION_FILE, "F");
            option(OPTION_TEXT, "T");
            option(OPTION_GREP, "g", true);
            option(OPTION_DIR, "d", true);
            option(OPTION_QUIET, "q");
            option(OPTION_FULLPATH);
            option(OPTION_LIST, "l");
            option(OPTION_LIST_POSIX, "L");
            option(OPTION_LIST_DETAIL);
            option(OPTION_LIST_LINES);
            option(OPTION_DEPTH, true);
            option(OPTION_HEAD, true);
            option(OPTION_HEADS);
            option(OPTION_TAIL, true);
            option(OPTION_TAILS);
            option(OPTION_EXTENSIONS);
            option(OPTION_SORT, "S", true);
            option(OPTION_SLASH);
            option(OPTION_INTERACTIVE, "i");
            option(OPTION_VERBOSE);
            option(OPTION_VERSION);
            option(OPTION_HELP);
        }

        public Options getOptions() {
            return options;
        }

        public OptionSet parse(String... args) throws Exception {
            OptionSet o = new OptionSet();
            CommandLineParser parser = new PosixParser();
            CommandLine cl = parser.parse(options, args);
            o.namePatterns = stringValues(cl, OPTION_NAME);
            o.exclusionPatterns = stringValues(cl, OPTION_EXCLUDE);
            o.fileSizePatterns = stringValues(cl, OPTION_SIZE);
            o.ctimePatterns = stringValues(cl, OPTION_CTIME);
            o.mtimePatterns = stringValues(cl, OPTION_MTIME);
            o.atimePatterns = stringValues(cl, OPTION_ATIME);
            o.file = bool(cl, OPTION_FILE);
            o.text = bool(cl, OPTION_TEXT);
            o.grepPatterns = stringValues(cl, OPTION_GREP);
            o.directories = stringValues(cl, OPTION_DIR);
            o.quiet = bool(cl, OPTION_QUIET);
            o.printsFullpath = bool(cl, OPTION_FULLPATH);
            o.printsList = bool(cl, OPTION_LIST);
            o.printsPosixLikeList = bool(cl, OPTION_LIST_POSIX);
            o.printsDetailList = bool(cl, OPTION_LIST_DETAIL);
            o.printsLineCount = bool(cl, OPTION_LIST_LINES);
            o.maxDepth = optIntValue(cl, OPTION_DEPTH);
            o.headCount = optIntValue(cl, OPTION_HEAD, OPTION_HEADS, 10);
            o.tailCount = optIntValue(cl, OPTION_TAIL, OPTION_TAILS, 10);
            o.collectsExtension = bool(cl, OPTION_EXTENSIONS);
            o.sortKeys = sortKeys(cl);
            o.slash = bool(cl, OPTION_SLASH);
            o.interactive = bool(cl, OPTION_INTERACTIVE);
            o.verbose = bool(cl, OPTION_VERBOSE);
            o.showVersion = bool(cl, OPTION_VERSION);
            o.help = bool(cl, OPTION_HELP);
            log.debug(() -> "non-option args=" + cl.getArgList());
            List<String> extPtns = new ArrayList<>();
            List<String> pathPtns = new ArrayList<>();
            for (final String arg : cl.getArgs())
                if (arg.startsWith("."))
                    extPtns.addAll(Arrays.asList(arg.substring(1).split(",")));
                else
                    pathPtns.add(arg);
            o.pathPatterns = Collections.unmodifiableList(pathPtns);
            o.extensionPatterns = Collections.unmodifiableList(extPtns);
            return o;
        }

        static List<String> sortKeys(CommandLine cl) {
            return stringValues(cl, OPTION_SORT).stream().map(x -> Arrays.asList(x.split(",")))
                    .flatMap(Collection::stream).collect(Collectors.toList());
        }

        Option option(String optionKey) {
            return option(optionKey, null, false);
        }

        Option option(String optionKey, boolean requiresArgument) {
            return option(optionKey, null, requiresArgument);
        }

        Option option(String optionKey, String shortKey) {
            return option(optionKey, shortKey, false);
        }

        Option option(String optionKey, String shortKey, boolean requiresArgument) {
            String desc = message("opt." + optionKey);
            Option opt = new Option(shortKey, optionKey, requiresArgument, desc);
            options.addOption(opt);
            return opt;
        }

        static boolean bool(CommandLine cl, String optionKey) {
            final boolean hasOption = cl.hasOption(optionKey);
            log.debug(() -> String.format("option: hasOption=%s, key=%s", (hasOption ? "T" : "F"), optionKey));
            return hasOption;
        }

        static OptionalInt optIntValue(CommandLine cl, String optionKey) {
            log.debug(() -> String.format("option: hasOption=%s, key=%s, value=%s",
                (cl.hasOption(optionKey) ? "T" : "F"), optionKey, cl.getOptionValue(optionKey)));
            return optInt(cl, optionKey);
        }

        static OptionalInt optIntValue(CommandLine cl, String optionKey, String optionKey2, int value2) {
            log.debug(() -> String.format("option: hasOption=%s, key=%s, value=%s, key2=%s, value2=%d",
                (cl.hasOption(optionKey) ? "T" : "F"), optionKey, cl.getOptionValue(optionKey), optionKey2, value2));
            OptionalInt v = optInt(cl, optionKey);
            if (v.isPresent())
                return v;
            return bool(cl, optionKey2) ? OptionalInt.of(value2) : OptionalInt.empty();
        }

        private static OptionalInt optInt(CommandLine cl, String optionKey) {
            if (!cl.hasOption(optionKey))
                return OptionalInt.empty();
            String v = cl.getOptionValue(optionKey);
            try {
                return OptionalInt.of(Integer.parseUnsignedInt(v));
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException(message("e.argOptionMustPositiveNumber", optionKey, v), e);
            }
        }

        static List<String> stringValues(CommandLine cl, String optionKey) {
            String[] values = cl.getOptionValues(optionKey);
            String[] a = (values == null) ? new String[0] : values;
            log.debug(() -> String.format("option: hasOption=%s, key=%s, values=%s",
                (cl.hasOption(optionKey) ? "T" : "F"), optionKey, Arrays.toString(a)));
            return Arrays.asList(a);
        }

    }

}
