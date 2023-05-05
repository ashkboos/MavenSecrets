package nl.tudelft;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarFile;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelParseException;
import org.apache.maven.model.io.ModelReader;
import org.eclipse.aether.artifact.Artifact;

import nl.tudelft.mavensecrets.resolver.Resolver;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.util.artifact.SubArtifact;

public class Maven {
    private final Resolver resolver;
    private final ModelReader modelReader;

    public Maven(Resolver resolver) {
        this.resolver = Objects.requireNonNull(resolver);
        this.modelReader = new DefaultModelReader();
    }

    public Package getPackage(PackageId id) throws PackageException {
        Objects.requireNonNull(id);

        Artifact artifact = resolver.createArtifact(id.group(), id.artifact(), id.version());

        try {
            JarFile artifactFile = new JarFile(resolver.getJar(artifact));
            Model pomFile = modelReader.read(resolver.getPom(artifact), null);

            return new Package(id, artifactFile, pomFile);
        } catch (ArtifactResolutionException ex) {
            throw new PackageException(id, "unable to resolve package", ex);
        } catch (ModelParseException ex) {
            throw new PackageException(id, "unable to parse POM", ex);
        } catch (IOException ex) {
            throw new PackageException(id, "unable to read artifact", ex);
        }
    }

    public List<Artifact> getArtifactSources(PackageId id) throws PackageException, ArtifactResolutionException {
        Objects.requireNonNull(id);
        List<Artifact> allArtifact = new ArrayList<>();

        Artifact artifact = resolver.createArtifact(id.group(), id.artifact(), id.version());
        Artifact subMd5 = new SubArtifact(artifact, "sources", "jar.md5");
        Artifact subSha = new SubArtifact(artifact, "sources", "jar.sha1");

        allArtifact.add(resolver.resolve(subMd5));
        allArtifact.add(resolver.resolve(subSha));

        return allArtifact;
    }

    public List<Artifact> getArtifactJavaDoc(PackageId id) throws PackageException, ArtifactResolutionException {
        Objects.requireNonNull(id);
        List<Artifact> allArtifact = new ArrayList<>();

        Artifact artifact = resolver.createArtifact(id.group(), id.artifact(), id.version());
        Artifact subMd5 = new SubArtifact(artifact, "javadoc", "jar.md5");
        Artifact subSha = new SubArtifact(artifact, "javadoc", "jar.sha1");

        allArtifact.add(resolver.resolve(subMd5));
        allArtifact.add(resolver.resolve(subSha));

        return allArtifact;
    }
}