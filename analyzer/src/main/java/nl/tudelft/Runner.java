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

    void clear(PackageId[] packages) {}

    void run(Maven mvn, List<PackageId> packages, List<String> packagingTypes) throws SQLException, IOException, PackageException {
        var fields = extractors.values().stream()
                .map(Extractor::fields)
                .flatMap(Arrays::stream)
                .toArray(Field[]::new);
        if (fields.length == 0)
            return;

        Instant fetchEnd = null;
        List<Object> values = null;
        int count = 0;
        int check = 0;
        for (var id : packages) {
            var start = Instant.now();
            String pkgType = packagingTypes.get(count);
            count ++;
            try (var pkg = mvn.getPackage(id, pkgType)) {
                fetchEnd = Instant.now();
                values = extractInto(mvn, pkg, pkgType);
            } catch (PackageException e) {
                LOGGER.error(e);
                continue;
            }
            check++;
            db.update(id, fields, values.toArray());
            var time = Duration.between(start, Instant.now());
            var fetchTime = Duration.between(start, fetchEnd);
            LOGGER.trace("processed " + id + " in " + time.toMillis() + " ms (fetch " + fetchTime.toMillis() + " ms)");
        }
    }

    private List<Object> extractInto(Maven mvn, Package pkg, String pkgType) throws IOException {
        var list = new LinkedList<>();
        for (var extractor : extractors.values()) {

            var result = extractor.extract(mvn, pkg, pkgType);
            if (result.length != extractor.fields().length)
                throw new RuntimeException("Extractor '" + extractor + "' returned unexpected number of values");

            list.addAll(Arrays.asList(result));
        }
        return list;
    }

    @Override
    public void close() throws IOException {
        db.close();
    }
}