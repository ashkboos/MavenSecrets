package nl.tudelft;

import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.jar.JarFile;

import org.apache.maven.model.Model;
import org.apache.maven.model.io.DefaultModelReader;
import org.apache.maven.model.io.ModelReader;
import org.eclipse.aether.artifact.Artifact;

import nl.tudelft.mavensecrets.resolver.Resolver;

public class Maven {

    private final Resolver resolver;
 
    public Maven(Resolver resolver) {
        this.resolver = Objects.requireNonNull(resolver);
    }

    public Package getPackage(PackageId id) {
        Objects.requireNonNull(id);

        Artifact artifact = resolver.createArtifact(id.getGroup(), id.getArtifact(), id.getVersion());

        JarFile artifactFile = resolver.getArtifact(artifact)
                .map(file -> {
                    try {
                        return new JarFile(file);
                    } catch (IOException exception) {
                        // FIXME Exception handling?
                        return null;
                    }
                })
                .orElse(null);
 
        if (artifactFile == null) {
            return null;
        }
        
        Model pomFile = resolver.getPom(artifact).map(file -> {
            try {
                return parsePom(file);
            } catch (IOException exception) {
                // FIXME Exception handling?
                return null;
            }
        }).orElse(null);

        if (pomFile == null) {
            return null;
        }

        return new Package(id, artifactFile, pomFile);
    }

    private Model parsePom(File file) throws IOException {
        // TODO Implementation
        ModelReader mr = new DefaultModelReader();
        return mr.read(file, null);
    }
}