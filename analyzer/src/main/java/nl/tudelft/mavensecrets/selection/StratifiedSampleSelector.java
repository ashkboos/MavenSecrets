package nl.tudelft.mavensecrets.selection;

import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import nl.tudelft.Database;
import nl.tudelft.PackageId;

public class StratifiedSampleSelector implements PackageSelector {

    //private static final Logger LOGGER = LogManager.getLogger(StratifiedSampleSelector.class);
    private final Database db;

    public StratifiedSampleSelector(Database db, long seed, float samplePercent) throws SQLException {
        Objects.requireNonNull(db);
        if (samplePercent < 0 || samplePercent > 100) {
            throw new IllegalArgumentException("Sample percent must be between 0 and 100");
        }

        this.db = db;
        Map<Integer, Integer> yearPopulationMap = db.getYearCounts();
        db.createSelectedTable();
        for (var kvPair : yearPopulationMap.entrySet()) {
            int year = kvPair.getKey();
            db.extractStrataSample(seed, samplePercent, year);
        }
    }

    @Override
    public Collection<? extends PackageId> getPackages() throws SQLException {
        return Collections.unmodifiableCollection(db.getSelectedPkgs());
    }
}
