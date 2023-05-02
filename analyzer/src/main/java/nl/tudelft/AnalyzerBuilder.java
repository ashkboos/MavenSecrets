package nl.tudelft;

import java.util.*;

public class AnalyzerBuilder {
    private final Map<String, Extractor> extractors = new HashMap<>();
    private final Set<String> whitelist;

    public AnalyzerBuilder() {
        whitelist = new HashSet<>();
    }

    public AnalyzerBuilder(Collection<String> whitelist) {
        this.whitelist = new HashSet<>(whitelist);
    }

    AnalyzerBuilder addExtractor(String name, Extractor extractor) {
        if (this.extractors.containsKey(name))
            throw new IllegalArgumentException("extractor `" + name + "` already added");
        if (!whitelist.isEmpty() && !whitelist.contains(name))
            return this;

        extractors.put(name, extractor);
        return this;
    }

    Analyzer build(Database db) {
        var analyzer =  new Analyzer(db);
        for (var entry : extractors.entrySet())
            analyzer.addExtractor(entry.getKey(), entry.getValue());

        return analyzer;
    }
}
