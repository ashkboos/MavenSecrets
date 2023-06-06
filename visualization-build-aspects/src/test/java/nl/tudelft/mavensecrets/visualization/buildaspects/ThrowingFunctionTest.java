package nl.tudelft.mavensecrets.visualization.buildaspects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ThrowingFunctionTest {

    @Test
    public void test_id() {
        ThrowingFunction<String, String, Throwable> id = ThrowingFunction.identity();

        Assertions.assertNotNull(id);

        String expected = "Hello, world!";

        String result;
        try {
            result = id.apply(expected);
        } catch (Throwable exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertSame(expected, result);
    }
}
