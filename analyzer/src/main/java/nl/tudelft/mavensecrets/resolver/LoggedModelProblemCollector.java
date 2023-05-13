package nl.tudelft.mavensecrets.resolver;

import org.apache.logging.log4j.Logger;
import org.apache.maven.model.building.ModelProblemCollector;
import org.apache.maven.model.building.ModelProblemCollectorRequest;

public class LoggedModelProblemCollector implements ModelProblemCollector {
    private final Logger log;

    public LoggedModelProblemCollector(Logger log) {
        this.log = log;
    }

    @Override
    public void add(ModelProblemCollectorRequest modelProblemCollectorRequest) {
        log.error(modelProblemCollectorRequest.getException());
    }
}
