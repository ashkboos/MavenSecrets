package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;

import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

public class AssertionsUtil {

    private AssertionsUtil() {
        // Nothing
    }

    public static void assertEmpty(@Nullable Optional<?> optional) {
        Assertions.assertNotNull(optional);
        Assertions.assertTrue(optional.isEmpty());
    }

    public static void assertPresent(@Nullable Optional<?> optional) {
        Assertions.assertNotNull(optional);
        Assertions.assertTrue(optional.isPresent());
    }

    public static void assertGreaterThan(int x, int y) {
        if (x <= y) {
            Assertions.fail("Expected: a value greater than <" + y + ">\nBut <" + x + "> was " + (x == y ? "equal to" : "less than") + " <" + y + ">");
        }
    }

    public static void assertGreaterThanOrEqualTo(int x, int y) {
        if (x < y) {
            Assertions.fail("Expected: a value greater than or equal to <" + y + ">\nBut <" + x + "> was less than <" + y + ">");
        }
    }

    public static void assertLessThan(int x, int y) {
        if (x >= y) {
            Assertions.fail("Expected: a value less than <" + y + ">\nBut <" + x + "> was " + (x == y ? "equal to" : "greater than") + " <" + y + ">");
        }
    }

    public static void assertLessThanOrEqualTo(int x, int y) {
        if (x > y) {
            Assertions.fail("Expected: a value less than or equal to <" + y + ">\nBut <" + x + "> was greater than <" + y + ">");
        }
    }

    public static <K, V> void assertMapEquals(@Nullable Map<? extends K, ? extends V> expected, @Nullable Map<? extends K, ? extends V> actual) {
        if (expected == null) {
            Assertions.assertNull(actual);
            return;
        }

        Assertions.assertNotNull(actual);
        Assertions.assertEquals(expected.size(), actual.size());

        for (Entry<? extends K, ? extends V> entry : expected.entrySet()) {
            K key = entry.getKey();
            V valueExpected = entry.getValue();
            V valueActual = actual.get(key);
            if (valueExpected == null) {
                Assertions.assertNull(valueActual);
                Assertions.assertTrue(actual.containsKey(key));
            } else {
                Assertions.assertEquals(valueExpected, valueActual);
            }
        }
    }

    public static <K, V> void assertMapNotEquals(@Nullable Map<? extends K, ? extends V> expected, @Nullable Map<? extends K, ? extends V> actual) {
        if (expected == null) {
            if (actual == null) {
                Assertions.fail("Expected: a map unequal to <" + expected + ">\nBut <" + actual + "> was equal to <" + expected + ">");
            }
            return;
        }

        if (actual == null) {
            return;
        }

        if (expected.size() != actual.size()) {
            return;
        }

        boolean mismatch = false;
        for (Entry<? extends K, ? extends V> entry : expected.entrySet()) {
            K key = entry.getKey();

            if (actual.containsKey(key)) {
                V valueExpected = entry.getValue();
                V valueActual = actual.get(key);

                if (!Objects.equals(valueExpected, valueActual)) {
                    mismatch = true;
                    break;
                }
            } else {
                mismatch = true;
                break;
            }
        }

        if (!mismatch) {
            Assertions.fail("Expected: a map unequal to <" + expected + ">\nBut <" + actual + "> was equal to <" + expected + ">");
        }
    }
}
