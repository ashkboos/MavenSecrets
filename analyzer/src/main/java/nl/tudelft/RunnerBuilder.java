package nl.tudelft;

import java.util.*;

public class RunnerBuilder {
    private final Map<String, Extractor> extractors = new HashMap<>();
    private final Set<String> whitelist;

    public RunnerBuilder() {
        whitelist = new HashSet<>();
    }

    public RunnerBuilder(Collection<String> whitelist) {
        this.whitelist = new HashSet<>(whitelist);
    }

    RunnerBuilder addExtractor(String name, Extractor extractor) {
        if (this.extractors.containsKey(name))
            throw new IllegalArgumentException("extractor `" + name + "` already added");
        if (!whitelist.isEmpty() && !whitelist.contains(name))
            return this;

        extractors.put(name, extractor);
        return this;
    }

    Runner build(Database db) {
        var analyzer =  new Runner(db);
        for (var entry : extractors.entrySet())
            analyzer.addExtractor(entry.getKey(), entry.getValue());

        return analyzer;
    }
}
