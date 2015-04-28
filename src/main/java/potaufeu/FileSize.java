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
        if (expr.matches("(\\d+|\\d+\\.\\d+)\\w\\w"))
            return toByteSize(expr, expr.length() - 2);
        throw new IllegalArgumentException("invalid filesize expression: " + expr);
    }

    private static long toByteSize(String expr, int indexOfUnit) {
        final double decimal = Double.parseDouble(expr.substring(0, indexOfUnit));
        final int scale = getScale(expr.substring(indexOfUnit));
        return (long)(decimal * Math.pow(1_024L, scale));
    }

    private static int getScale(String unit) {
        switch (unit.toUpperCase()) {
            case "K":
            case "KB":
                return 1;
            case "M":
            case "MB":
                return 2;
            case "G":
            case "GB":
                return 3;
            case "T":
            case "TB":
                return 4;
            case "P":
            case "PB":
                return 5;
            case "E":
            case "EB":
                return 6;
            case "Z":
            case "ZB":
                return 7;
            case "Y":
            case "YB":
                return 8;
            default:
        }
        throw new IllegalArgumentException("invalid unit: " + unit);
    }

}
