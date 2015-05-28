package potaufeu;

import static potaufeu.Messages.message;
import static potaufeu.PackagePrivate.flatten;
import java.util.*;
import java.util.stream.*;

/**
 * Messages for `verbose' option.
 */
public final class VerboseMessages {

    private VerboseMessages() {
    }

    public static String patterns(OptionSet opts) {
        return flatten(
            Stream.of(format("path", opts.getPathPatterns()), format("exts", opts.getExtensionPatterns()),
                format("name", opts.getNamePatterns()), format("exclude", opts.getExclusionPatterns()),
                format("size", opts.getFileSizePatterns()), format("ctime", opts.getCtimePatterns()),
                format("mtime", opts.getMtimePatterns()), format("atime", opts.getAtimePatterns()),
                format("file only", opts.isFile()))).collect(Collectors.joining(", "));
    }

    public static String options(OptionSet opts) {
        return flatten(
            Stream.of(format("dir", opts.getDirectories()), format("quiet", opts.isQuiet()),
                format("full path", opts.isPrintsFullpath()), format("list", opts.isPrintsList()),
                format("list-posix", opts.isPrintsPosixLikeList()), format("list-detail", opts.isPrintsDetailList()),
                format("list-linecount", opts.isPrintsLineCount()), format("sortkeys", opts.getSortKeys()),
                format("slash", opts.isSlash()), format("head", opts.getHeadCount()),
                format("tail", opts.getTailCount()))).collect(Collectors.joining(", "));
    }

    public static String end(long matchedCount, long allFileCount, long elapsed) {
        return message("i.verboseEndMessage", matchedCount, allFileCount, elapsed / 1_000f);
    }

    private static Optional<String> format(String optionKey, Collection<String> a) {
        return (a.isEmpty()) ? Optional.empty() : Optional.of(String.format("%s:%s", optionKey, a));
    }

    private static Optional<String> format(String optionKey, boolean exists) {
        return (exists) ? Optional.of(optionKey) : Optional.empty();
    }

    private static Optional<String> format(String optionKey, OptionalInt optInt) {
        return (optInt.isPresent()) ? Optional.of(optionKey + " " + optInt.getAsInt()) : Optional.empty();
    }

}
