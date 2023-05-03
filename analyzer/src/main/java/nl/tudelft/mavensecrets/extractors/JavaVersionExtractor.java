package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import nl.tudelft.Extractor;
import nl.tudelft.Field;
import nl.tudelft.Maven;
import nl.tudelft.Package;

/**
 * An extractor fetching Java versions from an artifact.
 * It looks both at <code>META-INF/MANIFEST.MD</code> and class bytecode.
 * It may be possible the bytecode read is from a class shaded in rather than a class from the actual project, in which case a different version may be detected. 
 */
public class JavaVersionExtractor implements Extractor {

    private final Field[] fields = {
            new Field("java_version_manifest", "VARCHAR(16)"),
            new Field("java_version_class_major", "BIT(16)"),
            new Field("java_version_class_minor", "BIT(16)")
    };

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg) throws IOException {
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);

        Object[] result = new Object[fields.length];

        JarFile jar = pkg.jar();

        // Manifest version if available
        Manifest manifest = jar.getManifest();
        result[0] = manifest == null ? null : manifest.getMainAttributes().get("Build-Jdk-Spec");

        // Class file
        JarEntry entry = jar.stream()
                .filter(je -> je.getName().endsWith(".class"))
                .findAny()
                .orElse(null);

        if (entry == null) {
            result[1] = null;
            result[2] = null;
        } else {
            try (InputStream stream = jar.getInputStream(entry)) {
                // https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.1

                byte[] buf = new byte[8];

                // Sanity check
                if (stream.read(buf) != buf.length || (buf[0] & 0xFF) != 0xCA || (buf[1] & 0xFF) != 0xFE || (buf[2] & 0xFF) != 0xBA || (buf[3] & 0xFF) != 0xBE) {
                    throw new IOException("Malformed header");
                }

                result[2] = Arrays.copyOfRange(buf, 4, 6);
                result[1] = Arrays.copyOfRange(buf, 6, 8);
            }
        }

        return result;
    }
}
