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

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (!(obj instanceof PackageId)) {
            return false;
        }

        PackageId other = (PackageId) obj;

        return this.group.equals(other.group) && this.artifact.equals(other.artifact) &&  this.version.equals(other.version);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 17;
        result = prime * result + (group == null ? 0 : group.hashCode());
        result = prime * result + (artifact == null ? 0 : artifact.hashCode());
        result = prime * result + (version == null ? 0 : version.hashCode());
        return result;
    }
}
