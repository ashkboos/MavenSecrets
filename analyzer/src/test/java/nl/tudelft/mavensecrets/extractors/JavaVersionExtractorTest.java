package nl.tudelft.mavensecrets.extractors;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import nl.tudelft.Database;
import nl.tudelft.Extractor;
import nl.tudelft.Field;
import nl.tudelft.Maven;
import nl.tudelft.Package;
import nl.tudelft.mavensecrets.JarUtil;
import nl.tudelft.mavensecrets.NopResolver;

public class JavaVersionExtractorTest {

    private static Extractor extractor = null;
    private static Maven maven = null;
    private static File file = null;
    private static String pkgName = null;
    private static Database db = null;

    @TempDir
    private static File dir;

    @Test
    public void test_fields_valid() {
        Field[] fields = extractor.fields();

        Assertions.assertNotNull(fields);
        Set<String> names = new HashSet<>();
        for (Field field : fields) {
            Assertions.assertNotNull(field);
            Assertions.assertNotNull(field.name());
            Assertions.assertNotNull(field.type());
            Assertions.assertTrue(names.add(field.name().toLowerCase()), "Duplicate field name: " + field.name());
        }
    }

    @Test
    public void test_no_jar() throws IOException, SQLException {
        try (Package pkg = createPackage(null)) {
            Object[] expected = new Object[extractor.fields().length];
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(expected, results);
        }
    }

    @Test
    public void test_correct_number_of_fields() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(extractor.fields().length, results.length);
        }
    }

    @Test
    public void test_manifest_created_by() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST.andThen(mf -> {
            mf.getMainAttributes().put(new Name("Created-By"), "1.7.0_06");
        }), JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] expected = new Object[extractor.fields().length];
            expected[0] = "1.7.0_06";
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(expected, results);
        }
    }

    @Test
    public void test_manifest_build_jdk() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST.andThen(mf -> {
            mf.getMainAttributes().put(new Name("Build-Jdk"), "1.8.0_201");
        }), JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] expected = new Object[extractor.fields().length];
            expected[1] = "1.8.0_201";
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(expected, results);
        }
    }

    @Test
    public void test_manifest_build_jdk_spec() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST.andThen(mf -> {
            mf.getMainAttributes().put(new Name("Build-Jdk-Spec"), "1.8");
        }), JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] expected = new Object[extractor.fields().length];
            expected[2] = "1.8";
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(expected, results);
        }
    }

    @Test
    public void test_manifest_multi_release_true() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST.andThen(mf -> {
            mf.getMainAttributes().put(Name.MULTI_RELEASE, "true");
        }), JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] expected = new Object[extractor.fields().length];
            expected[3] = true;
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(expected, results);
        }
    }

    @Test
    public void test_manifest_multi_release_false() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST.andThen(mf -> {
            mf.getMainAttributes().put(Name.MULTI_RELEASE, "false");
        }), JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] expected = new Object[extractor.fields().length];
            expected[3] = false;
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(expected, results);
        }
    }

    @Test
    public void test_manifest_multi_release_malformed() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST.andThen(mf -> {
            mf.getMainAttributes().put(Name.MULTI_RELEASE, "abc");
        }), JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] expected = new Object[extractor.fields().length];
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(expected, results);
        }
    }

    @Test
    public void test_class_malformed_length() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("my-class.class"));
            jos.write(new byte[1]);
            jos.closeEntry();
        }));
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] expected = new Object[extractor.fields().length];
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(expected, results);
        }
    }

    @Test
    public void test_class_malformed_magic() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("my-class.class"));
            jos.write(new byte[8]);
            jos.closeEntry();
        }));
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] expected = new Object[extractor.fields().length];
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(expected, results);
        }
    }

    @Test
    public void test_class_malformed_and_valid() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("my-class-0.class"));
            jos.write(new byte[1]);
            jos.closeEntry();
            jos.putNextEntry(new ZipEntry("my-class-1.class"));
            jos.write(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
            jos.write(new byte[] {1, 2, 3, 4});
            jos.closeEntry();
        }));
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] expected = new Object[extractor.fields().length];
            expected[4] = new byte[] {3, 4};
            expected[5] = new byte[] {1, 2};
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(expected, results);
        }
    }

    @Test
    public void test_class_absent() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] expected = new Object[extractor.fields().length];
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(expected, results);
        }
    }

    @Test
    public void test_class_single() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("my-class.class"));
            jos.write(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
            jos.write(new byte[] {1, 2, 3, 4});
            jos.closeEntry();
        }));
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] expected = new Object[extractor.fields().length];
            expected[4] = new byte[] {3, 4};
            expected[5] = new byte[] {1, 2};
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(expected, results);
        }
    }

    @Test
    public void test_class_multiple() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("my-class-0.class"));
            jos.write(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
            jos.write(new byte[] {1, 2, 3, 4});
            jos.closeEntry();
            jos.putNextEntry(new ZipEntry("my-class-1.class"));
            jos.write(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
            jos.write(new byte[] {5, 6, 7, 8});
            jos.closeEntry();
            jos.putNextEntry(new ZipEntry("my-class-2.class"));
            jos.write(new byte[] {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE});
            jos.write(new byte[] {5, 6, 7, 8});
            jos.closeEntry();
        }));
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] expected = new Object[extractor.fields().length];
            expected[4] = new byte[] {7, 8};
            expected[5] = new byte[] {5, 6};
            expected[6] = new byte[] {7, 8, 5, 6, 0, 0, 0, 2, 3, 4, 1, 2, 0, 0, 0, 1};
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(expected, results);
        }
    }

    @BeforeAll
    public static void setup() {
        extractor = new JavaVersionExtractor();
        maven = new Maven(NopResolver.getInstance());
        file = new File(dir, "my-jar.jar");
        pkgName = "";
        db = Mockito.mock(Database.class);
    }

    @AfterAll
    public static void teardown() {
        extractor = null;
        maven = null;
        file = null;
        pkgName = null;
        db = null;
    }

    private static Package createPackage(JarFile jar) {
        return new Package(null, jar, null);
    }
}
