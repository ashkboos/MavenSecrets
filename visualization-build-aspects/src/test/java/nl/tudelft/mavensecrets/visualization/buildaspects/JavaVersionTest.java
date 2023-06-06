package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

public class JavaVersionTest {

    @Test
    public void test_minor_absent() {
        JavaVersion version = JavaVersion.JAVA_8;

        Assertions.assertFalse(version.hasMinorVersion());
        Assertions.assertThrows(IllegalStateException.class, version::getMinorVersion);
    }

    @Test
    public void test_major_construction() {
        JavaVersion parent = JavaVersion.JAVA_8;

        JavaVersion version = parent.withMinorVersion((short) 1);

        JavaVersion major = version.withoutMinorVersion();

        Assertions.assertNotNull(major);
        Assertions.assertEquals(parent, major);
    }

    @Test
    public void test_minor_construction() {
        JavaVersion parent = JavaVersion.JAVA_8;

        JavaVersion version = parent.withMinorVersion((short) 1);

        Assertions.assertNotNull(version);
        Assertions.assertEquals(parent.getName(), version.getName());
        Assertions.assertEquals(parent.getMajorVersion(), version.getMajorVersion());
        Assertions.assertTrue(version.hasMinorVersion());
        Assertions.assertEquals((short) 1, version.getMinorVersion());
        Assertions.assertEquals(parent.isLongTermSupportVersion(), version.isLongTermSupportVersion());
    }

    @Test
    public void test_compare_null() {
        Assertions.assertThrows(NullPointerException.class, () -> JavaVersion.JAVA_8.compareTo(null));
    }

    @Test
    public void test_compare_self() {
        JavaVersion version = JavaVersion.JAVA_8;

        Assertions.assertEquals(0, version.compareTo(version));
    }

    @Test
    public void test_compare_majors() {
        JavaVersion version1 = JavaVersion.JAVA_8;
        JavaVersion version2 = JavaVersion.JAVA_9;

        int result1 = version1.compareTo(version2);
        int result2 = version2.compareTo(version1);
        AssertionsUtil.assertLessThan(result1, 0);
        AssertionsUtil.assertGreaterThan(result2, 0);
    }

    @Test
    public void test_compare_minors() {
        JavaVersion version1 = JavaVersion.JAVA_8.withMinorVersion((short) 1);
        JavaVersion version2 = JavaVersion.JAVA_8.withMinorVersion((short) 2);

        int result1 = version1.compareTo(version2);
        int result2 = version2.compareTo(version1);
        AssertionsUtil.assertLessThan(result1, 0);
        AssertionsUtil.assertGreaterThan(result2, 0);
    }

    @Test
    public void test_compare_major_minor_1() {
        JavaVersion version1 = JavaVersion.JAVA_8;
        JavaVersion version2 = JavaVersion.JAVA_8.withMinorVersion((short) 1);

        int result1 = version1.compareTo(version2);
        int result2 = version2.compareTo(version1);
        AssertionsUtil.assertLessThan(result1, 0);
        AssertionsUtil.assertGreaterThan(result2, 0);
    }

    @Test
    public void test_compare_major_minor_2() {
        JavaVersion version1 = JavaVersion.JAVA_8.withMinorVersion((short) 1);
        JavaVersion version2 = JavaVersion.JAVA_9;

        int result1 = version1.compareTo(version2);
        int result2 = version2.compareTo(version1);
        AssertionsUtil.assertLessThan(result1, 0);
        AssertionsUtil.assertGreaterThan(result2, 0);
    }

    @Test
    public void test_hashcode() {
        JavaVersion version1 = JavaVersion.JAVA_8;
        JavaVersion version2 = JavaVersion.JAVA_8.withMinorVersion((short) 1);
        JavaVersion version3 = JavaVersion.JAVA_9;

        int hash1 = version1.hashCode();
        int hash2 = version2.hashCode();
        int hash3 = version3.hashCode();

        Assertions.assertNotEquals(0, hash1);
        Assertions.assertNotEquals(0, hash2);
        Assertions.assertNotEquals(0, hash3);

        // Deliberately not #assertNotEqual(...) because we are not dealing with an 'expected' value
        Assertions.assertTrue(hash1 != hash2);
        Assertions.assertTrue(hash1 != hash3);
        Assertions.assertTrue(hash2 != hash3);
    }

    @Test
    public void test_equals_null() {
        JavaVersion version = JavaVersion.JAVA_8;

        Assertions.assertFalse(version.equals(null));
    }

    @Test
    public void test_equals_typediff() {
        JavaVersion version = JavaVersion.JAVA_8;

        Assertions.assertFalse(version.equals(new Object()));
    }

    @Test
    public void test_equals_self() {
        JavaVersion version = JavaVersion.JAVA_8;

        Assertions.assertTrue(version.equals(version));
    }

    @Test
    public void test_equals_other() {
        JavaVersion version1 = JavaVersion.JAVA_8;
        JavaVersion version2 = JavaVersion.JAVA_8.withMinorVersion((short) 1);
        JavaVersion version3 = JavaVersion.JAVA_9;

        Assertions.assertFalse(version1.equals(version2));
        Assertions.assertFalse(version2.equals(version1));

        Assertions.assertFalse(version1.equals(version3));
        Assertions.assertFalse(version3.equals(version1));

        Assertions.assertFalse(version2.equals(version3));
        Assertions.assertFalse(version3.equals(version2));
    }

    @Test
    public void test_tostring_major() {
        JavaVersion version = JavaVersion.JAVA_8;

        String string = version.toString();

        Assertions.assertNotNull(string);
        Assertions.assertEquals("Java SE 8", string);
    }

    @Test
    public void test_tostring_minor() {
        JavaVersion version = JavaVersion.JAVA_8.withMinorVersion((short) 1);

        String string = version.toString();

        Assertions.assertNotNull(string);
        Assertions.assertEquals("Java SE 8 (minor: 1)", string);
    }

    @Test
    public void test_from_bytes_null() {
        Assertions.assertThrows(NullPointerException.class, () -> JavaVersion.fromClassVersion(null, new byte[2]));
        Assertions.assertThrows(NullPointerException.class, () -> JavaVersion.fromClassVersion(new byte[2], null));
    }

    @Test
    public void test_from_bytes_array_size() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> JavaVersion.fromClassVersion(new byte[1], new byte[2]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JavaVersion.fromClassVersion(new byte[3], new byte[2]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JavaVersion.fromClassVersion(new byte[2], new byte[1]));
        Assertions.assertThrows(IllegalArgumentException.class, () -> JavaVersion.fromClassVersion(new byte[2], new byte[3]));
    }

    @Test
    public void test_from_bytes_major_invalid() {
        // Assuming class version 0x0000 is not valid
        Optional<? extends JavaVersion> optional = JavaVersion.fromClassVersion(new byte[2], new byte[2]);

        Assertions.assertNotNull(optional);
        Assertions.assertTrue(optional.isEmpty());
    }

    @Test
    public void test_from_bytes_major() {
        Optional<? extends JavaVersion> optional = JavaVersion.fromClassVersion(new byte[] {0x00, 0x34}, new byte[] {0x00, 0x01});

        Assertions.assertNotNull(optional);
        Assertions.assertTrue(optional.isPresent());

        JavaVersion version = optional.get();
        JavaVersion expected = JavaVersion.JAVA_8.withMinorVersion((short) 1);

        Assertions.assertEquals(expected, version);
    }

    @Disabled
    @Test
    public void test_from_string() {
        JavaVersion.fromString("");
    }

    @Disabled
    @Test
    public void test_from_string_minor() {
        JavaVersion.fromString("", true);
    }
}
