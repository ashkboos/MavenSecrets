package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.util.Optional;
import java.util.jar.JarFile;

import nl.tudelft.mavensecrets.Database;
import nl.tudelft.mavensecrets.Field;
import nl.tudelft.mavensecrets.JarUtils;
import nl.tudelft.mavensecrets.Maven;
import nl.tudelft.mavensecrets.Package;

public class MismatchedPackagesExtractor implements Extractor {
    private static final Field[] fields = new Field[] {
            new Field("mismatched_packages", "TEXT")
    };

    @Override
    public Field[] fields() {
        return fields;
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) throws IOException {
        JarFile jar = pkg.jar();

        // Sanity check
        if (jar == null) {
            return new Object[fields.length];
        }

        var prefix = pkg.id().group().replace(".", "/");

        var mismatched = jar.stream()
                .map(i -> JarUtils.packageFromPath(i.getRealName()))
                .flatMap(Optional::stream)
                .filter(i -> isMismatched(prefix, i))
                .reduce((i, j) -> i + "," + j)
                .orElse(null);
        return new Object[] { mismatched };
    }

    private final boolean isMismatched(String prefix, String file) {
        return file.toLowerCase().endsWith(".class") && !file.startsWith(prefix);
    }
}
