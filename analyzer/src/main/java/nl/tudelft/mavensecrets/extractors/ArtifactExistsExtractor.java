package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.tudelft.mavensecrets.Database;
import nl.tudelft.mavensecrets.Field;
import nl.tudelft.mavensecrets.Maven;
import nl.tudelft.mavensecrets.Package;

/**
 * An extractor checking if an artifact has some form of archive associated with it.
 */
public class ArtifactExistsExtractor implements Extractor {

    private static final Logger LOGGER = LogManager.getLogger(ParentExtractor.class);

    private final Field[] fields = new Field[] {
            new Field("has_artifact", "BOOLEAN")
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

        boolean hasJar = pkg.jar() != null;
        LOGGER.trace("Found jar: {} ({})", hasJar, pkg.id());
        return new Object[] {hasJar};
    }
}
