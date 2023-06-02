package nl.tudelft.mavensecrets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.tudelft.mavensecrets.config.Config;
import nl.tudelft.mavensecrets.config.YamlConfig;
import nl.tudelft.mavensecrets.resolver.DefaultResolver;
import nl.tudelft.mavensecrets.selection.PackageSelector;
import nl.tudelft.mavensecrets.selection.StratifiedSampleSelector;

public class App {

    private static final Logger LOGGER = LogManager.getLogger(App.class);
    private static final int PAGE_SIZE = 512;

    public static void main(String[] args) {
        // Config
        LOGGER.info("Loading configuration");
        Config config;
        try {
            config = loadConfiguration();
        } catch (IOException exception) {
            LOGGER.error("Could not load configuration", exception);
            return;
        }
        LOGGER.info("Extractors: {}", config.getExtractors());
        LOGGER.info("Threadpool size: {}", config.getThreads());
        LOGGER.info("Database configuration: {}", config.getDatabaseConfig());
        LOGGER.info("Index files: {}", config.getIndexFiles());
        LOGGER.info("Local repository: {}", config.getLocalRepository().getAbsolutePath());
        LOGGER.info("Seed: {}", config.getSeed());
        LOGGER.info("Sample percent: {}%", config.getSamplePercent());

        long startTime = System.currentTimeMillis();

        // Databse
        Database db;
        try {
            db = openDatabase(config.getDatabaseConfig());
            db.createUnresolvedTable();
            db.createNewExtensionTable();
        } catch (SQLException exception) {
            LOGGER.error("Could not open database", exception);
            return;
        }

        // Indexer
        try {
            runIndexerReader(config.getIndexFiles(), args, db);
        } catch (SQLException exception) {
            LOGGER.error("Could not insert indices into database", exception);
            return;
        }

        PackageSelector selector = new StratifiedSampleSelector(db, config.getSeed(), config.getSamplePercent());
        LOGGER.info("Package selector: {}", selector);

        var resolver = new DefaultResolver(config.getLocalRepository());
        var builder = extractors(config, new RunnerBuilder());
        var maven = new Maven(resolver);

        try (var runner = builder.build(db)) {
            Collection<? extends ArtifactId> artifacts;
            for (int i = 0; (artifacts = selector.getArtifacts(i, PAGE_SIZE)).size() > 0; i++) {
                runner.run(maven, artifacts, config.getThreads());
            }
        } catch (IOException | SQLException exception) {
            LOGGER.error("Runner failed with exception", exception);
        } catch (InterruptedException exception) {
            LOGGER.warn("Run interrupted");
            Thread.currentThread().interrupt();
        }

        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        LOGGER.info("Elapsed time: {}ms", elapsedTime);
    }

    private static void runIndexerReader(Collection<? extends String> indices, String[] args, Database db) throws SQLException {
        Objects.requireNonNull(indices);
        Objects.requireNonNull(args);
        Objects.requireNonNull(db);

        LOGGER.trace("Fetching indices...");

        Collection<Path> paths = new ArrayList<>();
        for (String index : indices) {
            Path path;
            try {
                path = getIndex(index);
            } catch (IOException exception) {
                LOGGER.warn("Could not fetch index {}", index, exception);
                continue;
            }
            paths.add(path);
        }

        // Backwards compatibility
        if (args.length > 1 && args[0].equals("index")) {
            LOGGER.warn("Found 'index' command line argument; use the configuration");
            String indexFile = args[1];
            try {
                Path path = getIndex(indexFile);
                paths.add(path);
            } catch (IOException exception) {
                LOGGER.warn("Could not fetch index {}", indexFile, exception);
            }
        }

        IndexerReader ir = new IndexerReader(db);
        for (Path path : paths) {
            LOGGER.trace("Reading index {}", path.getFileName());
            try {
                ir.indexerReader(path.toFile());
            } catch (SQLException exception) {
                throw exception;
            } catch (Throwable exception) { // Generic catch because malformed data may not throw IOExceptions
                LOGGER.warn("Could not read index {}", path.getFileName(), exception);
            }
        }
    }

    /**
     * Get the path to a given index file, downloading it if needed.
     *
     * @param index Index file name.
     * @return The path.
     * @throws IOException If an I/O error occurs.
     */
    private static Path getIndex(String index) throws IOException {
        Objects.requireNonNull(index);

        // Note: Index file name is not sanitized

        LOGGER.trace("Fetching index {}", index);

        // Directory
        Path dir = Paths.get("index-files");
        if (!Files.exists(dir)) {
            Files.createDirectory(dir);
        }

        Path legacyPath = Paths.get(index);
        Path path = Paths.get("index-files", index);

        // Legacy support
        if (Files.exists(legacyPath) && Files.isRegularFile(legacyPath) && !Files.exists(path)) {
            LOGGER.trace("Found index file for {} in legacy location, moving...", index);
            Files.move(legacyPath, path);
        }

        if (!Files.exists(path)) {
            LOGGER.trace("No index file found for {}, downloading...", index);
            URL fileUrl = new URL("https://repo.maven.apache.org/maven2/.index/" + index);
            Files.copy(fileUrl.openStream(), path);
        }

        return path;
    }

    private static RunnerBuilder extractors(Config config, RunnerBuilder builder) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(builder);

        // Should not be an issue since RunnerBuilder is mutable
        config.getExtractors().forEach(builder::addExtractor);

        return builder;
    }

    private static Database openDatabase(Config.Database config) throws SQLException {
        Objects.requireNonNull(config);

        // Not sanitized
        return Database.connect("jdbc:postgresql://" + config.getHostname() + ':' + config.getPort() + '/' + config.getName(), config.getUsername(), config.getPassword());
    }

    /**
     * Load the program's configuration.
     *
     * @return The configuration.
     * @throws IOException If an I/O error occurs.
     */
    private static Config loadConfiguration() throws IOException {
        File dir = new File(".");
        File file = new File(dir, "config.yml");

        // Copy defaults
        if (!file.exists()) {
            dir.mkdirs();
            try (InputStream in = App.class.getClassLoader().getResourceAsStream("config.yml")) {
                if (in == null) {
                    throw new RuntimeException("No config.yml resource present");
                }
                try (OutputStream out = new FileOutputStream(file)) {
                    byte[] buf = new byte[1 << 10];
                    int len;
                    while ((len = in.read(buf)) != -1) {
                        out.write(buf, 0, len);
                    }
                }
            }
        }

        return YamlConfig.fromFile(file);
    }
}
