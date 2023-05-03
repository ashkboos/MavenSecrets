package nl.tudelft;

import java.io.Closeable;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

public class Runner implements Closeable {
    private final Database db;
    private final Map<String, Extractor> extractors;

    Runner(Database db) {
        this.db = db;
        this.extractors = new TreeMap<>();
    }

    Runner addExtractor(String name, Extractor extractor) throws SQLException {
        if (this.extractors.containsKey(name))
            throw new IllegalArgumentException("extractor `" + name + "` already added");

        db.updateSchema(extractor.fields());
        extractors.put(name, extractor);

        return this;
    }

    void clear(PackageId[] packages) {}

    void run(Maven mvn, PackageId[] packages) throws SQLException, IOException {
        var fields = extractors.values().stream().flatMap(i -> Arrays.stream(i.fields())).toArray(Field[]::new);
        if (fields.length == 0)
            return;

        List<Object> values = null;
        for (var id : packages) {
            try (var pkg = mvn.getPackage(id)) {
                values = extractInto(mvn, pkg);
            }

            db.update(id, fields, values.toArray());
        }
    }

    private List<Object> extractInto(Maven mvn, Package pkg) {
        var list = new LinkedList<>();
        for (var pair : extractors.entrySet()) {
            var name = pair.getKey();
            var extractor = pair.getValue();

            var result = extractor.extract(mvn, pkg);
            if (result.length != extractor.fields().length)
                throw new RuntimeException("extractor `" + name + "` returned unexpected number of values");

            for (var value : result)
                list.add(value);
        }
        return list;
    }

    @Override
    public void close() throws IOException {
        db.close();
    }
}