package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.mavensecrets.Package;
import nl.tudelft.mavensecrets.*;
import nl.tudelft.mavensecrets.testutils.JarUtil;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static org.mockito.Mockito.mock;

public class DependencyExtractorTest {
    private static Extractor extractor = null;
    private static Maven maven = null;
    private static File file = null;
    private static Database db = mock(Database.class);
    private static String pkgType = "jar";

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
        try (Package pkg = createPackage(new Model())) {
            Object[] results = extractor.extract(maven, pkg, pkgType, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(extractor.fields().length, results.length);
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
                Object[] results = extractor.extract(maven, pkg, pkgType, db);
                Assertions.assertArrayEquals(new Object[] {3, 0}, results);
            }
        }

    @Test
    public void test_transitive_dependencies() {
        PackageId id2 = new PackageId("ca.wheatstalk", "cdk-ecs-keycloak","0.0.58");
        Model m = new Model();
        PackageId id = new PackageId("top.infra", "spring-boot-starter-redisson", "1.0.0");
        Package pkg = new Package(id, null, m);
        try {
            Object[] results = extractor.extract(maven, pkg, pkgType, db);
            Assertions.assertArrayEquals(new Object[] {0, 49}, results);
        } catch (Exception e) {
            System.out.println(e);
        }
    }

    @BeforeAll
    public static void setup() {
        extractor = new DependencyExtractor();
        maven = mock(Maven.class);
        file = new File(dir, "my-jar.jar");
    }

    private static Package createPackage(Model pom) {
        Objects.requireNonNull(pom);
        return new Package(new PackageId("a", "b", "1.0"), null, pom);
    }

    public Dependency createDependency(PackageId id) {
        Dependency dependency = new Dependency();
        dependency.setGroupId(id.group());
        dependency.setArtifactId(id.artifact());
        dependency.setVersion(id.version());
        return dependency;
    }
}
