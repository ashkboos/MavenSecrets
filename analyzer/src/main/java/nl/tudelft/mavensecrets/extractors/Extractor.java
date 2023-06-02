package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.mavensecrets.Database;
import nl.tudelft.mavensecrets.Field;
import nl.tudelft.mavensecrets.Maven;
import nl.tudelft.mavensecrets.Package;

import java.io.IOException;
import java.sql.SQLException;

public interface Extractor {
    Field[] fields();
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) throws IOException, SQLException;
}