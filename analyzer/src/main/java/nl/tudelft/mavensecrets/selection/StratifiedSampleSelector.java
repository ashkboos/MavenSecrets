package nl.tudelft.mavensecrets.selection;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import nl.tudelft.mavensecrets.ArtifactId;
import nl.tudelft.mavensecrets.Database;

public class StratifiedSampleSelector implements PackageSelector {

    private final Database db;
    private final long seed;
    private final double samplePercent;
    private final AtomicBoolean generated = new AtomicBoolean(false);

    public StratifiedSampleSelector(Database db, long seed, double samplePercent) {
        this.db = Objects.requireNonNull(db);
        this.seed = seed;
        this.samplePercent = samplePercent;
        if (samplePercent < 0D || samplePercent > 100D) {
            throw new IllegalArgumentException("Sample percent must be between 0 and 100");
        }
    }

    @Override
    public Collection<? extends ArtifactId> getArtifacts(int page, int pageSize) throws IOException, SQLException {
        // We assume page and pageSize is valid :)

        // Generate if not done so
        // Computational overhead is negligible
        if (!generated.getAndSet(true)) {
            generateSubset();
        }

        return Collections.unmodifiableCollection(db.getSelectedPkgs(page, pageSize));
    }

    /**
     * Generate the dataset.
     * Behaviour of repeated method calls is undefined.
     * 
     * @throws SQLException If a database error occurs.
     */
    private void generateSubset() throws SQLException {
        Map<Integer, Integer> yearPopulationMap = db.getYearCounts();
        db.createSelectedTable();
        for (Entry<Integer, Integer> entry : yearPopulationMap.entrySet()) {
            int year = entry.getKey();
            db.extractStrataSample(seed, samplePercent, year);
        }
    }
}
