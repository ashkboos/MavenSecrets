package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.Extractor;
import nl.tudelft.Field;
import nl.tudelft.Maven;
import nl.tudelft.Package;
import org.apache.maven.model.Dependency;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class DependencyExtractor implements Extractor {
    private final Field[] fields = {
            new Field("directdependencies", "INTEGER"),
            new Field("transitivedependencies", "INTEGER")
    };

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg) throws IOException {
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);
        Object[] result = new Object[2];

        File pomFile = pkg.pom().getPomFile();
        List<Dependency> dependencies = pkg.pom().getDependencies();
//        Set<Revision> directDeps = getDependencies(pomFile, true);
        return result;
    }
}
