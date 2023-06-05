package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.util.HashSet;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import nl.tudelft.mavensecrets.Database;
import nl.tudelft.mavensecrets.Field;
import nl.tudelft.mavensecrets.JarUtils;
import nl.tudelft.mavensecrets.Maven;
import nl.tudelft.mavensecrets.Package;

public class EmbedExtractor implements Extractor {
    private static final Field[] fields = new Field[] {
            new Field("embedded_packages", "TEXT")
    };

    @Override
    public Field[] fields() {
        return fields;
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) throws IOException {
        // TODO: used vs. unused embeds
        // TODO: identify JARs by magic number

        var jar = pkg.jar();

        // Sanity check
        if (jar == null) {
            return new Object[fields.length];
        }

        var jars = jar.stream().filter(i -> i.getRealName().toLowerCase().endsWith(".jar")).toArray(JarEntry[]::new);
        var packages = new HashSet<String>();
        for (var entry : jars) {
            try (var zip = new ZipInputStream(jar.getInputStream(entry))) {
                ZipEntry file;
                while ((file = zip.getNextEntry()) != null) {
                    JarUtils.packageFromPath(file.getName()).ifPresent(packages::add);
                    zip.closeEntry();
                }
            } catch (IOException ex) {
                // ignored.
            }
        }

        return new Object[] {
                packages.stream().reduce((i, j) -> i + "," + j).orElse(null)
        };
    }
}
