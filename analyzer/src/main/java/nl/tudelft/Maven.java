package nl.tudelft;

import java.io.IOException;
import java.util.Objects;
import java.util.jar.JarFile;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelParseException;
import org.eclipse.aether.artifact.Artifact;

import nl.tudelft.mavensecrets.resolver.Resolver;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.util.artifact.SubArtifact;

public class Maven {
    private final Resolver resolver;

    public Maven(Resolver resolver) {
        this.resolver = Objects.requireNonNull(resolver);
    }

    public Package getPackage(PackageId id) throws PackageException {
        Objects.requireNonNull(id);

        Artifact artifact = resolver.createArtifact(id.group(), id.artifact(), id.version());

        try {
            JarFile artifactFile = new JarFile(resolver.getJar(artifact));
            Model pomFile = resolver.loadPom(artifact);

            return new Package(id, artifactFile, pomFile);
        } catch (ArtifactResolutionException ex) {
            throw new PackageException(id, "unable to resolve package", ex);
        } catch (ModelParseException ex) {
            throw new PackageException(id, "unable to parse POM", ex);
        } catch (IOException ex) {
            throw new PackageException(id, "unable to read artifact", ex);
        }
    }

    public Artifact getArtifact(PackageId id, String executableType) throws ArtifactResolutionException {
        Objects.requireNonNull(id);

        Artifact artifact = resolver.createArtifact(id.group(), id.artifact(), id.version());
        Artifact sub = new SubArtifact(artifact, "", executableType);

        return resolver.resolve(sub);
    }

    public Artifact getArtifactChecksum(PackageId id, String checksumType) throws PackageException, ArtifactResolutionException {
        Objects.requireNonNull(id);

        Artifact artifact = resolver.createArtifact(id.group(), id.artifact(), id.version());
        Artifact subArtifact = new SubArtifact(artifact, "", checksumType);

        return resolver.resolve(subArtifact);
    }

    public Artifact getArtifactQualifier(PackageId id, String qualifier, String packagingType) throws PackageException, ArtifactResolutionException {
        Objects.requireNonNull(id);

        Artifact artifact = resolver.createArtifact(id.group(), id.artifact(), id.version());
        Artifact subArtifact = new SubArtifact(artifact, qualifier, packagingType);

        return resolver.resolve(subArtifact);
    }

    public Resolver getResolver() {
        return resolver;
    }
}