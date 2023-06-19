package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class UtilTest {

    @Test
    public void test_map_valid_null() {
        Map<JavaVersion, Integer> map = null;

        Assertions.assertFalse(Util.isValidVersionMap(map));
    }

    @Test
    public void test_map_valid_key_null() {
        Map<JavaVersion, Integer> map = new HashMap<>();
        map.put(null, 0);

        Assertions.assertFalse(Util.isValidVersionMap(map));
    }

    @Test
    public void test_map_valid_value_null() {
        Map<JavaVersion, Integer> map = new HashMap<>();
        map.put(JavaVersion.JAVA_8, null);

        Assertions.assertFalse(Util.isValidVersionMap(map));
    }

    @Test
    public void test_map_valid_value_negative() {
        Map<JavaVersion, Integer> map = new HashMap<>();
        map.put(JavaVersion.JAVA_8, -1);

        Assertions.assertFalse(Util.isValidVersionMap(map));
    }

    @Test
    public void test_map_valid() {
        Map<JavaVersion, Integer> map = new HashMap<>();
        map.put(JavaVersion.JAVA_8, 0);

        Assertions.assertTrue(Util.isValidVersionMap(map));
    }

    @Test
    public void test_merge_major_versions_null() {
        Assertions.assertThrows(NullPointerException.class, () -> Util.mergeMajorVersions(null));
    }

    @Test
    public void test_nulls_last_null() {
        Assertions.assertThrows(NullPointerException.class, () -> Util.nullsLast(null));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_nulls_last() {
        Comparator<? super String> nullUnsafe = Mockito.mock(Comparator.class);
        Mockito.when(nullUnsafe.compare(Mockito.anyString(), Mockito.anyString()))
                .thenAnswer(new Answer<>() {

            @NotNull
            @Override
            public Integer answer(@NotNull InvocationOnMock invocation) throws Throwable {
                // Preconditions
                Objects.requireNonNull(invocation);

                String a = invocation.getArgument(0, String.class);
                String b = invocation.getArgument(1, String.class);

                if (a == null || b == null) {
                    throw new NullPointerException();
                }

                return String.CASE_INSENSITIVE_ORDER.compare(a, b);
            }});

        Comparator<? super String> comparator = Util.nullsLast(nullUnsafe);

        Assertions.assertNotNull(comparator);

        String string1 = "a";
        String string2 = "b";
        int result;

        result = comparator.compare(null, null);

        Assertions.assertEquals(0, result);
        Mockito.verify(nullUnsafe, Mockito.times(0))
                .compare(Mockito.anyString(), Mockito.anyString());

        Mockito.clearInvocations(nullUnsafe);

        result = comparator.compare(null, string1);

        AssertionsUtil.assertGreaterThan(result, 0);
        Mockito.verify(nullUnsafe, Mockito.times(0))
                .compare(Mockito.anyString(), Mockito.anyString());

        Mockito.clearInvocations(nullUnsafe);

        result = comparator.compare(string1, null);

        AssertionsUtil.assertLessThan(result, 0);
        Mockito.verify(nullUnsafe, Mockito.times(0))
                .compare(Mockito.anyString(), Mockito.anyString());

        Mockito.clearInvocations(nullUnsafe);

        result = comparator.compare(string1, string2);

        Assertions.assertEquals(String.CASE_INSENSITIVE_ORDER.compare(string1, string2), result);
        Mockito.verify(nullUnsafe, Mockito.times(1))
                .compare(Mockito.anyString(), Mockito.anyString());
    }

    @Test
    public void test_null_save_group_by_and_count() {
        String string = "Hello, world!";
        Map<String, Integer> result = Util.nullSafeGroupByAndCount(Arrays.stream(new String[] {string, null, string, null}), Function.identity());

        Map<String, Integer> expected = new HashMap<>();
        expected.put(string, 2);
        expected.put(null, 2);

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void test_null_save_group_by_and_count_stream_null() {
        Assertions.assertThrows(NullPointerException.class, () -> Util.nullSafeGroupByAndCount(null, Function.identity()));
    }

    @Test
    public void test_null_save_group_by_and_count_classifier_null() {
        Assertions.assertThrows(NullPointerException.class, () -> Util.nullSafeGroupByAndCount(Arrays.stream(new Object[0]), null));
    }
}
