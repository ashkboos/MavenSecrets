package nl.tudelft;

import org.apache.maven.model.Model;

import java.io.Closeable;
import java.io.IOException;
import java.util.jar.JarFile;

public record Package(PackageId id, JarFile jar, Model pom) implements Closeable {

    @Override
    public void close() throws IOException {
        if (jar != null) {
            jar.close();
        }
    }
}
