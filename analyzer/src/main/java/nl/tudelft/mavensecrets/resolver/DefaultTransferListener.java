package nl.tudelft.mavensecrets.resolver;

import java.util.Objects;
import java.util.logging.Logger;

import org.eclipse.aether.transfer.AbstractTransferListener;

public class DefaultTransferListener extends AbstractTransferListener {

    @SuppressWarnings("unused")
    private final Logger logger;

    public DefaultTransferListener(Logger logger) {
        this.logger = Objects.requireNonNull(logger);
    }
}
