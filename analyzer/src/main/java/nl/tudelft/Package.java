package nl.tudelft;

import java.io.Closeable;
import java.util.jar.JarFile;
import org.apache.maven.model.Model;

public class Package implements Closeable {
    private final PackageId id;
    private final JarFile jar;
    private final Model pom;

    public Package(PackageId id, JarFile jar, Model pom) {
        this.id = id;
        this.jar = jar;
        this.pom = pom;
    }

    public PackageId getId() {
        return id;
    }

    public JarFile getJar() {
        return jar;
    }

    public Model getPom() {
        return pom;
    }

    @Override
    public void close() {}
}
