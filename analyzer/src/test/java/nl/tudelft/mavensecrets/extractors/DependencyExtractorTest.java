package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.*;
import nl.tudelft.Package;
import nl.tudelft.mavensecrets.JarUtil;
import nl.tudelft.mavensecrets.extractors.DependencyExtractor;


import nl.tudelft.mavensecrets.resolver.DefaultResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

public class DependencyExtractorTest {
    private static Extractor extractor = null;
    private static Maven maven = null;
    private static File file = null;
    private static Database db = Mockito.mock(Database.class);

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
    public void test_direct_dependencies() throws IOException, SQLException {
            Model m = new Model();
            Dependency dependency = new Dependency();
            Dependency dependency2 = new Dependency();
            Dependency dependency3 = new Dependency();
            m.addDependency(dependency);
            m.addDependency(dependency2);
            m.addDependency(dependency3);
            try (Package pkg = createPackage(m)) {
                Object[] results = extractor.extract(maven, pkg, db);
                Assertions.assertArrayEquals(new Object[] {3, -1}, results);
            }
        }

    @Test
    public void test_transitive_dependencies() throws IOException, SQLException {
        Model m = new Model();
        Model m2 = new Model();
        Model m3 = new Model();
        Dependency dependency = new Dependency();
        Dependency dependency2 = new Dependency();
        Dependency dependency3 = new Dependency();
        m.addDependency(dependency);
        m2.addDependency(dependency2);
        m3.addDependency(dependency3);
        try (Package pkg = createPackage(m)) {
            Object[] results = extractor.extract(maven, pkg, db);
            Assertions.assertArrayEquals(new Object[] {3, -1}, results);
        }
    }


    @BeforeAll
    public static void setup() {
        extractor = new DependencyExtractor();
        maven = new Maven(new DefaultResolver());
        file = new File(dir, "my-jar.jar");
    }

    private static Package createPackage(Model pom) {
        Objects.requireNonNull(pom);
        return new Package(null, null, pom);
    }
}
