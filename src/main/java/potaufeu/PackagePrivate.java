package potaufeu;

import java.util.*;
import java.util.stream.*;

/**
 * A class has package-private methods.
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
            opt.ifPresent(x -> a.add(x));
            return a.stream();
        });
    }

}
