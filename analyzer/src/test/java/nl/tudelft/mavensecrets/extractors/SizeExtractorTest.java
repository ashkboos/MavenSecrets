package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.mavensecrets.Package;
import nl.tudelft.mavensecrets.*;
import nl.tudelft.mavensecrets.testutils.JarUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

public class SizeExtractorTest {
    private static SizeExtractor extractor = null;
    private static Maven maven = null;
    private static String pkgName = "jar";
    private static File file = null;
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
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_CONTENT);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(extractor.fields().length, results.length);
        }
    }

    @Test
    public void test_no_jar() throws IOException, SQLException {
        try (Package pkg = createPackage(null)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {null, null}, results);
        }
    }

    @Test
    public void testSizeAndNumberOfFiles() throws Exception {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_CONTENT.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("module-info.class"));
            JarUtil.writeBytes(jos);
            jos.closeEntry();
        }));
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            long size = 0;
            Enumeration<JarEntry> files = pkg.jar().entries();
            while (files.hasMoreElements()) {
                size += files.nextElement().getSize();
            }
            Assertions.assertArrayEquals(new Object[] {size, (pkg.jar().size() - countDirectories(pkg.jar()))}, results);
        }


    }


    @Test
    public void testSizeExtractorExtensions() throws Exception {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_CONTENT.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("module-info.class"));
            jos.putNextEntry(new ZipEntry("demo.class"));
            jos.putNextEntry(new ZipEntry("demo2.class"));
            jos.putNextEntry(new ZipEntry("demo3.class"));
            jos.putNextEntry(new ZipEntry("pom.xml"));
            jos.putNextEntry(new ZipEntry("hello.ending"));
            jos.putNextEntry(new ZipEntry("helloworld.ending"));
            JarUtil.writeBytes(jos);
            jos.closeEntry();
        }));
        try (Package pkg = createPackage(new JarFile(file))) {
            extractor.extract(maven, pkg, pkgName, db);
            Map<String, SizeExtractor.ExtensionInfo> extensionInfo = extractor.getExtensionsTesting();
            assertEquals(1, extensionInfo.get("mf").count);
            assertEquals(4, extensionInfo.get("class").count - 2);
            assertEquals(1, extensionInfo.get("xml").count);
            assertEquals(2, extensionInfo.get("ending").count);
        }
    }

    public static int countDirectories(JarFile jar) {
        int count = 0;
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                count++;
            }
        }
        return count;
    }

    @BeforeAll
    public static void setup() {
        extractor = new SizeExtractor();
        maven = mock(Maven.class);
        file = new File(dir, "my-jar.jar");
    }

    private static Package createPackage(JarFile jar) {
        return new Package(new PackageId("a", "b", "1.0"), jar, null);
    }
}
