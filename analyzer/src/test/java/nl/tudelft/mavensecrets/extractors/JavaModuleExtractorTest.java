package nl.tudelft.mavensecrets.extractors;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashSet;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.Attributes.Name;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
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
import nl.tudelft.mavensecrets.NopResolver;

public class JavaModuleExtractorTest {

    private static Extractor extractor = null;
    private static Maven maven = null;
    private static Random random = null;
    private static byte[] buf = null;
    private static Consumer<Manifest> defaultManifest = null;
    private static IOConsumer<JarOutputStream> defaultStream = null;

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
        try (Package pkg = createPackage(createJar(defaultManifest, defaultStream))) {
            Object[] results = extractor.extract(maven, pkg);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(extractor.fields().length, results.length);
        }
    }

    @Test
    public void test_module_info_present_root() throws IOException {
        try (Package pkg = createPackage(createJar(defaultManifest, defaultStream.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("module-info.class"));
            writeBytes(jos);
            jos.closeEntry();
        })))) {
            Object[] results = extractor.extract(maven, pkg);
            Assertions.assertArrayEquals(new Object[] {true}, results);
        }
    }

    @Test
    public void test_module_info_present_nested() throws IOException {
        try (Package pkg = createPackage(createJar(defaultManifest, defaultStream.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("my-dir/module-info.class"));
            writeBytes(jos);
            jos.closeEntry();
        })))) {
            Object[] results = extractor.extract(maven, pkg);
            Assertions.assertArrayEquals(new Object[] {true}, results);
        }
    }

    @Test
    public void test_module_info_present_absent() throws IOException {
        try (Package pkg = createPackage(createJar(defaultManifest, defaultStream))) {
            Object[] results = extractor.extract(maven, pkg);
            Assertions.assertArrayEquals(new Object[] {false}, results);
        }
    }

    @BeforeAll
    public static void setup() {
        extractor = new JavaModuleExtractor();
        maven = new Maven(NopResolver.getInstance());
        random = new Random(1237L); // Set seed for determinism
        buf = new byte[1 << 10];
        defaultManifest = mf -> mf.getMainAttributes().put(new Name("my-key"), "my-value");
        defaultStream = jos -> {
            jos.putNextEntry(new ZipEntry("my-resource.txt"));
            writeBytes(jos);
            jos.closeEntry();
            jos.putNextEntry(new ZipEntry("my-class.class"));
            writeBytes(jos);
            jos.closeEntry();
            jos.putNextEntry(new ZipEntry("my-dir/"));
            jos.closeEntry();
            jos.putNextEntry(new ZipEntry("my-dir/my-nested-resource.txt"));
            writeBytes(jos);
            jos.closeEntry();
            jos.putNextEntry(new ZipEntry("my-dir/my-nested-class.class"));
            writeBytes(jos);
            jos.closeEntry();
        };
    }

    @AfterAll
    public static void teardown() {
        extractor = null;
        maven = null;
        random = null;
        buf = null;
        defaultManifest = null;
        defaultStream = null;
    }

    private static void writeBytes(OutputStream out) throws IOException {
        Objects.requireNonNull(out);

        random.nextBytes(buf);
        int len = random.nextInt(1, buf.length);
        out.write(buf, 0, len);
    }

    private static JarFile createJar(Consumer<Manifest> mfc, IOConsumer<JarOutputStream> josc) throws IOException {
        Objects.requireNonNull(mfc);
        Objects.requireNonNull(josc);

        Manifest mf = new Manifest();
        mfc.accept(mf);
        File jarFile = new File(dir, "my-jar.jar");
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(jarFile), mf)) {
            josc.accept(out);
        }

        
        return new JarFile(jarFile);
    }

    private static Package createPackage(JarFile jar) {
        Objects.requireNonNull(jar);

        return new Package(null, jar, null);
    }

    @FunctionalInterface
    private static interface IOConsumer<T> {
        void accept(T t) throws IOException;

        default IOConsumer<T> andThen(IOConsumer<? super T> consumer) {
            Objects.requireNonNull(consumer);

            return t -> {
                accept(t);
                consumer.accept(t);
            };
        }
    }
}
