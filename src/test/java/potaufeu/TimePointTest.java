package potaufeu;

import static org.junit.Assert.*;
import static potaufeu.TimePoint.millis;
import java.text.*;
import java.util.*;
import org.junit.Test;
import junit.framework.*;

public class TimePointTest {

    @Test
    public void testExceptions() {
        assertEquals("java.lang.IllegalArgumentException: ", getExceptionAsString(() -> millis("")));
        assertEquals("java.lang.IllegalArgumentException", getExceptionAsString(() -> millis(null)));
        for (String s : new String[] { "1x", "20150" })
            assertEquals("java.lang.IllegalArgumentException: bad format: " + s, getExceptionAsString(() -> millis(s)));
    }

    @Test
    public void testMillis() {
        assertNotNull(new TimePoint());
        assertEquals("20150301050500", _millis("201503010505"));
        assertEquals("20150301050500", _millis("201503010505", 0L));
        assertEquals("20150301050559", _millis("201503010505", true));
        assertEquals("20150301050637", _millis("20150301050637987"));
    }

    @Test
    public void testMillis_2() {
        final String now = "20150327123456";
        assertEquals("20150324123456", _millis("3d", now, false));
        assertEquals("20150325123455", _millis("3d", now, true));
        assertEquals("20150326233456", _millis("13h", now, false));
        assertEquals("20150327003455", _millis("13h", now, true));
        assertEquals("20150327120756", _millis("27m", now, false));
        assertEquals("20150327120855", _millis("27m", now, true));
        assertEquals("20150327123417", _millis("39s", now, false));
        assertEquals("20150327123417", _millis("39s", now, true));
        assertEquals("20150101000000", _millis("2015", now, false));
        assertEquals("20151231235959", _millis("2015", now, true));
        assertEquals("20150301000000", _millis("201503", now, false));
        assertEquals("20150331235959", _millis("201503", now, true));
        assertEquals("20150301000000", _millis("20150301", now, false));
        assertEquals("20150301235959", _millis("20150301", now, true));
        assertEquals("20150301120000", _millis("2015030112", now, false));
        assertEquals("20150301125959", _millis("2015030112", now, true));
        assertEquals("20150301050500", _millis("201503010505", now, false));
        assertEquals("20150301050559", _millis("201503010505", now, true));
        assertEquals("20150301050637", _millis("20150301050637", now, false));
        assertEquals("20150301050637", _millis("20150301050637", now, true));
    }

    static String _millis(String exp) {
        return stringFrom(TimePoint.millis(exp));
    }

    static String _millis(String exp, long millis) {
        return stringFrom(TimePoint.millis(exp, millis));
    }

    static String _millis(String exp, boolean end) {
        return stringFrom(TimePoint.millis(exp, end));
    }

    static String _millis(String exp, String timeExpr, boolean end) {
        return stringFrom(TimePoint.millis(exp, millisOf(timeExpr), end));
    }

    static long millisOf(String expr) {
        try {
            DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
            return df.parse(expr).getTime();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    static String stringFrom(long millis) {
        DateFormat df = new SimpleDateFormat("yyyyMMddHHmmss");
        return df.format(new Date(millis));
    }

    static String getExceptionAsString(Runnable action) {
        try {
            action.run();
            throw new AssertionFailedError();
        } catch (Exception e) {
            return e.toString();
        }
    }

}
