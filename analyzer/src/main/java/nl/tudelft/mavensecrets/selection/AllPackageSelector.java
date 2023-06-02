package nl.tudelft.mavensecrets.selection;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import nl.tudelft.ArtifactId;
import nl.tudelft.Database;

/**
 * A {@link PackageSelector} that fetches all available packages from a database.
 */
public class AllPackageSelector implements PackageSelector {

    private final Database db;

    /**
     * Create a selector instance.
     *
     * @param db Database.
     */
    public AllPackageSelector(Database db) {
        this.db = Objects.requireNonNull(db);
    }

    @Override
    public Collection<? extends ArtifactId> getArtifacts(int page, int pageSize) throws IOException, SQLException {
        return Collections.unmodifiableCollection(db.getArtifactIds(page, pageSize));
    }
}
