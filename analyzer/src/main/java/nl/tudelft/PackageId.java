package nl.tudelft;

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

    @Override
    public String toString() {
        return group + ":" + artifact + ":" + version;
    }
}
