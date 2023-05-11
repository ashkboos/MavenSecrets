package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.*;
import nl.tudelft.Package;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
//import eu.fasten.core.maven.resolution;

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
    public Object[] extract(Maven mvn, Package pkg, Database db) throws IOException {
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);
        Object[] result = new Object[2];
        //File pomFile = pkg.pom().getPomFile();
        Model m = pkg.pom();
        List<Dependency> dependencies = m.getDependencies();
        int directDependencies = dependencies.size();
        int transitiveDependencies = 0;

//        while(m.getDependencies().size() > 0) {
//            for(Dependency d : m.getDependencies()) {
//                PackageId dep = new PackageId(d.getGroupId(), d.getArtifactId(), d.getVersion());
//            }
//        }

        //Set<Revision> directDeps = getDependencies(pomFile, true);
        result[0] = directDependencies;
        result[1] = -1;
        return result;
    }
}
