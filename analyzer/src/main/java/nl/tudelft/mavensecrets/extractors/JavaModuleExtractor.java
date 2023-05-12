package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.util.Objects;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import nl.tudelft.Extractor;
import nl.tudelft.Field;
import nl.tudelft.Maven;
import nl.tudelft.Package;

/**
 * An extractor fetching whether or not an artifact uses Java modules.
 */
public class JavaModuleExtractor implements Extractor {

    private final Field[] fields = {
            new Field("use_java_modules", "BOOLEAN")
    };

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType) throws IOException {
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);

        JarFile jar = pkg.jar();
        boolean useModules = jar.stream()
                .map(ZipEntry::getName)
                .anyMatch(str -> {
                    int i = str.lastIndexOf('/');
                    String s = i == -1 ? str : str.substring(i + 1);
                    return s.equals("module-info.class");
                });
        return new Object[] {useModules};
    }
}
