package potaufeu;

import java.util.*;

/**
 * ResultList provides operations to control results, mainly for interactive mode.
 */
final class ResultList extends LinkedList<Result> {

    ResultList() { // empty
    }

    ResultList(Result firstResult) {
        add(firstResult);
    }

    /**
     * Picks a specified element and add first.
     * @param index the index of a specified element
     */
    void pick(int index) {
        int i = (index < 0) ? size() + index : index;
        push(get(i));
    }

    /**
     * Shifts elements as many times as specified.
     * @param ntimes the number of times
     */
    void shift(int ntimes) {
        for (int i = 0; !isEmpty() && i < ntimes; i++)
            pollLast();
    }

    /**
     * Removes the specified number of elements from the beginning of this list.
     * @param n number of elements to remove
     */
    public void drop(int n) {
        if (n < 1)
            throw new IllegalArgumentException("drop requires 1+");
        for (int i = 0; !isEmpty() && i < n; i++)
            pollFirst();
    }

    /**
     * Returns the summary of results as string.
     * @return the summary of results
     */
    public String summary() {
        List<String> a = new ArrayList<>();
        int i = 0;
        for (Result r : this) {
            String s = (r.getName().isEmpty()) ? "" : ":" + r.getName();
            a.add(String.format("#%d%s(%d)", i++, s, r.matchedCount()));
        }
        return String.format("Results: %s", a);
    }

}
