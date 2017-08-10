package potaufeu;

import static org.junit.Assert.*;
import static potaufeu.StringMatchingPredicate.*;
import java.util.*;
import org.junit.*;

public final class StringMatchingPredicateTest {

    @Test
    public void testMatches() {
        // ignore
    }

    @Test
    public void testAnd() {
        StringMatchingPredicate f1 = x -> x.startsWith("ABC");
        StringMatchingPredicate f2 = x -> x.endsWith("DEF");
        StringMatchingPredicate f = and(f1, f2);
        assertTrue(f.matches("ABCDEF"));
        assertTrue(f.matches("ABCXDEF"));
        assertTrue(f.matches("ABC-DEF"));
        assertFalse(f.matches("AABC-DEF"));
        assertFalse(f.matches("ABC-DEFF"));
    }

    @Test
    public void testOr() {
        StringMatchingPredicate f1 = x -> x.startsWith("ABC");
        StringMatchingPredicate f2 = x -> x.endsWith("DEF");
        StringMatchingPredicate f = or(f1, f2);
        assertTrue(f.matches("ABCDEF"));
        assertTrue(f.matches("ABCXDEFZ"));
        assertTrue(f.matches("PABC-DEF"));
        assertFalse(f.matches("AABC-DEFF"));
    }

    @Test
    public void testCreateString() {
        StringMatchingPredicate f1 = create("aabb");
        StringMatchingPredicate f2 = create("(?)POT");
        StringMatchingPredicate f3 = create("(?i)POT");
        assertTrue(f1.matches("ooaabbzz"));
        assertFalse(f1.matches("aab"));
        assertTrue(f2.matches("xPOTx"));
        assertFalse(f2.matches("xPoTx"));
        assertTrue(f3.matches("xPoTx"));
        assertFalse(f3.matches("xPxTx"));
    }

    @Test
    public void testCreateStringArray() {
        StringMatchingPredicate f = create("aabb", "XXX");
        assertTrue(f.matches("aabb000XXX"));
        assertFalse(f.matches("aab000XXX"));
        assertFalse(f.matches("aabb000XX"));
    }

    @Test
    public void testCreateListOfString() {
        StringMatchingPredicate f0 = create(Arrays.asList());
        StringMatchingPredicate f1 = create(Arrays.asList("(?)P[eu]t"));
        StringMatchingPredicate f2 = create(Arrays.asList("(?)mor", "(?i)a.m"));
        assertTrue(f0.matches("qaWsedRftgYhujiKolP"));
        assertTrue(f1.matches("Pet"));
        assertTrue(f1.matches("Put"));
        assertFalse(f1.matches("Pot"));
        assertTrue(f2.matches("Armor"));
        assertTrue(f2.matches("ARmor"));
        assertFalse(f2.matches("ARMor"));
    }

}
