package nl.tudelft;

import nl.tudelft.mavensecrets.extractors.CompilerConfigExtractor;
import nl.tudelft.mavensecrets.extractors.JavaModuleExtractor;
import nl.tudelft.mavensecrets.extractors.JavaVersionExtractor;
import nl.tudelft.mavensecrets.extractors.ParentExtractor;
import nl.tudelft.mavensecrets.resolver.DefaultResolver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.sql.SQLException;

public class App {
    private static final Logger LOGGER = LogManager.getLogger(App.class);

    public static void main(String[] args) throws IOException, SQLException, PackageException {
        var db = openDatabase();
        IndexerReader ir = new IndexerReader(db);
        ir.indexerReader();
        var packages = db.getPackageIds();

        if (packages.isEmpty()) {
            LOGGER.info("no packages, nothing to do");
            return;
        } else
            LOGGER.info("found " + packages.size() + " packages");

        var resolver = new DefaultResolver();
        var builder = extractors(new RunnerBuilder());
        var maven = new Maven(resolver);

        try (var runner = builder.build(db)) {
            runner.run(maven, packages);
        }
    }

    private static RunnerBuilder extractors(RunnerBuilder builder) {
        return builder
                .addExtractor("compiler", new CompilerConfigExtractor())
                .addExtractor("modules", new JavaModuleExtractor())
                .addExtractor("version", new JavaVersionExtractor())
                .addExtractor("parent", new ParentExtractor())
                .addExtractor("repo_urls", new ExtractorVC());
    }

    private static Database openDatabase() throws SQLException {
        return Database.connect("jdbc:postgresql://localhost:5432/postgres", "postgres", "SuperSekretPassword");
    }
}
