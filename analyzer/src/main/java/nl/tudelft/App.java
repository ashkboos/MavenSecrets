package nl.tudelft;

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
import java.util.Objects;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import io.github.cdimascio.dotenv.Dotenv;
import nl.tudelft.mavensecrets.Config;
import nl.tudelft.mavensecrets.YamlConfig;
import nl.tudelft.mavensecrets.resolver.DefaultResolver;

public class App {
    private static final Logger LOGGER = LogManager.getLogger(App.class);
    static String[] args;

    public static void main(String[] args) throws IOException, SQLException, PackageException {
        // Config
        LOGGER.info("Loading configuration");
        Config config = loadConfiguration();
        LOGGER.info("Extractors: " + config.getExtractors());

        App.args = args;
        
        long startTime = System.currentTimeMillis();
        var db = openDatabase();
        runIndexerReader(args, db);
        var packages = db.getPackageIds();

        if (packages.isEmpty()) {
            LOGGER.info("no packages, nothing to do");
            return;
        } else
            LOGGER.info("found " + packages.size() + " packages");

        var resolver = new DefaultResolver();
        var builder = extractors(config, new RunnerBuilder());
        var maven = new Maven(resolver);

        try (var runner = builder.build(db)) {
            runner.run(maven, packages);
        }
        long endTime = System.currentTimeMillis();
        long elapsedTime = endTime - startTime;

        LOGGER.info("Elapsed time: " + elapsedTime + " milliseconds");
    }

    private static void runIndexerReader(String[] args, Database db) throws IOException, SQLException {
        if(args.length > 1 && args[0].equals("index")) {
            String indexFile = args[1];
            String url = "https://repo.maven.apache.org/maven2/.index/" + indexFile;
            Path path = Paths.get(indexFile);

            // Check if the file exists
            if (!Files.exists(path)) {
                try {
                    // Download the file
                    URL fileUrl = new URL(url);
                    Files.copy(fileUrl.openStream(), path);
                    LOGGER.info("Successfully downloaded file");
                } catch (IOException e) {
                    LOGGER.error(e);
                }
            } else {
                LOGGER.info("Index file already exists");
            }
            IndexerReader ir = new IndexerReader(db);
            ir.indexerReader(indexFile);
        }
    }

    private static RunnerBuilder extractors(Config config, RunnerBuilder builder) {
        Objects.requireNonNull(config);
        Objects.requireNonNull(builder);

        // Should not be an issue since RunnerBuilder is mutable
        config.getExtractors().forEach(builder::addExtractor);

        return builder;
    }

    private static Database openDatabase() throws SQLException {
        var dotenv = Dotenv.configure()
                .ignoreIfMissing()
                .ignoreIfMalformed()
                .load();

        var host = dotenv.get("DB_HOST");
        var port = dotenv.get("DB_PORT");
        var name = dotenv.get("DB_NAME");
        var user = dotenv.get("DB_USER");
        var pass = dotenv.get("DB_PASS");
        if (host != null && name != null && user != null)
            return Database.connect("jdbc:postgresql://" + host + ":" + (port == null ? "5432" : port) + "/" + name, user, pass);
        else
            return Database.connect("jdbc:postgresql://localhost:5432/postgres", "postgres", "SuperSekretPassword");
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

    public static String[] getArgs() {
        return args;
    }
}
