package nl.tudelft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
        processPackages(packages, fields, mvn);
    }

    private void processPackages(Collection<PackageId> packages, Field[] fields, Maven mvn){
        ExecutorService executor = Executors.newFixedThreadPool(16);

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (PackageId id : packages) {
            CompletableFuture<Void> future = new CompletableFuture<>();
            executor.submit(new ProcessPackageTask(id, fields, mvn, future));
            futures.add(future);
        }
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        executor.shutdown();
    }

    private class ProcessPackageTask implements Runnable {

        private final PackageId id;
        private final Field[] fields;
        private final Maven mvn;
        private final CompletableFuture<Void> future;

        public ProcessPackageTask(PackageId id, Field[] fields, Maven mvn, CompletableFuture<Void> future) {
            this.id = id;
            this.fields = fields;
            this.mvn = mvn;
            this.future = future;
        }

        @Override
        public void run() {
            Instant fetchEnd;
            List<Object> values;

            var start = Instant.now();
            try (var pkg = mvn.getPackage(id)) {
                fetchEnd = Instant.now();
                values = extractInto(mvn, pkg);
            } catch (PackageException | IOException e) {
                LOGGER.error(e);
                future.complete(null);
                return;
            }

            try {
                db.update(id, fields, values.toArray());
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
            var time = Duration.between(start, Instant.now());
            var fetchTime = Duration.between(start, fetchEnd);
            LOGGER.trace("processed " + id + " in " + time.toMillis() + " ms (fetch " + fetchTime.toMillis() + " ms)");

            future.complete(null);
        }
    }

    private List<Object> extractInto(Maven mvn, Package pkg) throws IOException {
        var list = new LinkedList<>();
        for (var extractor : extractors.values()) {

            var result = extractor.extract(mvn, pkg);
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