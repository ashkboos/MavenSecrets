package nl.tudelft;

import java.io.IOException;

public interface Extractor {
    Field[] fields();
    Object[] extract(Maven mvn, Package pkg) throws IOException;
}