package nl.tudelft.mavensecrets.selection;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import nl.tudelft.Database;
import nl.tudelft.PackageId;

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
    public Collection<? extends PackageId> getPackages() throws IOException, SQLException {
        return Collections.unmodifiableCollection(db.getPackageIds());
    }
}
