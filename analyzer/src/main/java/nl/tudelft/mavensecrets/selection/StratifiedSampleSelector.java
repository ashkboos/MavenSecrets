package nl.tudelft.mavensecrets.selection;

import nl.tudelft.Database;
import nl.tudelft.PackageId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class StratifiedSampleSelector implements PackageSelector{
    private Logger LOGGER = LogManager.getLogger(StratifiedSampleSelector.class);
    private final int seed;
    private Database db;
    private Map<Integer, Integer> yearPopulationMap;

    public StratifiedSampleSelector(Database db, int seed, float samplePercent) throws SQLException {
        this.db = db;
        this.yearPopulationMap = db.getYearCounts();
        this.seed = seed;
        for (var kvPair : this.yearPopulationMap.entrySet()) {
            int year = kvPair.getKey();
            db.extractStrataSample(seed, samplePercent, year);
        }
    }

    /**
     * @return 
     * @throws IOException
     * @throws SQLException
     */

    @Override
    public Collection<? extends PackageId> getPackages() throws IOException, SQLException {
        LOGGER.debug(yearPopulationMap.toString());

        return new ArrayList<>();
    }
}
