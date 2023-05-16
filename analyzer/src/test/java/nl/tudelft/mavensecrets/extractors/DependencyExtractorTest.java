package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.*;
import nl.tudelft.Package;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentMatcher;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;


import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
                Assertions.assertArrayEquals(new Object[] {3, -1, 0, 0}, results);
            }
        }

    @Test
    public void test_transitive_dependencies() throws IOException, SQLException, PackageException {
//        Model m = new Model();
//        Model m2 = new Model();
//        Model m3 = new Model();
//        Model m4 = new Model();
//        PackageId id = new PackageId("a", "b", "1.0");
//        PackageId id2 = new PackageId("c", "d", "1.0");
//        PackageId id3 = new PackageId("e", "f", "1.0");
//        PackageId id4 = new PackageId("g", "h", "1.0");
//        PackageId id5 = new PackageId("g", "h", "1.1");
//        Dependency dependency = createDependency(id);
//        Dependency dependency2 = createDependency(id2);
//        Dependency dependency3 = createDependency(id3);
//        Dependency dependency4 = createDependency(id4);
//        Dependency dependency5 = createDependency(id5);
//        m.addDependency(dependency);
//        m.addDependency(dependency4);
//        m2.addDependency(dependency2);
//        m3.addDependency(dependency3);
//        m3.addDependency(dependency5);

//        org.jboss.shrinkwrap.resolver.api.maven.Maven mvn = mock(org.jboss.shrinkwrap.resolver.api.maven.Maven.class);
//        //when(org.jboss.shrinkwrap.resolver.api.maven.Maven.resolver()).thenReturn();
//        try (Package pkg = createPackage(m)) {
//            Object[] results = extractor.extract(maven, pkg, pkgType, db);
//            Assertions.assertArrayEquals(new Object[] {2, 5, 0, 0}, results);
//        }

        String id = "top.infra:spring-boot-starter-redisson:1.0.0";
        File[] files = org.jboss.shrinkwrap.resolver.api.maven.Maven.resolver().resolve(id).withTransitivity().asFile();
        System.out.println(files.length);
    }

    @Test
    public void test_transitive_dependencies_loop() throws IOException, SQLException, PackageException {
        Model m = new Model();
        Model m2 = new Model();
        Model m3 = new Model();

        PackageId id = new PackageId("a", "b", "1.0");
        PackageId id2 = new PackageId("c", "d", "1.0");
        PackageId id3 = new PackageId("e", "f", "1.0");
        PackageId id4 = new PackageId("g", "h", "1.1");
        Dependency dependency = createDependency(id);
        Dependency dependency2 = createDependency(id2);
        Dependency dependency3 = createDependency(id3);
        Dependency dependency4 = createDependency(id4);
        m.addDependency(dependency);
        m2.addDependency(dependency2);
        m3.addDependency(dependency3);
        m3.addDependency(dependency4);
        when(maven.getPom(argThat(new IdMatcher(id)))).thenReturn(m2);
        when(maven.getPom(argThat(new IdMatcher(id2)))).thenReturn(m3);
        when(maven.getPom(argThat(new IdMatcher(id3)))).thenReturn(m);

        try (Package pkg = createPackage(m)) {
            Object[] results = extractor.extract(maven, pkg, pkgType, db);
            Assertions.assertArrayEquals(new Object[] {1, 4, 0, 0}, results);
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

    public class IdMatcher implements ArgumentMatcher<PackageId> {
        private PackageId id;

        public IdMatcher(PackageId id) {
            this.id = id;
        }
        @Override
        public boolean matches(PackageId packageId) {
            return id.equals(packageId);
        }
    }
}
