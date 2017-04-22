package potaufeu;

import java.io.*;
import java.util.*;

final class State {

    private static final Log log = Log.logger(State.class);

    private LinkedList<Result> results;
    private boolean stateMode;

    public State() {
        this.results = new LinkedList<>();
    }

    public boolean existsResult() {
        return !results.isEmpty();
    }

    public Result getFirstResult() {
        return results.getFirst();
    }

    public void pushResult(Result result) {
        results.addFirst(result);
    }

    public void pickResult(int index) {
        log.debug(() -> "pickResult " + index);
        int i = (index < 0) ? results.size() + index : index;
        results.addFirst(results.get(i));
    }

    public void shiftResults(int size) {
        log.debug(() -> "shiftResults " + size);
        for (int i = 0; !results.isEmpty() && i < size; i++)
            results.pollLast();
    }

    public void dropResults(int size) {
        if (size < 1)
            throw new IllegalArgumentException("dropResults requires 1+");
        log.debug(() -> "dropResults " + size);
        for (int i = 0; !results.isEmpty() && i < size; i++)
            results.pollFirst();
    }

    public void clearResults() {
        results.clear();
    }

    public String resultsSummary() {
        List<String> a = new ArrayList<>();
        int i = 0;
        for (Result r : results) {
            String s = (r.getName().isEmpty()) ? "" : ":" + r.getName();
            a.add(String.format("#%d%s(%d)", i++, s, r.matchedCount()));
        }
        return String.format("Results: %s", a);
    }

    public boolean isStateMode() {
        return stateMode;
    }

    public void setStateMode(boolean stateMode) {
        this.stateMode = stateMode;
    }

    public void controlBy(PrintWriter out, String commandLine) {
        // TODO experimental features
        Parameter p = Parameter.parse(commandLine.replaceFirst("^\\s*:", ""));
        switch (p.at(0)) {
        case "set-state-mode":
            if (p.has(1)) {
                String p1 = p.at(1);
                if (p1.matches("on|true|yes")) {
                    setStateMode(true);
                    out.println("state-mode: on");
                    break;
                }
                else if (p1.matches("off|false|no")) {
                    setStateMode(false);
                    out.println("state-mode: off");
                    break;
                }
            }
            out.println(":set-state-mode (on|off)");
            break;
        case "drop-results":
        case "drop":
            try {
                dropResults(getParameterArgAsInt(p, 1, 1));
                out.println(resultsSummary());
                break;
            } catch (NumberFormatException e) {
                // TODO err
                out.println(e);
            }
            out.println(":drop-results [ size ]");
            break;
        case "pick-result":
        case "pick":
            try {
                pickResult(getParameterArgAsInt(p, 1, -1));
                out.println(resultsSummary());
                break;
            } catch (NumberFormatException e) {
                // TODO err
                out.println(e);
            }
            out.println(":pick-result [ index ]");
            break;
        case "sort-result":
        case "sort":
            try {
                Result r = new Result();
                results.get(0).pathStream().forEach(x -> r.addPath(x));
                results.addFirst(r);
                break;
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                // TODO err
                out.println(e);
            }
            out.println(":pick-result [ index ]");
            break;
        case "label-result":
        case "label":
            if (p.has(2))
                try {
                    int p1 = getParameterArgAsInt(p, 1, 0);
                    String p2 = p.at(2);
                    results.get(p1).setName(p2);
                    break;
                } catch (NumberFormatException e) {
                    // TODO err
                    out.println(e);
                }
            out.println(":label-result index text");
            break;
        case "merge-result-or":
        case "mor":
            try {
                int p1 = getParameterArgAsInt(p, 1, 1);
                int p2 = getParameterArgAsInt(p, 2, 0);
                if (p1 != p2) {
                    Result r1 = results.get(p1);
                    Result r2 = results.get(p2);
                    results.add(0, r1.mergeOr(r2));
                    out.println(resultsSummary());
                    break;
                }
            } catch (NumberFormatException | IndexOutOfBoundsException e) {
                // TODO err
                out.println(e);
            }
            out.println(":merge-result-or [index1 [index2]]");
            out.println(":mor             [index1 [index2]]");
            break;
        case "p":
            out.println(resultsSummary());
            break;
        default:
            out.println("? unknown command");
        }
    }

    /**
     * @param p
     * @param index
     * @param defaultValue
     * @throws NumberFormatException
     *             arg is not a number
     * @return
     */
    private static int getParameterArgAsInt(Parameter p, int index, int defaultValue) {
        if (p.has(index))
            return Integer.parseInt(p.at(index));
        return defaultValue;
    }

    private static final class Parameter {

        private final String string;
        private final String[] array;
        private final int[] indices;

        private Parameter(String string, int[] indices, String[] array) {
            this.string = string;
            this.array = array;
            this.indices = indices;
        }

        static Parameter parse(String string) {
            char[] chars = string.toCharArray();
            int[] indices = indices(chars);
            return new Parameter(string, indices, array(chars, indices));
        }

        private static int[] indices(char[] chars) {
            List<Integer> a = new ArrayList<Integer>();
            boolean prev = true;
            boolean quoted = false;
            for (int i = 0; i < chars.length; i++) {
                final char c = chars[i];
                if (c == '"')
                    quoted = !quoted;
                final boolean f = isSpaceChar(c);
                if (!f && f != prev)
                    a.add(i);
                prev = !quoted && f;
            }
            a.add(chars.length);
            int[] indices = new int[a.size()];
            for (int i = 0; i < indices.length; i++)
                indices[i] = a.get(i);
            return indices;
        }

        private static String[] array(char[] chars, int[] indices) {
            String[] a = new String[indices.length - 1];
            for (int i = 0; i < a.length; i++) {
                final int offset = indices[i];
                int end = indices[i + 1];
                while (end > offset) {
                    if (!isSpaceChar(chars[end - 1]))
                        break;
                    --end;
                }
                final String s = String.valueOf(chars, offset, end - offset);
                a[i] = (chars[offset] == '"') ? s.substring(1, s.length() - 1) : s;
            }
            return a;
        }

        private static boolean isSpaceChar(char c) {
            switch (c) {
            case '\t':
            case '\n':
            case '\f':
            case '\r':
            case ' ':
                return true;
            default:
            }
            return false;
        }

        /**
         * Returns the parameter at the position specified index.
         *
         * @param index
         * @return
         */
        String at(int index) {
            return has(index) ? array[index] : "";
        }

        /**
         * Returns the parameter after the position specified index.
         *
         * @param index
         * @return
         */
        @SuppressWarnings("unused")
        String after(int index) {
            return has(index) ? string.substring(indices[index]) : "";
        }

        /**
         * Returns whether a parameter exists at the position specified index.
         *
         * @param index
         * @return
         */
        boolean has(int index) {
            if (index < 0)
                throw new IndexOutOfBoundsException("index >= 0: " + index);
            return index < array.length;
        }

        /**
         * Returns this parameter as an array.
         *
         * @return
         */
        @SuppressWarnings("unused")
        String[] asArray() {
            return array.clone();
        }

        /**
         * Returns this parameter as String. is not the same as
         * <code>toString</code>
         *
         * @return
         */
        @SuppressWarnings("unused")
        String asString() {
            return string;
        }

        @Override
        public String toString() {
            return "Parameter[" + string + "]";
        }

    }

}
