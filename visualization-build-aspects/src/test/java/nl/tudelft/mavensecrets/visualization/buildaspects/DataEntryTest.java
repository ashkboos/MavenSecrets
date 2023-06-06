package nl.tudelft.mavensecrets.visualization.buildaspects;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.CompilerConfigData;
import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.Id;
import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.JavaModuleData;
import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.JavaVersionData;

public class DataEntryTest {

    private static Id id = null;
    private static JavaModuleData moduleData = null;
    private static JavaVersionData versionData = null;
    private static CompilerConfigData compilerData = null;

    @Test
    public void test_id_null() {
        Assertions.assertThrows(NullPointerException.class, () -> new DataEntry(null, false, 0, moduleData, versionData, compilerData));
    }

    @Test
    public void test_module_data_null() {
        Assertions.assertThrows(NullPointerException.class, () -> new DataEntry(id, false, 0, null, versionData, compilerData));
    }

    @Test
    public void test_version_data_null() {
        Assertions.assertThrows(NullPointerException.class, () -> new DataEntry(id, false, 0, moduleData, null, compilerData));
    }

    @Test
    public void test_compiler_data_null() {
        Assertions.assertThrows(NullPointerException.class, () -> new DataEntry(id, false, 0, moduleData, versionData, null));
    }

    @Test
    public void test_year_invalid() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> new DataEntry(id, false, -1, moduleData, versionData, compilerData));
    }

    @BeforeAll
    public static void setup() {
        id = new Id("a", "b", "c");
        moduleData = new JavaModuleData(false);
        versionData = new JavaVersionData(null, null, null, null, null, null);
        compilerData = new CompilerConfigData(false, null, null, null, null, null, null);
    }

    @AfterAll
    public static void teardown() {
        id = null;
        moduleData = null;
        versionData = null;
        compilerData = null;
    }
}
