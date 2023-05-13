package nl.tudelft.mavensecrets.extractors;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nl.tudelft.Extractor;
import nl.tudelft.Field;
import nl.tudelft.Maven;
import nl.tudelft.Package;
import nl.tudelft.mavensecrets.JarUtil;
import nl.tudelft.mavensecrets.NopResolver;

public class JavaModuleExtractorTest {

    private static Extractor extractor = null;
    private static Maven maven = null;
    private static File file = null;

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
    public void test_correct_number_of_fields() throws IOException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_CONTENT);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] results = extractor.extract(maven, pkg);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(extractor.fields().length, results.length);
        }
    }

    @Test
    public void test_module_info_present_root() throws IOException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_CONTENT.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("module-info.class"));
            JarUtil.writeBytes(jos);
            jos.closeEntry();
        }));
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] results = extractor.extract(maven, pkg);
            Assertions.assertArrayEquals(new Object[] {true}, results);
        }
    }

    @Test
    public void test_module_info_present_nested() throws IOException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_CONTENT.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("my-dir/module-info.class"));
            JarUtil.writeBytes(jos);
            jos.closeEntry();
        }));
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] results = extractor.extract(maven, pkg);
            Assertions.assertArrayEquals(new Object[] {true}, results);
        }
    }

    @Test
    public void test_module_info_present_absent() throws IOException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_CONTENT);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] results = extractor.extract(maven, pkg);
            Assertions.assertArrayEquals(new Object[] {false}, results);
        }
    }

    @BeforeAll
    public static void setup() {
        extractor = new JavaModuleExtractor();
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
