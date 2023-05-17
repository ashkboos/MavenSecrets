package nl.tudelft.mavensecrets;

import nl.tudelft.Extractor;

import java.util.Collection;

public interface Config {
    Collection<? extends Extractor> getExtractors();
    int getThreads();
}
