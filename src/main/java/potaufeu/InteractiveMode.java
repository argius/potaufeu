package potaufeu;

import java.io.*;
import java.util.*;
import jline.console.*;

final class InteractiveMode {

    private static final Log log = Log.logger(InteractiveMode.class);

    private InteractiveMode() { //empty
    }

    static void start(App app, PrintWriter out, ConsoleReader cr) {
        try {
            while (true) {
                final String line = cr.readLine();
                if (line == null || line.matches(":(exit|quit)"))
                    break;
                else if (line.trim().isEmpty())
                    continue; // do nothing
                else if (line.equals(":cls"))
                    cr.clearScreen();
                else if (line.matches("\\s*:.*"))
                    controlResults(app.results, out, line);
                else
                    try {
                        app.runCommand(OptionSet.parseArguments(line.split(" ")));
                    } catch (Exception e) {
                        log.warn(() -> "", e);
                    }
            }
        } catch (IOException e) {
            log.error(() -> "(while)", e);
        }
        log.info(() -> "exit interactive mode");
    }

    static void controlResults(ResultList results, PrintWriter out, String commandLine) {
        Parameter p = Parameter.parse(commandLine.replaceFirst("^\\s*:", ""));
        switch (p.at(0)) {
            case "drop":
                try {
                    results.drop(getParameterArgAsInt(p, 1, 1));
                    out.println(results.summary());
                    break;
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    // TODO err
                    out.println(e);
                }
                break;
            case "pick":
                try {
                    results.pick(getParameterArgAsInt(p, 1, -1));
                    out.println(results.summary());
                    break;
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    // TODO err
                    out.println(e);
                }
                break;
            case "sort":
                try {
                    Result r = new Result();
                    results.get(0).pathStream().forEach(r::addPath);
                    results.addFirst(r);
                    out.println(results.summary());
                    break;
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    // TODO err
                    out.println(e);
                }
                break;
            case "label":
                if (p.has(2))
                    try {
                        int p1 = getParameterArgAsInt(p, 1, 0);
                        String p2 = p.at(2);
                        results.get(p1).setName(p2);
                        out.println(results.summary());
                        break;
                    } catch (NumberFormatException e) {
                        // TODO err
                        out.println(e);
                    }
                break;
            case "merge":
                try {
                    int p1 = getParameterArgAsInt(p, 1, 1);
                    int p2 = getParameterArgAsInt(p, 2, 0);
                    if (p1 != p2) {
                        Result r1 = results.get(p1);
                        Result r2 = results.get(p2);
                        results.add(0, r1.mergeOr(r2));
                        out.println(results.summary());
                        break;
                    }
                } catch (NumberFormatException | IndexOutOfBoundsException e) {
                    // TODO err
                    out.println(e);
                }
                break;
            case "print":
            case "p":
                out.println(results.summary());
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
