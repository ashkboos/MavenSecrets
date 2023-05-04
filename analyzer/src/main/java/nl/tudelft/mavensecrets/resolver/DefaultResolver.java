package nl.tudelft.mavensecrets.resolver;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Objects;
import java.util.Properties;
import java.util.regex.Pattern;

import nl.tudelft.PackageId;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.interpolation.DefaultModelVersionProcessor;
import org.apache.maven.model.interpolation.StringSearchModelInterpolator;
import org.apache.maven.model.interpolation.StringVisitorModelInterpolator;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.codehaus.plexus.interpolation.StringSearchInterpolator;
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
    private static final Logger LOGGER = LogManager.getLogger(DefaultResolver.class);
    private static final RemoteRepository MAVEN_CENTRAL = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();
    private static final Pattern COMPONENT_PATTERN = Pattern.compile("^[^: ]+$");

    private final RepositorySystemSession session;
    private final ModelReader modelReader;
    private RepositorySystem repository = null;

    public DefaultResolver() {
        this(new File(System.getProperty("user.home"),".m2/repository"));
    }

    /**
     * Create a resolver instance.
     *
     * @param local Local repository directory.
     */
    public DefaultResolver(File local) {
        Objects.requireNonNull(local);
        this.repository = createRepositorySystem();
        this.session = createSession(new LocalRepository(local));
        this.modelReader = new DefaultModelReader();
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
    public Artifact resolve(Artifact artifact) throws ArtifactResolutionException {
        Objects.requireNonNull(artifact);

        ArtifactRequest request = new ArtifactRequest();
        request.setArtifact(artifact);
        request.setRepositories(Collections.singletonList(MAVEN_CENTRAL));
        ArtifactResult result;
        try {
            result = repository.resolveArtifact(session, request);
        } catch (ArtifactResolutionException ex) {
            LOGGER.error("failed to resolve artifact " + artifact.getGroupId() + ":" + artifact.getArtifactId() + ":" + artifact.getExtension() + ":" + artifact.getVersion(), ex);
            throw ex;
        }

        return result.getArtifact();
    }

    @Override
    public File getPom(Artifact artifact) throws ArtifactResolutionException {
        Objects.requireNonNull(artifact);

        return resolve(new SubArtifact(artifact, null, "pom")).getFile();
    }

    @Override
    public Model loadPom(Artifact artifact) throws ArtifactResolutionException, IOException {
        Model pomFile = modelReader.read(getPom(artifact), null);
        Properties properties = new Properties();
        if (pomFile.getParent() != null) {
            var parentPom = pomFile.getParent();
            var parentGroup = parentPom.getGroupId() == null ? artifact.getGroupId() : parentPom.getGroupId();
            var parentVersion = parentPom.getVersion() == null ? artifact.getVersion() : parentPom.getVersion();
            var parentId = new PackageId(parentGroup, parentPom.getArtifactId(), parentVersion);

            LOGGER.trace("resolving parent " + PackageId.fromArtifact(artifact) + " -> " + parentId);
            var parent = loadPom(createArtifact(parentId.group(), parentId.artifact(), parentId.version()));
            properties.putAll(parent.getProperties());
        }

        properties.putAll(pomFile.getProperties());

        var request = new DefaultModelBuildingRequest();
        request.setUserProperties(properties);
        request.setProcessPlugins(true);

        return new StringVisitorModelInterpolator()
                .setVersionPropertiesProcessor(new DefaultModelVersionProcessor())
                .interpolateModel(pomFile, null, request, new LoggedModelProblemCollector(LOGGER));
    }

    @Override
    public File getJar(Artifact artifact) throws ArtifactResolutionException {
        Objects.requireNonNull(artifact);

        return resolve(new SubArtifact(artifact, null, "jar")).getFile();
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
                LOGGER.error("Service creation failed for " + type + " with implementation " + impl, exception);
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
        session.setTransferListener(new DefaultTransferListener(LOGGER));
        session.setRepositoryListener(new DefaultRepositoryListener(LOGGER));
        session.setReadOnly();

        return session;
    }
}
