package nl.tudelft;

import java.io.Closeable;
import java.io.IOException;
import java.util.jar.JarFile;
import org.apache.maven.model.Model;

public record Package(PackageId id, JarFile jar, Model pom) implements Closeable {

    @Override
    public void close() throws IOException {
        if (jar != null) {
            jar.close();
        }
    }
}
