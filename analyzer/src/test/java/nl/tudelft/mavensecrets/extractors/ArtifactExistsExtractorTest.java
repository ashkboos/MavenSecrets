package nl.tudelft.mavensecrets.extractors;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Set;
import java.util.jar.JarFile;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import nl.tudelft.mavensecrets.Database;
import nl.tudelft.mavensecrets.Field;
import nl.tudelft.mavensecrets.Maven;
import nl.tudelft.mavensecrets.Package;
import nl.tudelft.mavensecrets.testutils.JarUtil;
import nl.tudelft.mavensecrets.testutils.NopResolver;

public class ArtifactExistsExtractorTest {

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
    public void test_correct_number_of_fields() throws IOException, SQLException {
        try (Package pkg = createPackage(null)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(extractor.fields().length, results.length);
        }
    }

    @Test
    public void test_no_jar() throws IOException, SQLException {
        try (Package pkg = createPackage(null)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {false}, results);
        }
    }

    @Test
    public void test_jar() throws IOException, SQLException {
        JarUtil.createJar(file, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_CONTENT);
        try (Package pkg = createPackage(new JarFile(file))) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {true}, results);
        }
    }

    @BeforeAll
    public static void setup() {
        extractor = new ArtifactExistsExtractor();
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
