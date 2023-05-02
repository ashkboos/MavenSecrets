package nl.tudelft.mavensecrets.resolver;

import java.io.File;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;

import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.connector.basic.BasicRepositoryConnectorFactory;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.repository.RepositoryPolicy;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.spi.connector.RepositoryConnectorFactory;
import org.eclipse.aether.spi.connector.transport.TransporterFactory;
import org.eclipse.aether.transport.file.FileTransporterFactory;
import org.eclipse.aether.transport.http.HttpTransporterFactory;
import org.eclipse.aether.util.artifact.SubArtifact;

/**
 * A default artefact {@link Resolver}.
 * This implementation pulls from Maven Central.
 */
public class DefaultResolver implements Resolver {

    private static final RemoteRepository MAVEN_CENTRAL = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();
    private static final Pattern COMPONENT_PATTERN = Pattern.compile("^[^: ]+$");

    private final Logger logger;
    private final RepositorySystemSession session;
    private RepositorySystem repository = null;

    /**
     * Create a resolver instance.
     *
     * @param logger Logger.
     * @param local Local repository directory.
     */
    public DefaultResolver(final Logger logger, File local) {
        Objects.requireNonNull(logger);
        Objects.requireNonNull(local);
        this.logger = logger;
        this.repository = createRepositorySystem();
        this.session = createSession(new LocalRepository(local));
    }

    @Override
    public Artifact createArtifact(String groupId, String artifactId, String version) {
        Objects.requireNonNull(groupId);
        Objects.requireNonNull(artifactId);
        Objects.requireNonNull(version);
 
        if (!COMPONENT_PATTERN.matcher(groupId).matches()) {
            throw new IllegalArgumentException("Invalid group id: " + groupId);
        }
        if (!COMPONENT_PATTERN.matcher(artifactId).matches()) {
            throw new IllegalArgumentException("Invalid artifact id: " + artifactId);
        }
        if (!COMPONENT_PATTERN.matcher(version).matches()) {
            throw new IllegalArgumentException("Invalid version: " + version);
        }

        return new DefaultArtifact(groupId, artifactId, null, version);
    }

    @Override
    public Artifact createArtifact(String coords) {
        Objects.requireNonNull(coords);

        return new DefaultArtifact(coords);
    }

    @Override
    public Optional<Artifact> resolve(Artifact artifact) {
        Objects.requireNonNull(artifact);

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(Collections.singletonList(MAVEN_CENTRAL));
        ArtifactResult result;
        try {
            result = repository.resolveArtifact(session, request);
        } catch (ArtifactResolutionException exception) {
            // FIXME log? This may be handled by the listeners already.
            return Optional.empty();
        }
        return Optional.ofNullable(result.getArtifact());
    }

    @Override
    public Optional<File> getPom(Artifact artifact) {
        Objects.requireNonNull(artifact);

        Artifact pom = new SubArtifact(artifact, null, "pom");
        return resolve(pom)
                .map(Artifact::getFile);
    }

    @Override
    public Optional<File> getArtifact(Artifact artifact) {
        Objects.requireNonNull(artifact);

        return resolve(artifact)
                .map(Artifact::getFile);
    }

    /**
     * Create a repository system.
     *
     * @return The repository system.
     */
    @SuppressWarnings("deprecation")
    private RepositorySystem createRepositorySystem() {
        // I have not found another way
        org.eclipse.aether.impl.DefaultServiceLocator locator = MavenRepositorySystemUtils.newServiceLocator();
        locator.addService(RepositoryConnectorFactory.class, BasicRepositoryConnectorFactory.class);
        locator.addService(TransporterFactory.class, FileTransporterFactory.class);
        locator.addService(TransporterFactory.class, HttpTransporterFactory.class);
        locator.setErrorHandler(new org.eclipse.aether.impl.DefaultServiceLocator.ErrorHandler() {
            @Override
            public void serviceCreationFailed(Class<?> type, Class<?> impl, Throwable exception) {
                logger.log(Level.SEVERE, "Service creation failed for " + type + " with implementation " + impl, exception);
            }
        });

        return locator.getService(RepositorySystem.class);
    }

    /**
     * Create a repository system session.
     *
     * @param local The local repository.
     * @return The system session.
     */
    private RepositorySystemSession createSession(LocalRepository local) {
        Objects.requireNonNull(local);

        DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();
        session.setLocalRepositoryManager(repository.newLocalRepositoryManager(session, local));
        session.setChecksumPolicy(RepositoryPolicy.CHECKSUM_POLICY_FAIL);
        session.setTransferListener(new DefaultTransferListener(logger));
        session.setRepositoryListener(new DefaultRepositoryListener(logger));
        session.setReadOnly();

        return session;
    }
}
