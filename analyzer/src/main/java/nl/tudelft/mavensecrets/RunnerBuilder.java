package nl.tudelft.mavensecrets;

import nl.tudelft.mavensecrets.extractors.Extractor;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class RunnerBuilder {

    private final Map<Class<?>, Extractor> extractors = new HashMap<>();

    RunnerBuilder addExtractor(Extractor extractor) {
        Objects.requireNonNull(extractor);

        extractors.putIfAbsent(extractor.getClass(), extractor);
        return this;
    }

    Runner build(Database db) throws SQLException {
        var analyzer =  new Runner(db);
        for (var entry : extractors.values())
            analyzer.addExtractor(entry);

        return analyzer;
    }
}
