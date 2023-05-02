package nl.tudelft;

public interface Extractor {
    Field[] fields();
    Object[] extract(Maven mvn, Package pkg);
}