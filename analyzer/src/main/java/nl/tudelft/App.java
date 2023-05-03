package nl.tudelft;

import nl.tudelft.mavensecrets.resolver.DefaultResolver;

import java.io.IOException;
import java.sql.SQLException;

public class App {
    public static void main(String[] args) throws IOException, SQLException, PackageException {
        if (args.length != 1)
            throw new RuntimeException("expected a single package id");

        var id = PackageId.tryParse(args[0]);
        if (id.isEmpty())
            throw new RuntimeException("invalid package id");

        var packages = new PackageId[]{
                id.get()
        };

        var resolver = new DefaultResolver();
        var builder = extractors(new RunnerBuilder());
        var maven = new Maven(resolver);
        var db = openDatabase();
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
