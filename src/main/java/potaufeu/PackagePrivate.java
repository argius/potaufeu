package potaufeu;

import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.stream.*;

/**
 * A class has package-private methods.
 * The methods in this class may be always removed.
 */
final class PackagePrivate {

    private PackagePrivate() {
    }

    static ResourceBundle getResourceBundle(Class<?> c) {
        final String s = c.getSimpleName();
        final String baseName = c.getPackage().getName() + '/' + s.substring(0, 1).toLowerCase() + s.substring(1);
        return ResourceBundle.getBundle(baseName);
    }

    static <T> Stream<T> flatten(Stream<Optional<T>> stream) {
        return stream.flatMap(opt -> {
            List<T> a = new ArrayList<>();
            opt.ifPresent(a::add);
            return a.stream();
        });
    }

    static PrintWriter asPrintWriter(PrintStream ps) {
        return new PrintWriter(ps, true);
    }

    /**
     * Return the string expression of specified object.
     * This method must only uses for loggings and debugs.
     * @param o object
     * @return string
     */
    static String toStringWithReflection(Object o) {
        Class<?> c = o.getClass();
        try {
            StringBuilder sb = new StringBuilder();
            for (Field field : c.getDeclaredFields()) {
                field.setAccessible(true);
                sb.append(String.format(",%s=%s", field.getName(), field.get(o)));
            }
            return String.format("%s(%s)", c.getSimpleName(), sb.substring(1));
        } catch (Exception e) {
            return String.format("Error: Class=%s, Exception=[%s]", c, e);
        }
    }

}
