package nl.tudelft.mavensecrets;

import java.util.Objects;

public class ArtifactId extends PackageId {
    private final String extension;

    public ArtifactId(String group, String artifact, String version, String extension) {
        super(group, artifact, version);
        this.extension = extension;
    }

    public String extension() {
        return extension;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof ArtifactId that))
            return false;

        return super.equals(that) && this.extension.equals(that.extension);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), extension);
    }
}
