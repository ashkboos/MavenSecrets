package nl.tudelft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

public class Runner implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger(Runner.class);
    private final Database db;
    private final Map<String, Extractor> extractors;

    Runner(Database db) {
        this.db = db;
        this.extractors = new TreeMap<>();
    }

    Runner addExtractor(String name, Extractor extractor) throws SQLException {
        if (this.extractors.containsKey(name))
            throw new IllegalArgumentException("extractor `" + name + "` already added");

        LOGGER.trace("adding extractor `" + name + "`: " + extractor.getClass().getName());
        db.updateSchema(extractor.fields());
        extractors.put(name, extractor);

        return this;
    }

    void clear(PackageId[] packages) {}

    void run(Maven mvn, Collection<PackageId> packages) throws SQLException, IOException, PackageException {
        var fields = extractors.values().stream().flatMap(i -> Arrays.stream(i.fields())).toArray(Field[]::new);
        if (fields.length == 0)
            return;

        List<Object> values = null;
        for (var id : packages) {
            var start = Instant.now();
            Instant fetchEnd;
            try (var pkg = mvn.getPackage(id)) {
                fetchEnd = Instant.now();
                values = extractInto(mvn, pkg);
            } catch (PackageException e) {
                LOGGER.error(e);
                continue;
            }

            var dbStart = Instant.now();
            db.update(id, fields, values.toArray());
            var end = Instant.now();
            var time = Duration.between(start, end);
            var fetchTime = Duration.between(start, fetchEnd);
            var extractTime = Duration.between(fetchEnd, dbStart);
            var dbTime = Duration.between(dbStart, end);
            LOGGER.info("processed " + id + " in " + time.toMillis() + " ms (fetch: " + fetchTime.toMillis() + " ms, extract: " + extractTime.toMillis() + " ms, db: " + dbTime.toMillis() + " ms)");
        }
    }

    private List<Object> extractInto(Maven mvn, Package pkg) throws IOException {
        var list = new LinkedList<>();
        for (var pair : extractors.entrySet()) {
            var name = pair.getKey();
            var extractor = pair.getValue();

            var result = extractor.extract(mvn, pkg);
            if (result.length != extractor.fields().length)
                throw new RuntimeException("extractor `" + name + "` returned unexpected number of values");

            list.addAll(Arrays.asList(result));
        }
        return list;
    }

    @Override
    public void close() throws IOException {
        db.close();
    }
}