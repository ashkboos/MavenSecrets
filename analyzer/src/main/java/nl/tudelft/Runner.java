package nl.tudelft;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import nl.tudelft.mavensecrets.Config;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Runner implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger(Runner.class);
    private final Database db;
    private final Map<Class<?>, Extractor> extractors = new HashMap<>();

    // Set this to force all subsequently started threads to skip processing
    private AtomicBoolean cancelled = new AtomicBoolean(false);

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

    void run(Maven mvn, List<PackageId> packages, Map<PackageId, String> packagingTypes, Config config) {
        var fields = extractors.values().stream()
                .map(Extractor::fields)
                .flatMap(Arrays::stream)
                .toArray(Field[]::new);
        if (fields.length == 0)
            return;
        processPackages(packages, fields, mvn, packagingTypes, config);
    }

    private void processPackages(Collection<PackageId> packages, Field[] fields, Maven mvn, Map<PackageId, String> packagingTypes, Config config) {
        LOGGER.debug(config.getThreads());
        ExecutorService executor = Executors.newFixedThreadPool(config.getThreads());

        // We manually create then manage the future inside the task
        // since executor.submit() only returns a Future, but we need
        // a CompletableFuture to be able to use CompletableFutures.allOf()
        List<Future<Void>> futures = new ArrayList<>();
        for (PackageId id : packages) {
            String pkgType = packagingTypes.get(id);
            CompletableFuture<Void> future = new CompletableFuture<>();
            executor.submit(new ProcessPackageTask(id, fields, mvn, future, pkgType));
            futures.add(future);
        }

        // Combine all CompletableFutures into a single CompletableFuture then .get() to wait for all threads
        // to complete (successfully or exceptionally)
        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.error(e);
        } catch (ExecutionException e) {
            // Whenever a package exception occurs at any point during execution,
            // it will be logged here, so ignore it.
            LOGGER.error(e);
        }
        finally {
            executor.shutdown();
        }
    }

    private List<Object> extractInto(Maven mvn, Package pkg, String pkgType, Database db) throws IOException, SQLException {
        var list = new LinkedList<>();
        for (var extractor : extractors.values()) {

            var result = extractor.extract(mvn, pkg, pkgType, db);
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

    private class ProcessPackageTask implements Callable<Void> {

        private final PackageId id;
        private final Field[] fields;
        private final Maven mvn;
        private final String pkgType;
        private final CompletableFuture<Void> future;

        public ProcessPackageTask(PackageId id, Field[] fields, Maven mvn, CompletableFuture<Void> future, String pkgType) {
            this.id = id;
            this.fields = fields;
            this.mvn = mvn;
            this.future = future;
            this.pkgType = pkgType;
        }

        @Override
        public Void call() throws SQLException {
            if (cancelled.get()) {
                LOGGER.error("SQL Exception encountered in another thread. Skipping package " + id);
                future.completeExceptionally(new SQLException("Lost connection to DB!"));
                return null;
            }

            Instant fetchEnd;
            List<Object> values;

            var start = Instant.now();
            try (var pkg = mvn.getPackage(id, pkgType)) {
                fetchEnd = Instant.now();
                values = extractInto(mvn, pkg, pkgType, db);
            } catch (PackageException | IOException  | SQLException e) {
                LOGGER.error(e);
                future.complete(null);
                // TODO Put this package in the unresolved table
                db.createUnresolvedTable(false);
                db.updateUnresolvedTable(id.toString(), e.getMessage());
                return null;
            }

            try {
                db.update(id, fields, values.toArray());
            } catch (SQLException e) {
                LOGGER.error(e);
                cancelled.set(true);
                future.completeExceptionally(e);
                return null;
            }

            var time = Duration.between(start, Instant.now());
            var fetchTime = Duration.between(start, fetchEnd);
            LOGGER.trace("processed " + id + " in " + time.toMillis() + " ms (fetch " + fetchTime.toMillis() + " ms)");

            future.complete(null);
            return null;
        }
    }
}