package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.util.function.Consumer;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ThrowingConsumerTest {

    @Test
    public void test_and_then_consumer() {
        ThrowingConsumer<boolean[], Throwable> tc1 = array -> array[0] = true;
        Consumer<boolean[]> tc2 = array -> array[1] = true;

        ThrowingConsumer<boolean[], Throwable> merged = tc1.andThen(tc2);

        Assertions.assertNotNull(merged);

        boolean[] data = new boolean[2];

        try {
            merged.accept(data);
        } catch (Throwable exception) {
            Assertions.fail(exception);
        }

        Assertions.assertTrue(data[0], "Consumer 1 did not run");
        Assertions.assertTrue(data[1], "Consumer 2 did not run");
    }

    @Test
    public void test_and_then_throwing_consumer() {
        ThrowingConsumer<boolean[], Throwable> tc1 = array -> array[0] = true;
        ThrowingConsumer<boolean[], Throwable> tc2 = array -> array[1] = true;

        ThrowingConsumer<boolean[], Throwable> merged = tc1.andThen(tc2);

        Assertions.assertNotNull(merged);

        boolean[] data = new boolean[2];

        try {
            merged.accept(data);
        } catch (Throwable exception) {
            Assertions.fail(exception);
        }

        Assertions.assertTrue(data[0], "Consumer 1 did not run");
        Assertions.assertTrue(data[1], "Consumer 2 did not run");
    }
}
