package nl.tudelft;

import org.eclipse.aether.artifact.Artifact;

import java.util.Optional;

public record PackageId(String group, String artifact, String version) {
    @Override
    public String toString() {
        return group + ":" + artifact + ":" + version;
    }

    public static PackageId fromArtifact(Artifact artifact) {
        return new PackageId(artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion());
    }

    public static Optional<PackageId> tryParse(String text) {
        var split = text.split(":", 4);
        if (split.length != 3)
            return Optional.empty();

        return Optional.of(new PackageId(split[0], split[1], split[2]));
    }
}
