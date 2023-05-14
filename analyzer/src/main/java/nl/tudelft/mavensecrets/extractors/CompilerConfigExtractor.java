package nl.tudelft.mavensecrets.extractors;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.codehaus.plexus.util.xml.Xpp3Dom;

import nl.tudelft.Extractor;
import nl.tudelft.Field;
import nl.tudelft.Maven;
import nl.tudelft.Package;

/**
 * An extractor fetching various elements of the Maven compiler plugin configuration if present.
 * Note that placeholders, configurations inherited from parents, user properties and executions are not checked.
 */
public class CompilerConfigExtractor implements Extractor {

    private final Field[] fields = new Field[] {
            new Field("use_maven_compiler_plugin", "BOOLEAN"),
            new Field("maven_compiler_plugin_version", "VARCHAR(128)"),
            new Field("compiler_args", "BYTEA"), // Not pretty but this is a list
            new Field("compiler_id", "VARCHAR(128)"),
            new Field("compiler_encoding", "VARCHAR(128)"),
            new Field("compiler_version_source", "VARCHAR(128)"),
            new Field("compiler_version_target", "VARCHAR(128)")
    };

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType) throws IOException {
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);

        Model model = pkg.pom();

        return Optional.ofNullable(model.getBuild())
                .map(Build::getPlugins)
                .flatMap(this::findMavenCompilerPlugin)
                .<Object[]>map(plugin -> {
                    Optional<Xpp3Dom> optional = Optional.ofNullable(plugin.getConfiguration())
                            .map(object -> object instanceof Xpp3Dom ? (Xpp3Dom) object : null);
                    byte[] cargs;
                    try (ByteArrayOutputStream baos = new ByteArrayOutputStream(); DataOutputStream dos = new DataOutputStream(baos)) {
                        optional
                                .map(dom -> dom.getChild("compilerArgs"))
                                .map(Xpp3Dom::getChildren)
                                .stream()
                                .flatMap(Arrays::stream)
                                .map(Xpp3Dom::getValue)
                                .forEach(str -> {
                                    try {
                                        dos.writeUTF(str);
                                    } catch (IOException exception) {
                                        throw new AssertionError(exception);
                                    }
                                });

                        cargs = baos.toByteArray();
                    } catch (IOException exception) {
                        throw new AssertionError(exception);
                    }
                    String cid = optional
                            .map(dom -> dom.getChild("compilerId"))
                            .map(Xpp3Dom::getValue)
                            .orElse(null);
                    String encoding = optional
                            .map(dom -> dom.getChild("encoding"))
                            .map(Xpp3Dom::getValue)
                            .orElse(null);
                    String source = optional
                            .map(dom -> dom.getChild("source"))
                            .map(Xpp3Dom::getValue)
                            .orElse(null);
                    String target = optional
                            .map(dom -> dom.getChild("target"))
                            .map(Xpp3Dom::getValue)
                            .orElse(null);
                    return new Object[] {
                            true,
                            plugin.getVersion(),
                            cargs,
                            cid,
                            encoding,
                            source,
                            target
                    };
                })
                .orElseGet(() -> {
                    Object[] results = new Object[fields.length];
                    results[0] = false;
                    return results;
                });
    }

    /**
     * Find the Maven compiler plugin in a collection of {@link Plugin plugins}.
     * @param collection Collection.
     * @return The plugin instance wrapped in an {@link Optional}.
     */
    private Optional<Plugin> findMavenCompilerPlugin(Collection<Plugin> collection) {
        Objects.requireNonNull(collection);

        return collection.stream()
                .filter(plugin -> {
                    String groupId = plugin.getGroupId();
                    // For maven plugins group id can be omitted as it is a reserved prefix
                    return groupId == null || groupId.equals("org.apache.maven.plugins");
                })
                .filter(plugin -> plugin.getArtifactId().equals("maven-compiler-plugin"))
                .findAny();
    }
}
