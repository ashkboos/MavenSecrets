package nl.tudelft.mavensecrets.extractors;

import java.io.File;
import java.io.IOException;
import java.util.*;

import nl.tudelft.Package;
import nl.tudelft.*;
import nl.tudelft.mavensecrets.resolver.DefaultResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
//import org.jboss.shrinkwrap.resolver.api.maven;

public class DependencyExtractor implements Extractor {
    private DefaultResolver defaultResolver = new DefaultResolver();
    private static final Logger LOGGER = LogManager.getLogger(Runner.class);;
    private final Field[] fields = {
            new Field("directdependencies", "INTEGER"),
            new Field("transitivedependencies", "INTEGER"),
//            new Field("idisnull", "INTEGER"),
//            new Field("transitivenotresolved", "INTEGER")
    };

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) throws IOException {
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);
        Object[] result = new Object[2];
        Model m = pkg.pom();
        List<Dependency> dependencies = m.getDependencies();
        int directDependencies = dependencies.size();
        File[] files = org.jboss.shrinkwrap.resolver.api.maven.Maven.resolver().resolve(pkg.toString()).withTransitivity().asFile();
        result[0] = directDependencies;
        result[1] = files.length;
        return result;
    }
}
