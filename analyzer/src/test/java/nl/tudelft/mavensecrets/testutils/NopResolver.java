package nl.tudelft.mavensecrets.testutils;

import java.io.File;
import java.io.IOException;
import nl.tudelft.mavensecrets.resolver.Resolver;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;

/**
 * A utility no-operation {@link Resolver} implementation.
 */
public class NopResolver implements Resolver {

    private static final Resolver INSTANCE = new NopResolver();

    protected NopResolver() {
        // Nothing
    }

    @Override
    public Artifact createArtifact(String groupId, String artifactId, String version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Artifact resolve(Artifact artifact) throws ArtifactResolutionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getPom(Artifact artifact) throws ArtifactResolutionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public File getJar(Artifact artifact, String pkgType) throws ArtifactResolutionException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Model loadPom(Artifact artifact) throws ArtifactResolutionException, IOException {
        throw new UnsupportedOperationException();
    }

    /**
     * Get the resolver instance.
     *
     * @return The instance.
     */
    public static Resolver getInstance() {
        return INSTANCE;
    }
}
