package potaufeu;

import java.nio.file.*;
import java.util.*;

@FunctionalInterface
public interface PathSorter extends Comparator<Path> {

    public static Optional<Comparator<Path>> getSorter(List<String> sortExprs) {
        return sortExprs.stream().map(x -> createComparator(x)).reduce((x, y) -> x.thenComparing(y));
    }

    static Comparator<Path> createComparator(String expr) {
        final boolean desc = expr.startsWith("_");
        final String key;
        if (desc)
            key = expr.substring(1);
        else if (expr.startsWith("+"))
            key = expr.substring(1);
        else
            key = expr;
        Comparator<Path> tmp;
        switch (key) {
            case "size":
            case "ctime":
            case "mtime":
            case "atime":
                tmp = Comparator.comparing(FileAttributeFormatter.toLongLambda(key));
                break;
            case "name": {
                tmp = Comparator.comparing(FileAttributeFormatter::name);
            }
                break;
            case "iname":
                // TODO use String.CASE_INSENSITIVE_ORDER instead ?
                tmp = Comparator.comparing(x -> FileAttributeFormatter.name(x).toLowerCase());
                break;
            default:
                throw new IllegalArgumentException("unknown sortkey: " + expr);
        }
        return (desc) ? tmp.reversed() : tmp;
    }

}
