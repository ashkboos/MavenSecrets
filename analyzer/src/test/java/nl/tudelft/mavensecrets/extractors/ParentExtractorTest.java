package nl.tudelft.mavensecrets.extractors;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import nl.tudelft.mavensecrets.Database;
import nl.tudelft.mavensecrets.Field;
import nl.tudelft.mavensecrets.Maven;
import nl.tudelft.mavensecrets.Package;
import nl.tudelft.mavensecrets.testutils.NopResolver;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class ParentExtractorTest {

    private static Extractor extractor = null;
    private static Maven maven = null;
    private static Database db = mock(Database.class);
    private static String pkgName = "";

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
        try (Package pkg = createPackage(new Model())) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(extractor.fields().length, results.length);
        }
    }

    @Test
    public void test_parent_present() throws IOException, SQLException  {
        Model model = new Model();
        Parent parent = new Parent();
        parent.setGroupId("my-group-id");
        parent.setArtifactId("my-artifact-id");
        parent.setVersion("1.0");
        model.setParent(parent);

        try (Package pkg = createPackage(model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {"my-group-id", "my-artifact-id", "1.0"}, results);
        }
    }
    
    @Test
    public void test_parent_absent() throws IOException, SQLException  {
        try (Package pkg = createPackage(new Model())) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {null, null, null}, results);
        }
    }

    @BeforeAll
    public static void setup() {
        extractor = new ParentExtractor();
        maven = new Maven(NopResolver.getInstance());
    }

    @AfterAll
    public static void teardown() {
        extractor = null;
        maven = null;
    }

    private static Package createPackage(Model model) {
        Objects.requireNonNull(model);

        return new Package(null, null, model);
    }
}
