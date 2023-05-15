package nl.tudelft.mavensecrets.extractors;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import nl.tudelft.*;
import nl.tudelft.Package;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nl.tudelft.mavensecrets.JarUtil;
import nl.tudelft.mavensecrets.NopResolver;

import static org.mockito.Mockito.mock;

public class JavaVersionExtractorTest {

    private static Extractor extractor = null;
    private static Maven maven = null;
    private static File file = null;
    private static String pkgName = "";
    private static Database db = mock(Database.class);

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
    public void test_correct_number_of_fields() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(extractor.fields().length, results.length);
        }
    }

    @Test
    public void test_manifest_build_jdk() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST.andThen(mf -> {
            mf.getMainAttributes().put(new Name("Build-Jdk"), "1.8.0_201");
        }), JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            //System.out.println(file.getAbsolutePath());
            //while (true) {}
            Assertions.assertArrayEquals(new Object[] {"1.8.0_201", null, null, null}, results);
        }
    }

    @Test
    public void test_manifest_build_jdk_spec() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST.andThen(mf -> {
            mf.getMainAttributes().put(new Name("Build-Jdk-Spec"), "1.8");
        }), JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {null, "1.8", null, null}, results);
        }
    }

    @Test
    public void test_class_malformed_length() throws IOException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("my-class.class"));
            jos.write(new byte[1]);
            jos.closeEntry();
        }));
        try (Package pkg = createPackage(new JarFile(file))) {
            Assertions.assertThrows(IOException.class, () -> extractor.extract(maven, pkg, pkgName, db));
        }
    }

    @Test
    public void test_class_malformed_magic() throws IOException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("my-class.class"));
            jos.write(new byte[8]);
            jos.closeEntry();
        }));
        try (Package pkg = createPackage(new JarFile(file))) {
            Assertions.assertThrows(IOException.class, () -> extractor.extract(maven, pkg, pkgName, db));
        }
    }

    @Test
    public void test_class_absent() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {null, null, null, null}, results);
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
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {null, null, new byte[] {3, 4}, new byte[] {1, 2}}, results);
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
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {null, null, new byte[] {7, 8}, new byte[] {5, 6}}, results);
        }
    }

    @BeforeAll
    public static void setup() {
        extractor = new JavaVersionExtractor();
        maven = new Maven(NopResolver.getInstance());
        file = new File(dir, "my-jar.jar");
    }

    @AfterAll
    public static void teardown() {
        extractor = null;
        maven = null;
        file = null;
    }

    private static Package createPackage(JarFile jar) {
        Objects.requireNonNull(jar);

        return new Package(null, jar, null);
    }
}
