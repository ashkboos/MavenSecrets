package nl.tudelft.mavensecrets;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.jar.JarFile;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.ModelParseException;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.util.artifact.SubArtifact;

import nl.tudelft.mavensecrets.resolver.Resolver;

public class Maven {

    private static final Logger LOGGER = LogManager.getLogger(Maven.class);

    private final Resolver resolver;

    public Maven(Resolver resolver) {
        this.resolver = Objects.requireNonNull(resolver);
    }

    public Package getPackage(ArtifactId id) throws PackageException {
        Objects.requireNonNull(id);

        Artifact artifact = resolver.createArtifact(id.group(), id.artifact(), id.version());

        File jar;
        try {
            jar = id.extension().equals("pom") ? null : resolver.getJar(artifact, id.extension());
        } catch (ArtifactResolutionException exception) {
            LOGGER.warn("Could not fetch archive ({})", id, exception);
            jar = null;
        }

        JarFile jf;
        try {
            jf = jar == null ? null : new JarFile(jar);
        } catch (IOException exception) {
            LOGGER.warn("Could not open archive {} ({})", jar.getAbsolutePath(), id, exception);
            jf = null;
        }

        Model pomFile;
        try {
            pomFile = resolver.loadPom(artifact);
        } catch (ArtifactResolutionException | IOException exception) {
            throw new PackageException(id, "Could not fetch POM", exception);
        }

        return new Package(id, jf, pomFile);
    }

    public Model getPom(PackageId id) throws PackageException {
        Objects.requireNonNull(id);
        Artifact artifact = resolver.createArtifact(id.group(), id.artifact(), id.version());

        try {
            Model pomFile = resolver.loadPom(artifact);
            return pomFile;
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