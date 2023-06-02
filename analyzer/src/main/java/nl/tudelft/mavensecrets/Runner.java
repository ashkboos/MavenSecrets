package nl.tudelft.mavensecrets;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ForkJoinPool;

import nl.tudelft.mavensecrets.extractors.Extractor;
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
        LOGGER.trace("Adding extractor '{}': {}", extractor, extractor.getClass());
        db.updateSchema(extractor.fields());
        extractors.putIfAbsent(extractor.getClass(), extractor);

        return this;
    }

    void run(Maven mvn, Collection<? extends ArtifactId> packages, int threads) throws InterruptedException {
        var fields = extractors.values()
                .stream()
                .map(Extractor::fields)
                .flatMap(Arrays::stream)
                .toArray(Field[]::new);
        if (fields.length == 0) {
            return;
        }

        var pool = new ForkJoinPool(threads);
        try {
            pool.submit(() -> packages.parallelStream().forEach(id -> execute(mvn, fields, id))).get();
        } catch (ExecutionException exception) {
            LOGGER.error("Task execution failed", exception.getCause());
        } finally {
            pool.shutdown();
        }
    }

    private void execute(Maven mvn, Field[] fields, ArtifactId id) {
        try (var artifact = mvn.getPackage(id)) {
            var offset = 0;
            Object[] values = new Object[fields.length];
            for (var extractor : extractors.values()) {
                Object[] result;
                try {
                    result = extractor.extract(mvn, artifact, id.extension(), db);
                } catch (Throwable exception) { // Generic catch just in case
                    LOGGER.warn("Extractor '{}' threw an unexpected exception", extractor, exception);
                    result = new Object[extractor.fields().length];
                }
                if (result.length != extractor.fields().length) {
                    LOGGER.warn("Extractor '{}' returned unexpected number of values", extractor);
                }

                System.arraycopy(result, 0, values, offset, result.length);
                offset += result.length;
            }

            db.update(id, fields, values);
        } catch (PackageException | IOException | SQLException exception) {
            LOGGER.warn("Could not extract fields of {}", id, exception);
            try {
                db.updateUnresolvedTable(id, exception.toString());
            } catch (SQLException exception1) {
                LOGGER.error("Could not write failure to databse", exception1);
            }
        }
    }

    @Override
    public void close() throws IOException {
        db.close();
    }
}