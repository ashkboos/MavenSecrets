package nl.tudelft;

import java.io.IOException;

public class App {
    public static void main(String[] args) throws IOException {
        if (args.length != 1)
            throw new RuntimeException("expected a single package id");

        var id = PackageId.tryParse(args[0]);
        if (id.isEmpty())
            throw new RuntimeException("invalid package id");

        var packages = new PackageId[]{
                id.get()
        };

        var builder = extractors(new AnalyzerBuilder());
        var maven = new Maven();
        var db = new Database();
        try (var analyzer = builder.build(db)) {
            analyzer.run(maven, packages);
        }
    }

    private static AnalyzerBuilder extractors(AnalyzerBuilder builder) {
        return builder;
    }

    private static Database openDatabase() {
        return new Database();
    }
}
