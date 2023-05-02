package nl.tudelft.mavensecrets.resolver;

import java.util.Objects;
import java.util.logging.Logger;

import org.eclipse.aether.AbstractRepositoryListener;

public class DefaultRepositoryListener extends AbstractRepositoryListener {

    @SuppressWarnings("unused")
    private final Logger logger;

    public DefaultRepositoryListener(Logger logger) {
        this.logger = Objects.requireNonNull(logger);
    }
}
