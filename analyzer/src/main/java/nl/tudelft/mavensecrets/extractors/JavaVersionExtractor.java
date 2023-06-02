package nl.tudelft.mavensecrets.extractors;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.Attributes.Name;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.tudelft.Database;
import nl.tudelft.Extractor;
import nl.tudelft.Field;
import nl.tudelft.Maven;
import nl.tudelft.Package;

/**
 * An extractor fetching Java versions from an artifact.
 * It looks both at <code>META-INF/MANIFEST.MF</code> and class bytecode.
 * It may be possible the bytecode read is from a class shaded in rather than a class from the actual project, in which case a different version may be detected. 
 */
public class JavaVersionExtractor implements Extractor {

    // TODO: Built-By, X-Compile-Target-JDK, X-Compile-Source-JDK

    private static final Logger LOGGER = LogManager.getLogger(JavaVersionExtractor.class);
    private static final Name CREATED_BY = new Name("Created-By");
    private static final Name BUILD_JDK = new Name("Build-Jdk");
    private static final Name BUILD_JDK_SPEC = new Name("Build-Jdk-Spec");
    private static final Name MULTI_RELEASE = Name.MULTI_RELEASE;
    //private static final long CLASS_FILE_LIMIT = 25L; // Arbitrary limit

    private final Field[] fields = {
            new Field("java_version_manifest_1", "VARCHAR"), // Created-By
            new Field("java_version_manifest_2", "VARCHAR"), // Build-Jdk
            new Field("java_version_manifest_3", "VARCHAR"), // Build-Jdk-Spec
            new Field("java_version_manifest_multirelease", "BOOLEAN"),
            new Field("java_version_class_major", "BYTEA"),
            new Field("java_version_class_minor", "BYTEA"),
            new Field("java_version_class_map", "BYTEA")
    };

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) throws IOException, SQLException {
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);
        Objects.requireNonNull(pkgType);
        Objects.requireNonNull(db);

        JarFile jar = pkg.jar();
        LOGGER.trace("Found jar: {} ({})", jar != null, pkg.id());
        if (jar == null) {
            return new Object[fields.length];
        }

        Object[] result = new Object[fields.length];

        // Manifest version if available
        Manifest manifest = jar.getManifest();
        LOGGER.trace("Found META-INF/MANIFEST.MF: {} ({})", manifest != null, pkg.id());
        if (manifest != null) {
            Attributes attributes = manifest.getMainAttributes();
            result[0] = attributes.get(CREATED_BY);
            if (result[0] != null) {
                LOGGER.trace("Found {} entry in META-INF/MANIFEST.MF: {} ({})", CREATED_BY, result[0], pkg.id());
            }
            result[1] = attributes.get(BUILD_JDK);
            if (result[1] != null) {
                LOGGER.trace("Found {} entry in META-INF/MANIFEST.MF: {} ({})", BUILD_JDK, result[1], pkg.id());
            }
            result[2] = attributes.get(BUILD_JDK_SPEC);
            if (result[2] != null) {
                LOGGER.trace("Found {} entry in META-INF/MANIFEST.MF: {} ({})", BUILD_JDK_SPEC, result[2], pkg.id());
            }
            Object value = attributes.get(MULTI_RELEASE);
            result[3] = parseBoolStrict(value);
            if (value != null && result[3] == null) {
                LOGGER.trace("Found malformed {} entry in META-INF/MANIFEST.MF: {} ({})", MULTI_RELEASE, value, pkg.id());
            }
            if (result[3] != null) {
                LOGGER.trace("Found {} entry in META-INF/MANIFEST.MF: {} ({})", MULTI_RELEASE, result[3], pkg.id());
            }
        }

        // Class file
        Map<JavaClassVersion, Integer> versions = new HashMap<>();
        List<JarEntry> entries = jar.stream()
                .filter(je -> je.getName().endsWith(".class"))
                //.limit(CLASS_FILE_LIMIT)
                .toList();
        //LOGGER.trace("Found {}/{} class file(s) to analyze ({})", entries.size(), CLASS_FILE_LIMIT, pkg.id());
        LOGGER.trace("Found {} class file(s) to analyze ({})", entries.size(), pkg.id());
        for (JarEntry entry : entries) {
            JavaClassVersion jcv;
            try (InputStream stream = jar.getInputStream(entry)) {
                jcv = fetchClassVersion(stream);
            }
            if (jcv == null) {
                LOGGER.trace("Failed to fetch class version from {}: malformed class header? ({})", entry.getName(), pkg.id());
                continue;
            }
            versions.merge(jcv, 1, Integer::sum);
        }
        if (!versions.isEmpty()) {
            LOGGER.trace("Found version occurrences: {} ({})", versions, pkg.id());
        }

        // Only needed if there are multiple versions
        if (versions.size() > 1) {
            result[6] = serializeVersionMap(versions);
        }

        // Find most frequent type
        versions.entrySet()
                .stream()
                .sorted(Collections.reverseOrder(Entry.comparingByValue()))
                .map(Entry::getKey)
                .findFirst()
                .ifPresent(jcv -> {
                    LOGGER.trace("Found most common Java class version: {} ({})" + jcv, pkg.id());
                    result[4] = jcv.major();
                    result[5] = jcv.minor();
                });

        return result;
    }

    /**
     * Read the class version from a stream.
     *
     * @param stream Class {@link InputStream}.
     * @return The class versions.
     * @throws IOException If an I/O error occurs.
     */
    private JavaClassVersion fetchClassVersion(InputStream stream) throws IOException {
        Objects.requireNonNull(stream);

        // https://docs.oracle.com/javase/specs/jvms/se17/html/jvms-4.html#jvms-4.1

        byte[] buf = new byte[8];

        // Sanity check
        // 0xCAFEBABE magic number
        if (stream.read(buf) != buf.length || (buf[0] & 0xFF) != 0xCA || (buf[1] & 0xFF) != 0xFE || (buf[2] & 0xFF) != 0xBA || (buf[3] & 0xFF) != 0xBE) {
            return null;
        }

        byte[] minor = Arrays.copyOfRange(buf, 4, 6);
        byte[] major = Arrays.copyOfRange(buf, 6, 8);
        return new JavaClassVersion(major, minor);
    }

    /**
     * Serialize a class version map into (proprietary) binary data.
     *
     * @param map Map to serialize.
     * @return The serialized map.
     */
    private byte[] serializeVersionMap(Map<JavaClassVersion, Integer> map) {
        Objects.requireNonNull(map);

        byte[] array;
        try (ByteArrayOutputStream stream = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(stream)) {
            for (Entry<JavaClassVersion, Integer> entry : map.entrySet()) {
                JavaClassVersion jcv = entry.getKey();
                out.write(jcv.major());
                out.write(jcv.minor());
                out.writeInt(entry.getValue());
            }
            array = stream.toByteArray();
        } catch (IOException exception) {
            throw new AssertionError(exception);
        }
        return array;
    }

    /**
     * Strict boolean parsing.
     * 
     * @param string Input string.
     * @return The parsed boolean or <code>null</code> if not valid.
     */
    private Boolean parseBoolStrict(Object object) {
        if (object == null || !(object instanceof String)) {
            return null;
        }

        switch (((String) object).toLowerCase()) {
            case "true":
                return Boolean.TRUE;
            case "false":
                return Boolean.FALSE;
            default:
                return null;
        }
    }

    /**
     * A Java class version record storing both major and minor class versions.
     */
    private static record JavaClassVersion(byte[] major, byte[] minor) {

        private JavaClassVersion(byte[] major, byte[] minor) {
            Objects.requireNonNull(major);
            Objects.requireNonNull(minor);
            if (major.length != 2) {
                throw new IllegalArgumentException("Invalid major version, found " + major.length + " byte(s), but expected 2 bytes");
            }
            if (minor.length != 2) {
                throw new IllegalArgumentException("Invalid minor version, found " + minor.length + " byte(s), but expected 2 bytes");
            }
            this.major = major;
            this.minor = minor;
        }

        @Override
        public byte[] major() {
            return major.clone();
        }

        @Override
        public byte[] minor() {
            return minor.clone();
        }

        /*
         * Records do not do deep array comparison 
         */

        @Override
        public int hashCode() {
            int result = 37;
            result = result * 17 + Arrays.hashCode(major);
            result = result * 17 + Arrays.hashCode(minor);
            return result;
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof JavaClassVersion other) {
                return Arrays.equals(major, other.major())
                        && Arrays.equals(minor, other.minor());
            }
            return false; 
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append(this.getClass().getSimpleName())
                    .append("[major=")
                    .append(Arrays.toString(major))
                    .append(", minor=")
                    .append(Arrays.toString(minor))
                    .append(']')
                    .toString();
        }
    }
}
