package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

import nl.tudelft.mavensecrets.Database;
import nl.tudelft.mavensecrets.Field;
import nl.tudelft.mavensecrets.Maven;
import nl.tudelft.mavensecrets.Package;

/**
 * An extractor fetching parent information if available.
 */
public class ParentExtractor implements Extractor {

    private static final Logger LOGGER = LogManager.getLogger(ParentExtractor.class);

    private final Field[] fields = {
            new Field("parent_group_id", "VARCHAR"),
            new Field("parent_artifact_id", "VARCHAR"),
            new Field("parent_version", "VARCHAR")
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

        Object[] results = new Object[fields.length];

        Model model = pkg.pom();
        Parent parent = model.getParent();

        LOGGER.trace("Found 'parent' defined in POM: {} ({})", parent != null, pkg.id());
        if (parent != null) {
            results[0] = parent.getGroupId();
            results[1] = parent.getArtifactId();
            results[2] = parent.getVersion();
            LOGGER.trace("Found parent artifact defined in POM: {}:{}:{} ({})", results[0], results[1], results[2], pkg.id());
        }
        return results;
    }
}
