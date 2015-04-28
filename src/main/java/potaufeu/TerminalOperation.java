package potaufeu;

import java.io.*;
import java.nio.file.*;
import java.util.function.*;

@FunctionalInterface
public interface TerminalOperation extends Consumer<Path> {

    static final String EOL = getEol();

    static String getEol() {
        if (System.getProperty("jline.terminal", "").equals("jline.UnixTerminal"))
            return "\n";
        return String.format("%n");
    }

    public static TerminalOperation with(PrintWriter out, OptionSet opts) {
        if (opts.isQuiet())
            return x -> {
            };
        Function<Path, String> path2s = (opts.isSlash()) ? path -> {
            StringBuilder sb = new StringBuilder();
            path.forEach(x -> sb.append('/').append(x));
            sb.delete(0, 1);
            if (path.isAbsolute())
                sb.insert(0, path.getRoot().toString().replace('\\', '/'));
            return sb.toString();
        } : Path::toString;
        FileAttributePrinter pf = new FileAttributePrinter(out, EOL, path2s);
        if (opts.isPrintsFullpath())
            return pf.fullPath();
        if (opts.isPrintsDetailList())
            return pf.detailList();
        if (opts.isPrintsPosixLikeList())
            return pf.posixLikeList();
        if (opts.isPrintsList())
            return pf.list();
        if (opts.isPrintsLineCount())
            return pf.linesCountList();
        return pf.path();
    }


}
