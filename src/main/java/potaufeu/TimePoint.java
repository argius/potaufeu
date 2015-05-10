package potaufeu;

import static java.time.temporal.ChronoUnit.*;
import java.time.*;
import java.time.format.*;
import java.time.temporal.*;
import java.util.*;

/**
 * TimePoint indicates a specified instant as `EpochMilli'.
 */
public final class TimePoint {

    private static final String format = "yyyyMMddHHmmssSSS";

    /**
     * Converts date-time expression to milliseconds, as starting point of time range.
     * @param expr date-time expression
     * @return EpochMilli
     */
    public static long millis(String expr) {
        return millis(expr, OptionalLong.empty(), false);
    }

    /**
     * Converts date-time expression to milliseconds, as starting point of time range.
     * @param expr date-time expression
     * @param now now (milliseconds)
     * @return EpochMilli
     */
    public static long millis(String expr, long now) {
        return millis(expr, OptionalLong.of(now), false);
    }

    /**
     * @param expr date-time expression
     * @param end if true, return time as ending point of time range, otherwise returns time as starting point.
     * @return EpochMilli
     */
    public static long millis(String expr, boolean end) {
        return millis(expr, OptionalLong.empty(), end);
    }

    /**
     * @param expr date-time expression
     * @param now now (milliseconds)
     * @param end if true, return time as ending point of time range, otherwise returns time as starting point.
     * @return EpochMilli
     */
    public static long millis(String expr, long now, boolean end) {
        return millis(expr, OptionalLong.of(now), end);
    }

    static long millis(String expr, OptionalLong optionalNow, boolean end) {
        if (expr == null || expr.trim().length() == 0)
            throw new IllegalArgumentException(expr);
        if (expr.matches("\\d{4}|\\d{6}|\\d{8}|\\d{10}|\\d{12}|\\d{14}|\\d{17}"))
            return parseAbsoluteTime(expr, end);
        else if (expr.matches("\\d+[DdHhMmSs]"))
            return parseRelativeTime(expr, optionalNow.orElse(System.currentTimeMillis()), end);
        else
            throw new IllegalArgumentException("bad format: " + expr);
    }

    static long parseRelativeTime(String expr, long millis, boolean end) {
        final int lastIndex = expr.length() - 1;
        final char lastchar = Character.toUpperCase(expr.charAt(lastIndex));
        final int amt = Integer.parseInt(expr.substring(0, lastIndex));
        final ChronoUnit unit;
        switch (lastchar) {
            case 'D':
                unit = DAYS;
                break;
            case 'H':
                unit = HOURS;
                break;
            case 'M':
                unit = MINUTES;
                break;
            case 'S':
            default: // unexpected
                unit = SECONDS;
        }
        ZoneId zoneId = ZoneId.systemDefault();
        LocalDateTime local = LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), zoneId);
        LocalDateTime result = (end) ? local.minus(amt - 1, unit).minusNanos(1_000L) : local.minus(amt, unit);
        return result.atZone(zoneId).toInstant().toEpochMilli();
    }

    static long parseAbsoluteTime(String s, boolean end) {
        final ChronoUnit unit = length2ChronoUnit(s.length());
        LocalDateTime local = parseAbsoluteTime0(s, unit);
        LocalDateTime result = (end) ? local.plus(1, unit).minusNanos(1_000L) : local;
        return result.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }

    private static LocalDateTime parseAbsoluteTime0(String s, ChronoUnit unit) {
        if (unit == MILLIS) {
            // irregular
            final int i = 8;
            LocalDate date = LocalDate.parse(s.substring(0, i), DateTimeFormatter.BASIC_ISO_DATE);
            LocalTime time = LocalTime.parse(s.substring(i), DateTimeFormatter.ofPattern(format.substring(i)));
            return date.atTime(time);
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format.substring(0, s.length()));
        switch (unit) {
            case YEARS:
                return Year.parse(s, formatter).atMonth(1).atDay(1).atStartOfDay();
            case MONTHS:
                return YearMonth.parse(s, formatter).atDay(1).atStartOfDay();
            case DAYS:
                return LocalDate.parse(s, formatter).atStartOfDay();
            default:
                return LocalDateTime.parse(s, formatter);
        }
    }

    private static ChronoUnit length2ChronoUnit(int length) {
        switch (length) {
            case 4:
                return YEARS;
            case 6:
                return MONTHS;
            case 8:
                return DAYS;
            case 10:
                return HOURS;
            case 12:
                return MINUTES;
            case 14:
                return SECONDS;
            default:
                return MILLIS;
        }
    }

}
