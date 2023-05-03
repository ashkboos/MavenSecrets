package nl.tudelft;

import nl.tudelft.mavensecrets.resolver.DefaultResolver;
import org.apache.logging.log4j.LogManager;

import java.io.IOException;
import java.sql.SQLException;

public class App {
    public static void main(String[] args) throws IOException, SQLException, PackageException {
        var log = LogManager.getLogger(App.class);
        var db = openDatabase();
        var packages = db.getPackageIds();

        if (packages.isEmpty()) {
            log.info("no packages, nothing to do");
            return;
        } else
            log.info("found " + packages.size() + " packages");

        var resolver = new DefaultResolver();
        var builder = extractors(new RunnerBuilder());
        var maven = new Maven(resolver);
        try (var runner = builder.build(db)) {
            runner.run(maven, packages);
        }
    }

    private static RunnerBuilder extractors(RunnerBuilder builder) {
        return builder.addExtractor("favoriteName", new DemoExtractor());
    }

    private static Database openDatabase() throws SQLException {
        return Database.connect("jdbc:postgresql://localhost:5432/postgres", "postgres", "SuperSekretPassword");
    }
}
