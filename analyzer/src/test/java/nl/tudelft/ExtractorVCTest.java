package nl.tudelft;

import nl.tudelft.mavensecrets.resolver.DefaultResolver;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.util.Objects;

class ExtractorVCTest {
    private static Extractor extractor;
    private static Maven maven;
    private static File file;
    private static String pkgName = "";

    @BeforeAll
    static void setUp() {
        extractor = new ExtractorVC();
        maven = new Maven(new DefaultResolver("/.m2/test"));
    }

    @AfterAll
    static void tearDown() {
    }

    @Test
    public void testNumFields () throws IOException {
        try (Package pkg = createPackage(new Model())) {
            Object[] results = extractor.extract(maven, pkg, pkgName);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(extractor.fields().length, results.length);
        }
    }

    @Test
    void extract() {
    }

    private static Package createPackage(Model model) {
        return new Package(new PackageId("org.test","test", "1.42"), null, model);
    }
}