package nl.tudelft;

import nl.tudelft.mavensecrets.resolver.DefaultResolver;
import nl.tudelft.mavensecrets.resolver.Resolver;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

public class App {
    public static void main(String[] args) throws IOException, SQLException {
        if (args.length != 1)
            throw new RuntimeException("expected a single package id");

        var id = PackageId.tryParse(args[0]);
        if (id.isEmpty())
            throw new RuntimeException("invalid package id");

        var packages = new PackageId[]{
                id.get()
        };

        Logger logger = Logger.getGlobal();
        File local = new File(System.getProperty("user.home") + "/.m2/repository");
        local.mkdir();
        Resolver resolver = new DefaultResolver(logger, local);

        var builder = extractors(new RunnerBuilder());
        var maven = new Maven(resolver);
        var db = openDatabase();
        try (var runner = builder.build(db)) {
            runner.run(maven, packages);
        }
    }

    private static RunnerBuilder extractors(RunnerBuilder builder) {
        return builder.addExtractor("favoriteName", new DemoExtractor()).addExtractor("vc", new ExtractorVC());
    }

    private static Database openDatabase() throws SQLException {
        return Database.connect("jdbc:postgresql://localhost:5432/postgres", "postgres", "SuperSekretPassword");
    }
}
