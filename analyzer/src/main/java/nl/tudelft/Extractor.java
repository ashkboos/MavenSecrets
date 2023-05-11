package nl.tudelft;

import java.io.IOException;
import java.sql.SQLException;

public interface Extractor {
    Field[] fields();
    Object[] extract(Maven mvn, Package pkg, Database db) throws IOException, SQLException;
}