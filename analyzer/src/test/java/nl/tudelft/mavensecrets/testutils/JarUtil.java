package nl.tudelft.mavensecrets.testutils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.Random;
import java.util.function.Consumer;
import java.util.jar.Attributes.Name;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

/**
 * Jar utilities for testing.
 */
public class JarUtil {

    public static final Consumer<Manifest> DEFAULT_MANIFEST = mf -> mf.getMainAttributes().put(Name.MANIFEST_VERSION, "1.0");
    public static final IOConsumer<JarOutputStream> DEFAULT_RESOURCES = jos -> {
        jos.putNextEntry(new ZipEntry("my-resource.txt"));
        writeBytes(jos);
        jos.closeEntry();
        jos.putNextEntry(new ZipEntry("my-dir/"));
        jos.closeEntry();
        jos.putNextEntry(new ZipEntry("my-dir/my-nested-resource.txt"));
        writeBytes(jos);
        jos.closeEntry();
    };
    public static final IOConsumer<JarOutputStream> DEFAULT_CONTENT = DEFAULT_RESOURCES.andThen(jos -> {
        jos.putNextEntry(new ZipEntry("my-class.class"));
        writeBytes(jos);
        jos.closeEntry();
        jos.putNextEntry(new ZipEntry("my-dir/my-nested-class.class"));
        writeBytes(jos);
        jos.closeEntry();
    });

    private static final Random RANDOM = new Random();

    private JarUtil() {
        // Nothing
    }

    /**
     * Write random bytes.
     *
     * @param out Target stream.
     * @throws IOException If an I/O error occurs.
     */
    public static void writeBytes(OutputStream out) throws IOException {
        Objects.requireNonNull(out);

        byte[] buf = new byte[1 << 10];
        RANDOM.nextBytes(buf);
        int len = RANDOM.nextInt(1, buf.length);
        out.write(buf, 0, len);
    }

    /**
     * Create a Jar file.
     *
     * @param file Target file.
     * @param mfc Manifest writer.
     * @param josc Content writer.
     * @throws IOException If an I/O error occurs.
     */
    public static void createJar(File file, Consumer<Manifest> mfc, IOConsumer<JarOutputStream> josc) throws IOException {
        Objects.requireNonNull(file);
        Objects.requireNonNull(mfc);
        Objects.requireNonNull(josc);

        Manifest mf = new Manifest();
        mfc.accept(mf);
        try (JarOutputStream out = new JarOutputStream(new FileOutputStream(file), mf)) {
            josc.accept(out);
        }
    }

    /**
     * A {@link java.util.function.Consumer} that can throw an {@link IOException}.
     *
     * @param <T> Argument type.
     */
    @FunctionalInterface
    public static interface IOConsumer<T> {
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
