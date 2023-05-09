package nl.tudelft;

import nl.tudelft.mavensecrets.extractors.*;
import nl.tudelft.mavensecrets.resolver.DefaultResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.SQLException;

public class App {
    private static final Logger LOGGER = LogManager.getLogger(App.class);

    public static void main(String[] args) throws IOException, SQLException, PackageException {
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
        var builder = extractors(new RunnerBuilder(), db);
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

    private static RunnerBuilder extractors(RunnerBuilder builder, Database db) {
        return builder
                .addExtractor("compiler", new CompilerConfigExtractor())
                .addExtractor("modules", new JavaModuleExtractor())
                .addExtractor("version", new JavaVersionExtractor())
                .addExtractor("parent", new ParentExtractor())
                .addExtractor("repo_urls", new ExtractorVC())
                .addExtractor("size", new JarSizeAndNumberOfFilesExtractor(db));

    }

    private static Database openDatabase() throws SQLException {
        return Database.connect("jdbc:postgresql://localhost:5432/postgres", "postgres", "SuperSekretPassword");
    }
}
