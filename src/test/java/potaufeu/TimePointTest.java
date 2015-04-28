package potaufeu;

import static org.junit.Assert.assertEquals;
import java.text.*;
import java.util.*;
import org.junit.*;

public class TimePointTest {

    @Test
    public void testMillis() {
        final String now = "20150327123456";
        assertEquals("20150324123456", _millis("3d", now, false));
        assertEquals("20150325123455", _millis("3d", now, true));
        assertEquals("20150326233456", _millis("13h", now, false));
        assertEquals("20150327003455", _millis("13h", now, true));
        assertEquals("20150301050500", _millis("201503010505", now, false));
        assertEquals("20150301050559", _millis("201503010505", now, true));
        assertEquals("20150301120000", _millis("2015030112", now, false));
        assertEquals("20150301125959", _millis("2015030112", now, true));
        assertEquals("20150301000000", _millis("20150301", now, false));
        assertEquals("20150301235959", _millis("20150301", now, true));
        assertEquals("20150101000000", _millis("2015", now, false));
        assertEquals("20151231235959", _millis("2015", now, true));
        assertEquals("20150101000000", _millis("2015", now, false));
        assertEquals("20151231235959", _millis("2015", now, true));
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

}
