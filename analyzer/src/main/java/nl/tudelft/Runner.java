package nl.tudelft;

import java.io.Closeable;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeMap;

public class Runner implements Closeable {
    private final Database db;
    private final Map<String, Extractor> extractors;

    Runner(Database db) {
        this.db = db;
        this.extractors = new TreeMap<>();
    }

    Runner addExtractor(String name, Extractor extractor) {
        if (this.extractors.containsKey(name))
            throw new IllegalArgumentException("extractor `" + name + "` already added");

        db.updateSchema(extractor.fields());
        extractors.put(name, extractor);

        return this;
    }

    void clear(PackageId[] packages) {}

    void run(Maven mvn, PackageId[] packages) {
        var fields = extractors.values().stream().flatMap(i -> Arrays.stream(i.fields())).toArray(Field[]::new);
        if (fields.length == 0)
            return;

        var values = new Object[fields.length];
        for (var id : packages) {
            try (var pkg = mvn.get(id)) {
                extractInto(mvn, pkg, values);
            }

            db.update(id, fields, values);
        }
    }

    private void extractInto(Maven mvn, Package pkg, Object[] values) {
        var offset = 0;
        for (var pair : extractors.entrySet()) {
            var name = pair.getKey();
            var extractor = pair.getValue();

            var result = extractor.extract(mvn, pkg);
            if (result.length != extractor.fields().length)
                throw new RuntimeException("extractor `" + name + "` returned unexpected number of values");

            for (var value : values)
                values[offset++] = value;
        }
    }

    @Override
    public void close() throws IOException {
        db.close();
    }
}