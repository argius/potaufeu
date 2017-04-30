package potaufeu;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import junit.framework.*;

public final class ResultListTest {

    @Test
    public void testResultList() {
        ResultList a = new ResultList(namedResult("first"));
        assertEquals(1, a.size());
        assertEquals("first", a.getFirst().getName());
    }

    @Test
    public void testPick() {
        ResultList a = new ResultList();
        a.add(namedResult("first"));
        a.add(namedResult("second"));
        a.pick(1);
        assertEquals(3, a.size());
        assertEquals("Results: [#0:second(0), #1:first(0), #2:second(0)]", a.summary());
        a.pick(-2);
        assertEquals(4, a.size());
        assertEquals("Results: [#0:first(0), #1:second(0), #2:first(0), #3:second(0)]", a.summary());
    }

    @Test
    public void testShift() {
        ResultList a = new ResultList();
        a.shift(1);
        a.add(namedResult("first"));
        a.add(namedResult("second"));
        a.shift(1);
        assertEquals(1, a.size());
        assertEquals("Results: [#0:first(0)]", a.summary());
        a.add(namedResult("second"));
        a.shift(3);
        assertEquals(0, a.size());
        assertEquals("Results: []", a.summary());
    }

    @Test
    public void testDrop() {
        ResultList a = new ResultList();
        a.add(namedResult("first"));
        a.add(namedResult("second"));
        assertEquals("java.lang.IllegalArgumentException: drop requires 1+", getExceptionAsString(() -> a.drop(0)));
        a.drop(1);
        assertEquals(1, a.size());
        assertEquals("Results: [#0:second(0)]", a.summary());
        a.drop(2);
        assertEquals(0, a.size());
        assertEquals("Results: []", a.summary());
    }

    @Test
    public void testSummary() {
        ResultList a = new ResultList();
        a.add(namedResult(""));
        assertEquals("Results: [#0(0)]", a.summary());
    }

    static Result namedResult(String name) {
        Result o = new Result();
        o.setName(name);
        return o;
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
