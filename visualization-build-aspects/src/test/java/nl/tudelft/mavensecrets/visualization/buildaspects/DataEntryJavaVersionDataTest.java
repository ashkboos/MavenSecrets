package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.JavaVersionData;

public class DataEntryJavaVersionDataTest {

    @Test
    public void test_class_map_invalid_1() {
        Map<JavaVersion, Integer> map = new HashMap<>();
        map.put(null, 0);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new JavaVersionData(null, null, null, null, null, map));
    }

    @Test
    public void test_class_map_invalid_2() {
        Map<JavaVersion, Integer> map = new HashMap<>();
        map.put(JavaVersion.JAVA_8, null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new JavaVersionData(null, null, null, null, null, map));
    }

    @Test
    public void test_class_map_invalid_3() {
        Map<JavaVersion, Integer> map = new HashMap<>();
        map.put(JavaVersion.JAVA_8, -1);
        Assertions.assertThrows(IllegalArgumentException.class, () -> new JavaVersionData(null, null, null, null, null, map));
    }

    @Test
    public void test_class_map_null() {
        Map<JavaVersion, Integer> map = null;
        JavaVersionData data = new JavaVersionData(null, null, null, null, null, map);

        Map<JavaVersion, Integer> result = data.versionClassMap();

        AssertionsUtil.assertMapEquals(map, result);
    }

    @Test
    public void test_class_map_unmodifiable() {
        Map<JavaVersion, Integer> map = new HashMap<>();
        JavaVersionData data = new JavaVersionData(null, null, null, null, null, map);

        Map<JavaVersion, Integer> result = data.versionClassMap();

        AssertionsUtil.assertMapEquals(map, result);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> result.put(JavaVersion.JAVA_8, 0));
    }

    @Test
    public void test_class_map_copied() {
        Map<JavaVersion, Integer> map = new HashMap<>();
        JavaVersionData data = new JavaVersionData(null, null, null, null, null, map);

        Map<JavaVersion, Integer> result1 = data.versionClassMap();
        map.put(JavaVersion.JAVA_8, 0);
        Map<JavaVersion, Integer> result2 = data.versionClassMap();

        AssertionsUtil.assertMapNotEquals(map, result1);
        AssertionsUtil.assertMapNotEquals(map, result2);
        AssertionsUtil.assertMapEquals(result1, result2);
    }
}
