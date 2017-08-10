package potaufeu;

import java.util.*;
import java.util.regex.*;

/**
 * String matching predicate is same as <code>Predicate&lt;String&gt;</code>,
 * except it has a factory method.
 */
@FunctionalInterface
public interface StringMatchingPredicate {

    boolean matches(String s);

    static StringMatchingPredicate and(StringMatchingPredicate pred1, StringMatchingPredicate pred2) {
        return s -> pred1.matches(s) && pred2.matches(s);
    }

    static StringMatchingPredicate or(StringMatchingPredicate pred1, StringMatchingPredicate pred2) {
        return s -> pred1.matches(s) || pred2.matches(s);
    }

    static StringMatchingPredicate create(String pattern) {
        if (pattern.startsWith("(?")) {
            // experimental
            Pattern p = Pattern.compile(pattern);
            return x -> p.matcher(x).find();
        }
        else
            return x -> x.contains(pattern);
    }

    static StringMatchingPredicate create(String... patterns) {
        return create(Arrays.asList(patterns));
    }

    static StringMatchingPredicate create(List<String> patterns) {
        switch (patterns.size()) {
            case 0:
                return x -> true;
            case 1:
                return create(patterns.get(0));
            default:
                return patterns.stream().map(x -> create(x)).reduce(StringMatchingPredicate::and).get();
        }
    }

}
