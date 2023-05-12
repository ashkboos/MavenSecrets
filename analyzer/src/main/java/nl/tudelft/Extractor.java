package nl.tudelft;

import org.apache.maven.shared.invoker.MavenInvocationException;

import java.io.IOException;
import java.sql.SQLException;

public interface Extractor {
    Field[] fields();
    Object[] extract(Maven mvn, Package pkg, Database db) throws IOException, SQLException, MavenInvocationException;
}