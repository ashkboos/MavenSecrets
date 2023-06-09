package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

import nl.tudelft.mavensecrets.Database;
import nl.tudelft.mavensecrets.Field;
import nl.tudelft.mavensecrets.Maven;
import nl.tudelft.mavensecrets.Package;

public class DependencyExtractor implements Extractor {

    //private static final Logger LOGGER = LogManager.getLogger(DependencyExtractor.class);

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
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) throws IOException, SQLException {
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);
        Object[] result = new Object[2];
        //File pomFile = pkg.pom().getPomFile();
        Model m = pkg.pom();
        List<Dependency> dependencies = m.getDependencies();
        int directDependencies = dependencies.size();
        result[0] = directDependencies;
        //insert -1 for the transitive dependencies because shrinkwrap does not work in this branch
        result[1] = -1;
        return result;
        //Set<Revision> directDeps = getDependencies(pomFile, true);
    }
}
