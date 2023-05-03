package nl.tudelft;

import nl.tudelft.mavensecrets.resolver.DefaultResolver;

import java.io.IOException;
import java.sql.SQLException;

public class App {
    public static void main(String[] args) throws IOException, SQLException, PackageException {
        var db = openDatabase();
        var packages = db.getPackageIds();

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
