package nl.tudelft.mavensecrets.resolver;

import java.io.File;

import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;

/**
 * An artifact resolver.
 */
public interface Resolver {

    /**
     * Create an artifact descriptor.
     *
     * @param groupId Group id.
     * @param artifactId Artifact id.
     * @param version Version.
     * @return The artifact.
     */
    Artifact createArtifact(String groupId, String artifactId, String version);

    /**
     * Resolve an artifact.
     *
     * @param artifact Artifact.
     * @return The artifact or an empty optional if resolution fails.
     */
    Artifact resolve(Artifact artifact) throws ArtifactResolutionException;

    /**
     * Resolve an artifact's POM.
     *
     * @param artifact Artifact.
     * @return The file location of the POM or an empty optional if resolution fails.
     */
    File getPom(Artifact artifact) throws ArtifactResolutionException;

    /**
     * Resolve an artifact.
     *
     * @param artifact Artifact.
     * @param pkgType
     * @return The file location of the artifact or an empty optional if resolution fails.
     */
    File getJar(Artifact artifact, String pkgType) throws ArtifactResolutionException;
}
