package nl.tudelft;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Runner implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger(Runner.class);
    private final Database db;
    private final Map<Class<?>, Extractor> extractors = new HashMap<>();

    Runner(Database db) {
        this.db = db;
    }

    Runner addExtractor(Extractor extractor) throws SQLException {
        LOGGER.trace("Adding extractor '" + extractor + "': " + extractor.getClass());
        db.updateSchema(extractor.fields());
        extractors.putIfAbsent(extractor.getClass(), extractor);

        return this;
    }

    void run(Maven mvn, Collection<ArtifactId> packages, int threads) throws SQLException, InterruptedException {
        var fields = extractors.values().stream()
                .map(Extractor::fields)
                .flatMap(Arrays::stream)
                .toArray(Field[]::new);
        if (fields.length == 0)
            return;

        db.createUnresolvedTable(false);
        var pool = new ForkJoinPool(threads);
        try {
            pool.submit(() -> packages.parallelStream().forEach(id -> execute(mvn, fields, id))).get();
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } finally {
            pool.shutdown();
        }
    }

    private void execute(Maven mvn, Field[] fields, ArtifactId id) {
        try (var artifact = mvn.getPackage(id)) {
            var offset = 0;
            Object[] values = new Object[fields.length];
            for (var extractor : extractors.values()) {
                var result = extractor.extract(mvn, artifact, id.extension(), db);
                if (result.length != extractor.fields().length)
                    throw new RuntimeException("Extractor '" + extractor + "' returned unexpected number of values");

                for (var i = 0; i < result.length; offset++, i++)
                    values[offset] = result[i];
            }

            db.update(id, fields, values, true);
        } catch (PackageException | IOException | SQLException ex) {
            try {
                db.updateUnresolvedTable(id, ex.toString());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void close() throws IOException {
        db.close();
    }
}