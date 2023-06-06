package nl.tudelft.mavensecrets.visualization.buildaspects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.Id;

public class DataEntryIdTest {

    @Test
    public void test_id_null() {
        Assertions.assertThrows(NullPointerException.class, () -> new Id(null, "b", "c"));
        Assertions.assertThrows(NullPointerException.class, () -> new Id("a", null, "c"));
        Assertions.assertThrows(NullPointerException.class, () -> new Id("a", "b", null));
    }

    @Test
    public void test_id_empty() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Id("", "b", "c"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Id("a", "", "c"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Id("a", "b", ""));
    }

    @Test
    public void test_id_invalid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Id(":", "b", "c"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Id(" ", "b", "c"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Id("a", ":", "c"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Id("a", " ", "c"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Id("a", "b", ":"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> new Id("a", "b", " "));
    }

    @Test
    public void test_tostring() {
        Id id = new Id("a", "b", "c");

        String string = id.toString();

        Assertions.assertNotNull(string);
        Assertions.assertEquals("a:b:c", string);
    }
}
