package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.util.Optional;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JavaVersionTest {

    @SuppressWarnings("deprecation")
    @Test
    public void test_lts() {
        Assertions.assertFalse(JavaVersion.JAVA_0.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_0.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_1.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_1.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_2.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_2.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_3.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_3.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_4.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_4.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_5.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_5.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_6.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_6.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_7.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_7.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertTrue(JavaVersion.JAVA_8.isLongTermSupportVersion());
        Assertions.assertTrue(JavaVersion.JAVA_8.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_9.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_9.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_10.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_10.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertTrue(JavaVersion.JAVA_11.isLongTermSupportVersion());
        Assertions.assertTrue(JavaVersion.JAVA_11.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_12.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_12.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_13.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_13.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_14.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_14.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_15.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_15.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_16.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_16.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertTrue(JavaVersion.JAVA_17.isLongTermSupportVersion());
        Assertions.assertTrue(JavaVersion.JAVA_17.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_18.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_18.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_19.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_19.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_20.isLongTermSupportVersion());
        Assertions.assertFalse(JavaVersion.JAVA_20.withMinorVersion((short) 1).isLongTermSupportVersion());
        Assertions.assertTrue(JavaVersion.JAVA_21.isLongTermSupportVersion());
        Assertions.assertTrue(JavaVersion.JAVA_21.withMinorVersion((short) 1).isLongTermSupportVersion());
    }

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

        AssertionsUtil.assertEmpty(optional);
    }

    @Test
    public void test_from_bytes_major() {
        Optional<? extends JavaVersion> optional = JavaVersion.fromClassVersion(new byte[] {0x00, 0x34}, new byte[] {0x00, 0x01});

        AssertionsUtil.assertPresent(optional);

        JavaVersion version = optional.get();
        JavaVersion expected = JavaVersion.JAVA_8.withMinorVersion((short) 1);

        Assertions.assertEquals(expected, version);
    }

    @Test
    public void test_from_string_malformed() {
        Optional<? extends JavaVersion> optional1 = JavaVersion.fromString(" 0.0.0_0 (Oracle)");
        Optional<? extends JavaVersion> optional2 = JavaVersion.fromString("0.0.0_0 (Oracle) ");
        Optional<? extends JavaVersion> optional3 = JavaVersion.fromString("x.0.0_0 (Oracle)");
        Optional<? extends JavaVersion> optional4 = JavaVersion.fromString("0.x.0_0 (Oracle)");
        Optional<? extends JavaVersion> optional5 = JavaVersion.fromString("0.0.x_0 (Oracle)");
        Optional<? extends JavaVersion> optional6 = JavaVersion.fromString("0.0.0_x (Oracle)");

        AssertionsUtil.assertEmpty(optional1);
        AssertionsUtil.assertEmpty(optional2);
        AssertionsUtil.assertEmpty(optional3);
        AssertionsUtil.assertEmpty(optional4);
        AssertionsUtil.assertEmpty(optional5);
        AssertionsUtil.assertEmpty(optional6);
    }
    
    @Test
    public void test_from_string() {
        Optional<? extends JavaVersion> optional1 = JavaVersion.fromString("8");
        Optional<? extends JavaVersion> optional2 = JavaVersion.fromString("8.0");
        Optional<? extends JavaVersion> optional3 = JavaVersion.fromString("8.0.0");
        Optional<? extends JavaVersion> optional4 = JavaVersion.fromString("8_1");
        Optional<? extends JavaVersion> optional5 = JavaVersion.fromString("8.0_1");
        Optional<? extends JavaVersion> optional6 = JavaVersion.fromString("8.0.0_1");
        Optional<? extends JavaVersion> optional7 = JavaVersion.fromString("8 (Oracle)");
        Optional<? extends JavaVersion> optional8 = JavaVersion.fromString("8.0 (Oracle)");
        Optional<? extends JavaVersion> optional9 = JavaVersion.fromString("8.0.0 (Oracle)");
        Optional<? extends JavaVersion> optional10 = JavaVersion.fromString("8_1 (Oracle)");
        Optional<? extends JavaVersion> optional11 = JavaVersion.fromString("8.0_1 (Oracle)");
        Optional<? extends JavaVersion> optional12 = JavaVersion.fromString("8.0.0_1 (Oracle)");

        AssertionsUtil.assertPresent(optional1);
        Assertions.assertEquals(JavaVersion.JAVA_8, optional1.get());
        AssertionsUtil.assertPresent(optional2);
        Assertions.assertEquals(JavaVersion.JAVA_8, optional2.get());
        AssertionsUtil.assertPresent(optional3);
        Assertions.assertEquals(JavaVersion.JAVA_8, optional3.get());
        AssertionsUtil.assertPresent(optional4);
        Assertions.assertEquals(JavaVersion.JAVA_8.withMinorVersion((short) 1), optional4.get());
        AssertionsUtil.assertPresent(optional5);
        Assertions.assertEquals(JavaVersion.JAVA_8.withMinorVersion((short) 1), optional5.get());
        AssertionsUtil.assertPresent(optional6);
        Assertions.assertEquals(JavaVersion.JAVA_8.withMinorVersion((short) 1), optional6.get());
        AssertionsUtil.assertPresent(optional7);
        Assertions.assertEquals(JavaVersion.JAVA_8, optional7.get());
        AssertionsUtil.assertPresent(optional8);
        Assertions.assertEquals(JavaVersion.JAVA_8, optional8.get());
        AssertionsUtil.assertPresent(optional9);
        Assertions.assertEquals(JavaVersion.JAVA_8, optional9.get());
        AssertionsUtil.assertPresent(optional10);
        Assertions.assertEquals(JavaVersion.JAVA_8.withMinorVersion((short) 1), optional10.get());
        AssertionsUtil.assertPresent(optional11);
        Assertions.assertEquals(JavaVersion.JAVA_8.withMinorVersion((short) 1), optional11.get());
        AssertionsUtil.assertPresent(optional12);
        Assertions.assertEquals(JavaVersion.JAVA_8.withMinorVersion((short) 1), optional12.get());
    }
}
