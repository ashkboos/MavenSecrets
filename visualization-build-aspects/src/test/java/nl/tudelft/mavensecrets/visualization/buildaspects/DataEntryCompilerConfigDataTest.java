package nl.tudelft.mavensecrets.visualization.buildaspects;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.CompilerConfigData;

public class DataEntryCompilerConfigDataTest {

    @Test
    public void test_arg_null() {
        Assertions.assertThrows(NullPointerException.class, () -> new CompilerConfigData(true, null, new String[1], null, null, null, null));
    }
}
