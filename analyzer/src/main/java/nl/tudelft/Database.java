package nl.tudelft;

import java.io.Closeable;

public class Database implements Closeable {
    void updateSchema(Field[] fields) {}

    void update(PackageId id, Field[] fields, Object[] values) {}

    @Override
    public void close() {}
}