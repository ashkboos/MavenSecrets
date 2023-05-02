package nl.tudelft.mavensecrets.resolver;

import java.io.File;
import java.util.Optional;

import org.eclipse.aether.artifact.Artifact;

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
     * Create an artifact descriptor.
     *
     * @param coords Descriptor.
     * @return The artifact.
     */
    Artifact createArtifact(String coords);

    /**
     * Resolve an artifact.
     *
     * @param artifact Artifact.
     * @return The artifact or an empty optional if resolution fails.
     */
    Optional<Artifact> resolve(Artifact artifact);

    /**
     * Resolve an artifact's POM.
     *
     * @param artifact Artifact.
     * @return The file location of the POM or an empty optional if resolution fails.
     */
    Optional<File> getPom(Artifact artifact);

    /**
     * Resolve an artifact.
     *
     * @param artifact Artifact.
     * @return The file location of the artifact or an empty optional if resolution fails.
     */
    Optional<File> getArtifact(Artifact artifact);
}
