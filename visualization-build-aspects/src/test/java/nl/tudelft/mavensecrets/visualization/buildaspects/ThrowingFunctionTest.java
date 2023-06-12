package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.util.function.Function;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ThrowingFunctionTest {

    @Test
    public void test_compose_function() {
        ThrowingFunction<String, String, Throwable> tf1 = x -> x + "c";
        Function<String, String> tf2 = x -> x + "b";

        ThrowingFunction<String, String, Throwable> merged = tf1.compose(tf2);

        Assertions.assertNotNull(merged);

        String result;
        try {
            result = merged.apply("a");
        } catch (Throwable exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertEquals("abc", result);
    }

    @Test
    public void test_compose_throwing_function() {
        ThrowingFunction<String, String, Throwable> tf1 = x -> x + "c";
        ThrowingFunction<String, String, Throwable> tf2 = x -> x + "b";

        ThrowingFunction<String, String, Throwable> merged = tf1.compose(tf2);

        Assertions.assertNotNull(merged);

        String result;
        try {
            result = merged.apply("a");
        } catch (Throwable exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertEquals("abc", result);
    }

    @Test
    public void test_and_then_function() {
        ThrowingFunction<String, String, Throwable> tf1 = x -> x + "b";
        Function<String, String> tf2 = x -> x + "c";

        ThrowingFunction<String, String, Throwable> merged = tf1.andThen(tf2);

        Assertions.assertNotNull(merged);

        String result;
        try {
            result = merged.apply("a");
        } catch (Throwable exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertEquals("abc", result);
    }

    @Test
    public void test_and_then_throwing_function() {
        ThrowingFunction<String, String, Throwable> tf1 = x -> x + "b";
        ThrowingFunction<String, String, Throwable> tf2 = x -> x + "c";

        ThrowingFunction<String, String, Throwable> merged = tf1.andThen(tf2);

        Assertions.assertNotNull(merged);

        String result;
        try {
            result = merged.apply("a");
        } catch (Throwable exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertEquals("abc", result);
    }

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
