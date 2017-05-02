package potaufeu;

import static potaufeu.Messages.message;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.stream.*;
import jline.console.*;

final class InteractiveMode {

    private static final Log log = Log.logger(InteractiveMode.class);
    private static final String savefileSuffix = ".ss.bin";

    private InteractiveMode() { //empty
    }

    static void start(App app, PrintWriter out, ConsoleReader cr) {
        while (true) {
            final String line;
            try {
                line = cr.readLine();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            try {
                if (line == null || line.matches(":(exit|quit)"))
                    break;
                else if (line.trim().isEmpty())
                    continue; // do nothing
                else if (line.equals(":cls"))
                    cr.clearScreen();
                else if (line.equals(":help"))
                    out.println(message("help.interactive"));
                else if (line.matches("\\s*:.*"))
                    controlResults(app.results, out, line);
                else
                    try {
                        app.runCommand(OptionSet.parseArguments(line.split(" ")));
                    } catch (Exception e) {
                        log.warn(() -> "unexpected error", e);
                        out.println(message("e.0", e.getMessage()));
                    }
            } catch (Exception e) {
                log.warn(() -> "unexpected error", e);
                out.println(message("e.0", e.getMessage()));
            }
        }
        log.info(() -> "exit interactive mode");
    }

    static void controlResults(ResultList results, PrintWriter out, String commandLine) {
        Parameter p = Parameter.parse(commandLine.replaceFirst("^\\s*:", ""));
        try {
            switch (p.at(0)) {
                case "drop":
                    results.drop(getParameterArgAsInt(p, 1, 1));
                    out.println(results.summary());
                    break;
                case "pick":
                    results.pick(getParameterArgAsInt(p, 1, -1));
                    out.println(results.summary());
                    break;
                case "sort":
                    Result r = new Result();
                    results.get(0).pathStream().forEach(r::addPath);
                    results.addFirst(r);
                    out.println(results.summary());
                    break;
                case "label":
                    if (p.has(2)) {
                        int p1 = getParameterArgAsInt(p, 1, 0);
                        String p2 = p.at(2);
                        results.get(p1).setName(p2);
                        out.println(results.summary());
                    }
                    break;
                case "merge":
                    int p1 = getParameterArgAsInt(p, 1, 1);
                    int p2 = getParameterArgAsInt(p, 2, 0);
                    if (p1 != p2) {
                        Result r1 = results.get(p1);
                        Result r2 = results.get(p2);
                        results.add(0, r1.mergeOr(r2));
                        out.println(results.summary());
                    }
                    break;
                case "load":
                    if (p.has(1)) {
                        String name = p.at(1);
                        File f = new File(getEtcDirectory(), name + savefileSuffix);
                        try (FileInputStream fis = new FileInputStream(f)) {
                            ObjectInputStream ois = new ObjectInputStream(fis);
                            ResultList o = (ResultList) ois.readObject();
                            results.clear();
                            results.addAll(o);
                            out.println(results.summary());
                            log.debug(() -> "loaded from " + f.getAbsolutePath());
                        } catch (ClassNotFoundException | EOFException e) {
                            log.warn(() -> "while deserializing", e);
                            throw new IOException(message("e.failedToLoadFile"), e);
                        }
                    }
                    else
                        showSnapshotFiles(out);
                    break;
                case "save":
                    if (p.has(1)) {
                        String name = p.at(1);
                        File dir = getEtcDirectory();
                        if (!dir.exists() && !dir.mkdir())
                            throw new IOException("can't create directory: " + dir.getAbsolutePath());
                        File f = new File(dir, name + savefileSuffix);
                        try (FileOutputStream fos = new FileOutputStream(f)) {
                            ObjectOutputStream oos = new ObjectOutputStream(fos);
                            oos.writeObject(results);
                            log.debug(() -> "saved to " + f.getAbsolutePath());
                        }
                    }
                    else
                        showSnapshotFiles(out);
                    break;
                case "print":
                case "p":
                    out.println(results.summary());
                    break;
                default:
                    out.println("? unknown command");
            }
        } catch (IndexOutOfBoundsException e) {
            log.debug(() -> "runtime error: " + e);
            out.println(message("e.0", message("e.numberIndexOutOfBounds", e.getMessage())));
        } catch (IllegalArgumentException e) {
            log.debug(() -> "runtime error: " + e);
            out.println(message("e.0", message("e.illegalArgument", e.getMessage())));
        } catch (RuntimeException e) {
            log.debug(() -> "runtime error: " + e);
            out.println(message("e.0", e.getMessage()));
        } catch (FileNotFoundException e) {
            log.warn(() -> "File not found", e);
            out.println(message("e.0", message("e.fileCannotOpen", e.getMessage())));
        } catch (IOException e) {
            log.warn(() -> "I/O error", e);
            out.println(message("e.0", e.getMessage()));
        }
    }

    static void showSnapshotFiles(PrintWriter out) throws IOException {
        final File etcDir = getEtcDirectory();
        out.println("directory: " + etcDir.getAbsolutePath());
        if (etcDir.exists()) {
            PathMatcher pm = FileSystems.getDefault().getPathMatcher("glob:*" + savefileSuffix);
            List<Path> a = Files.list(etcDir.toPath()).filter(x -> pm.matches(x.getFileName()))
                    .sorted(PathSorter.createComparator("_mtime")).collect(Collectors.toList());
            if (!a.isEmpty()) {
                out.println("files:");
                final int suffixLen = savefileSuffix.length();
                final int nameWidth = a.stream().mapToInt(x -> FileAttributeFormatter.name(x).length() - suffixLen)
                        .max().orElseGet(() -> 1);
                final long fileSizeWidth =
                    a.stream().mapToLong(x -> String.valueOf(x.toFile().length()).length()).max().orElseGet(() -> 1);
                final String fmt = "%-" + nameWidth + "s (%" + fileSizeWidth + "d bytes, mtime=%s, filename=[%s])"
                                   + TerminalOperation.EOL;
                for (Path path : a) {
                    FileAttributeFormatter faf = new FileAttributeFormatter(path);
                    faf.setFileTimeFormatter(x -> String.format("%1$tF %1$tT", x.toMillis()));
                    String name = faf.name();
                    String shortName = name.substring(0, name.length() - suffixLen);
                    out.printf(fmt, shortName, faf.size(), faf.formattedMtime(), name);
                }
                return;
            }
        }
        out.println(message("i.noSnapshotFiles"));
    }

    static File getEtcDirectory() {
        return new File(getWorkingDirectory(), "etc");
    }

    static File getWorkingDirectory() {
        String homeDir = Optional.ofNullable(System.getProperty("potaufeu.user.home"))
                .orElseGet(() -> System.getProperty("user.home", ""));
        if (homeDir.isEmpty())
            throw new IllegalStateException("can't detect home directory");
        File dir = new File(homeDir, ".potaufeu");
        log.debug(() -> String.format("home directory = [%s]", dir.getAbsolutePath()));
        if (!dir.exists())
            throw new IllegalStateException("working directory does not exist: " + dir.getAbsolutePath());
        return dir;
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
