package nl.tudelft;

import java.util.Optional;

public class PackageId {
    private String group;
    private String artifact;
    private String version;

    public PackageId(String group, String artifact, String version) {
        this.group = group;
        this.artifact = artifact;
        this.version = version;
    }

    public String getGroup() {
        return group;
    }

    public String getArtifact() {
        return artifact;
    }

    public String getVersion() {
        return version;
    }

    public static Optional<PackageId> tryParse(String text) {
        var split = text.split(":", 4);
        if (split.length != 3)
            return Optional.empty();

        return Optional.of(new PackageId(split[0], split[1], split[2]));
    }
}
