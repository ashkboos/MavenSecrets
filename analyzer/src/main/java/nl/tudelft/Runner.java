package nl.tudelft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.shared.invoker.MavenInvocationException;

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

    void run(Maven mvn, Collection<PackageId> packages) throws SQLException, IOException, PackageException {
        var fields = extractors.values().stream()
                .map(Extractor::fields)
                .flatMap(Arrays::stream)
                .toArray(Field[]::new);
        if (fields.length == 0)
            return;

        Instant fetchEnd = null;
        List<Object> values = null;
        for (var id : packages) {
            var start = Instant.now();
            try (var pkg = mvn.getPackage(id)) {
                fetchEnd = Instant.now();
                values = extractInto(mvn, pkg);
            } catch (PackageException e) {
                LOGGER.error(e);
                db.createUnresolvedTable(false);
                db.updateUnresolvedTable(id.toString(), e.getError());
                continue;
            } catch (MavenInvocationException e) {
                throw new RuntimeException(e);
            }

            db.update(id, fields, values.toArray(), true);
            var time = Duration.between(start, Instant.now());
            var fetchTime = Duration.between(start, fetchEnd);
            LOGGER.trace("processed " + id + " in " + time.toMillis() + " ms (fetch " + fetchTime.toMillis() + " ms)");
        }
    }

    private List<Object> extractInto(Maven mvn, Package pkg) throws IOException, SQLException, PackageException, MavenInvocationException {
        var list = new LinkedList<>();
        for (var extractor : extractors.values()) {

            var result = extractor.extract(mvn, pkg, db);
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