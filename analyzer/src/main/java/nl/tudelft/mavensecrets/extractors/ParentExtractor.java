package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.util.Objects;

import nl.tudelft.*;
import nl.tudelft.Package;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;

/**
 * An extractor fetching parent information if available.
 */
public class ParentExtractor implements Extractor {

    private final Field[] fields = {
            new Field("parent_group_id", "VARCHAR(128)"),
            new Field("parent_artifact_id", "VARCHAR(128)"),
            new Field("parent_version", "VARCHAR(128)")
    };

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, Database db) throws IOException {
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);

        Object[] results = new Object[fields.length];

        Model model = pkg.pom();
        Parent parent = model.getParent();

        if (parent != null) {
            results[0] = parent.getGroupId();
            results[1] = parent.getArtifactId();
            results[2] = parent.getVersion();
        }
        return results;
    }
}
