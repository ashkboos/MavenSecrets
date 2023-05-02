package nl.tudelft;

public class Analyzer {
    Analyzer(Database db) {}

    Analyzer addExtractor(String name, Extractor extractor) {
        throw new RuntimeException("TODO");
    }

    void clear(PackageId[] packages) {}

    void run(Maven mvn, PackageId[] packages) {}
}