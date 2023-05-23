package nl.tudelft.mavensecrets.selection;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;

import nl.tudelft.ArtifactId;

/**
 * A supplier of packages to analyze.
 */
@FunctionalInterface
public interface PackageSelector {

    /**
     * Get the packages to analyze.
     * There is no guarantee repeated calls produce the same results.
     *
     * @return The package list.
     * @throws IOException If an I/O error occurs.
     * @throws SQLException If a database error occurs.
     */
    Collection<? extends ArtifactId> getArtifacts(int page, int pageSize) throws IOException, SQLException;
}
