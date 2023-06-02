package nl.tudelft.mavensecrets;

import java.util.Optional;
import org.eclipse.aether.artifact.Artifact;

public class PackageId {
    private final String group;
    private final String artifact;
    private final String version;

    public PackageId(String group, String artifact, String version) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
    }

    public String group() {
        return group;
    }

    public String artifact() {
        return artifact;
    }

    public String version() {
        return version;
    }

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
        if (obj == this)
            return true;
        if (!(obj instanceof PackageId that))
            return false;

        return this.group.equals(that.group) && this.artifact.equals(that.artifact) && this.version.equals(that.version);
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
