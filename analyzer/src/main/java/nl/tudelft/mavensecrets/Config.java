package nl.tudelft.mavensecrets;

import java.util.Collection;

import nl.tudelft.Extractor;

public interface Config {
    Collection<? extends Extractor> getExtractors();
    int getThreads();
}
