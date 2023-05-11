package nl.tudelft.mavensecrets.extractors;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import nl.tudelft.Extractor;
import nl.tudelft.Field;
import nl.tudelft.Maven;
import nl.tudelft.Package;
import nl.tudelft.mavensecrets.NopResolver;
import nl.tudelft.mavensecrets.resolver.Resolver;

public class ParentExtractorTest {

    private static Extractor extractor = null;
    private static Maven maven = null;

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
    public void test_correct_number_of_fields() {
        Package pkg = createPackage(new Model());

        Object[] results;
        try {
            results = extractor.extract(maven, pkg);
        } catch (IOException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertNotNull(results);
        Assertions.assertEquals(extractor.fields().length, results.length);
    }

    @Test
    public void test_parent_present() {
        Model model = new Model();
        Parent parent = new Parent();
        parent.setGroupId("my-group-id");
        parent.setArtifactId("my-artifact-id");
        parent.setVersion("1.0");
        model.setParent(parent);

        Package pkg = createPackage(model);

        Object[] results;
        try {
            results = extractor.extract(maven, pkg);
        } catch (IOException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertArrayEquals(new Object[] {"my-group-id", "my-artifact-id", "1.0"}, results);
    }
    
    @Test
    public void test_parent_absent() {
        Package pkg = createPackage(new Model());

        Object[] results;
        try {
            results = extractor.extract(maven, pkg);
        } catch (IOException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertArrayEquals(new Object[] {null, null, null}, results);
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
