package nl.tudelft;

import nl.tudelft.mavensecrets.resolver.DefaultResolver;
import nl.tudelft.mavensecrets.resolver.Resolver;

import java.io.File;
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.logging.Logger;

public class App {
    public static void main(String[] args) throws IOException, SQLException {
        var db = openDatabase();
        IndexerReader ir = new IndexerReader(db);
        ir.indexerReader();
        var packages = db.getPackageIds();

        Logger logger = Logger.getGlobal();
        File local = new File(System.getProperty("user.home") + "/.m2/repository");
        local.mkdir();
        Resolver resolver = new DefaultResolver(logger, local);

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
