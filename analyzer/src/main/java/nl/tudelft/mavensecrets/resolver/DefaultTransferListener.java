package nl.tudelft.mavensecrets.resolver;

import java.util.Objects;
import org.apache.logging.log4j.Logger;
import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;

public class DefaultTransferListener extends AbstractTransferListener {
    private final Logger log;

    public DefaultTransferListener(Logger log) {
        this.log = Objects.requireNonNull(log);
    }

    @Override
    public void transferInitiated(TransferEvent event) throws TransferCancelledException {
        //log.trace("GET {}{}", event.getResource().getRepositoryUrl(), event.getResource().getResourceName());
    }

    @Override
    public void transferFailed(TransferEvent event) {
        //log.error("Failed to fetch {}", event.getResource().getResourceName(), event.getException());
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        //log.debug("Fetched {}", event.getResource().getFile());
    }
}
