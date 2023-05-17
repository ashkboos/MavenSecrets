package nl.tudelft.mavensecrets.resolver;

import org.apache.logging.log4j.Logger;
import org.eclipse.aether.AbstractRepositoryListener;

import java.util.Objects;

public class DefaultRepositoryListener extends AbstractRepositoryListener {
    @SuppressWarnings("unused")
    private final Logger log;

    public DefaultRepositoryListener(Logger log) {
        this.log = Objects.requireNonNull(log);
    }
}
