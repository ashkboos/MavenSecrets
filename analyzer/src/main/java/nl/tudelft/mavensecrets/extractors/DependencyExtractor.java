package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.util.*;

import nl.tudelft.Package;
import nl.tudelft.*;
import nl.tudelft.mavensecrets.resolver.DefaultResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;
//import org.jboss.shrinkwrap.resolver.api.maven;

public class DependencyExtractor implements Extractor {
    private DefaultResolver defaultResolver = new DefaultResolver();
    private static final Logger LOGGER = LogManager.getLogger(Runner.class);;
    private final Field[] fields = {
            new Field("directdependencies", "INTEGER"),
            new Field("transitivedependencies", "INTEGER")
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
        String id = pkg.id().group() + ":" + pkg.id().artifact() + ":" + pkgType + ":" + pkg.id().version();
        List<MavenCoordinate> files = resolve(id);
        int a = files.size() - 1;
        if(a < 0) a = 0;
        result[0] = directDependencies;
        result[1] = a;
        return result;
    }

    private static List<org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate> resolve(
            final String row) {
        List<MavenCoordinate> result =
                new ArrayList<>();
        try {
            result = org.jboss.shrinkwrap.resolver.api.maven.Maven.resolver().resolve(row).withTransitivity()
                    .asList(org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate.class);
        } catch (Exception e) {
            System.out.println("Exception occurred while resolving " + row + ", " + e);
            System.out.println(e);
            return result;
        }
        return result;
    }
}
