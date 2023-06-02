package nl.tudelft.mavensecrets.extractors;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

import nl.tudelft.mavensecrets.Field;
import nl.tudelft.mavensecrets.extractors.Extractor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nl.tudelft.mavensecrets.extractors.DependencyExtractor;

public class DependencyExtractorTest {
    private static Extractor extractor = null;
    //private static Maven maven = null;
    //private static File file = null;

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


    @BeforeAll
    public static void setup() {
        extractor = new DependencyExtractor();
        //maven = new Maven(new DefaultResolver());
        //file = new File(dir, "my-jar.jar");
    }

    //private static Package createPackage(JarFile jar) {
    //    Objects.requireNonNull(jar);

    //    return new Package(null, jar, null);
    //}
}
