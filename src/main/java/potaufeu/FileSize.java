package potaufeu;

/**
 * This class indicates file size.
 */
public final class FileSize {

    private final String expr;

    /**
     * A constructor.
     * @param expr file size expression
     */
    public FileSize(String expr) {
        toByteSize(expr); // for validation
        this.expr = expr;
    }

    /**
     * Converts sizes in each units.
     * @return the number of bytes
     */
    public long toByteSize() {
        return toByteSize(expr);
    }

    /**
     * Converts sizes in each units.
     * @param expr file size expression
     * @return the number of bytes
     */
    public static long toByteSize(String expr) {
        if (expr.matches("\\d+"))
            return Long.parseLong(expr);
        if (expr.matches("(\\d+|\\d+\\.\\d+)\\w"))
            return toByteSize(expr, expr.length() - 1);
        if (expr.matches("(\\d+|\\d+\\.\\d+)\\w[Bb]"))
            return toByteSize(expr, expr.length() - 2);
        throw new IllegalArgumentException("invalid filesize expression: " + expr);
    }

    private static long toByteSize(String expr, int indexOfUnit) {
        final double decimal = Double.parseDouble(expr.substring(0, indexOfUnit));
        final int index = "KMGTPEZY".indexOf(Character.toUpperCase(expr.charAt(indexOfUnit)));
        if (index < 0)
            throw new IllegalArgumentException("invalid filesize expression: " + expr);
        final int scale = index + 1;
        return (long) (decimal * Math.pow(1_024L, scale));
    }

    @Override
    public String toString() {
        return "FileSize(" + expr + ")";
    }

}
