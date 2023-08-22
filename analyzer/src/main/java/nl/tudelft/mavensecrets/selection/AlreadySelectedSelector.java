package nl.tudelft.mavensecrets.selection;

import nl.tudelft.mavensecrets.ArtifactId;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import nl.tudelft.mavensecrets.Database;

/**
 * This {@link PackageSelector} does NOT perform any selection. We use it to skip selection
 * iff selection has already been performed and the <i>selected_packages</i> table exists.
 */
public class AlreadySelectedSelector implements PackageSelector{
    private final Database db;

    public AlreadySelectedSelector(Database db) {
        this.db = Objects.requireNonNull(db);
    }

    /**
     * Returns
     * @param page
     * @param pageSize
     * @return
     * @throws SQLException
     */
    @Override
    public Collection<? extends ArtifactId> getArtifacts(int page, int pageSize) throws SQLException {
        return Collections.unmodifiableCollection(db.getSelectedPkgs(page, pageSize));
    }
}
