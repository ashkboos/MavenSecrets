package nl.tudelft;

import java.io.IOException;
import java.sql.SQLException;

public interface Extractor {
    Field[] fields();
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) throws IOException, SQLException;
}