package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.*;
import nl.tudelft.Package;

import java.io.IOException;
import java.util.Optional;

public class MismatchedPackagesExtractor implements Extractor {
    private static final Field[] fields = new Field[] {
            new Field("mismatched_packages", "TEXT")
    };

    @Override
    public Field[] fields() {
        return fields;
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType) throws IOException {
        var prefix = pkg.id().group().replace(".", "/");
        var mismatched = pkg.jar().stream()
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
