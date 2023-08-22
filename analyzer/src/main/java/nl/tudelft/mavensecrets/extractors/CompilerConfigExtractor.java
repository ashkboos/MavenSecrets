package nl.tudelft.mavensecrets.extractors;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import nl.tudelft.mavensecrets.Database;
import nl.tudelft.mavensecrets.Field;
import nl.tudelft.mavensecrets.Maven;
import nl.tudelft.mavensecrets.Package;
import nl.tudelft.mavensecrets.PackageId;
import nl.tudelft.mavensecrets.resolver.Resolver;

/**
 * An extractor fetching various elements of the Maven compiler plugin configuration if present.
 * This extractor looks at <code>plugin</code>, <code>pluginManagement</code> and user properties for Maven compiler plugin configurations.
 * It has some flaws:
 * <ul>
 * <li>compile:compile/compile:testCompile release parameter is not used (including its user property <code>maven.compiler.release</code>).
 * <li>The legacy <code>java.version</code> property is not used.
 * <li>No maven compiler plugin version is specified, in which cases Maven (depending on the Maven version) will resolve different plugin versions, which all have slight behavioural changes.
 * <li>Executions are ignored.
 * <li>No <code>source</code> or <code>target</code> is specified, in which case a default depending on the plugin version is used.
 * </ul>
 */
public class CompilerConfigExtractor implements Extractor {

    private static final Logger LOGGER = LogManager.getLogger(CompilerConfigExtractor.class);

    private final Field[] fields = new Field[] {
            new Field("use_maven_compiler_plugin", "BOOLEAN"),
            new Field("maven_compiler_plugin_version", "VARCHAR"),
            new Field("compiler_args", "BYTEA"), // Not pretty but this is a list
            new Field("compiler_id", "VARCHAR"),
            new Field("compiler_encoding", "VARCHAR"),
            new Field("compiler_version_source", "VARCHAR"),
            new Field("compiler_version_target", "VARCHAR"),
            new Field("compiler_version_release", "VARCHAR")
    };

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) throws IOException, SQLException {
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);
        Objects.requireNonNull(pkgType);
        Objects.requireNonNull(db);

        Resolver resolver = mvn.getResolver();

        // Get POM hierarchy
        List<ProjectMavenCompilerConfig> list = new ArrayList<>();
        PackageId id = pkg.id();
        Model model = pkg.pom();
        list.add(fetchProjectConfig(model));
        Artifact artifact = resolver.createArtifact(id.group(), id.artifact(), id.version());
        while (true) {
            Parent parent = model.getParent();

            // No parent
            if (parent == null) {
                break;
            }

            // Fetch parent artifact
            String gid = parent.getGroupId();
            String aid = parent.getArtifactId();
            String v = parent.getVersion();
            if (gid == null || aid == null || v == null) {
                LOGGER.warn("Invalid parent of artifact {}: missing groupId/artifactId/version ({})", artifact, id);
                break;
            }

            Artifact oldArtifact = artifact;
            try {
                artifact = resolver.createArtifact(gid, aid, v);
            } catch (IllegalArgumentException exception) {
                LOGGER.warn("Malformed parent artifact for {} ({})", artifact, id, exception);
                break;
            }

            LOGGER.trace("Loading POM for parent artifact {} of artifact {} ({})", artifact, oldArtifact, id);

            // Load parent model
            try {
                model = resolver.loadPom(artifact);
            } catch (ArtifactResolutionException | IOException exception) {
                LOGGER.warn("Could not load POM for artifact {} ({})", artifact, id, exception);
                break;
            }

            list.add(fetchProjectConfig(model));
        }

        // Resolution order: plugin, parent plugin, ..., plugin management, parent plugin management, ..., property, parent property, ...
        ProjectMavenCompilerConfig pcfg = list.remove(0);
        Iterator<ProjectMavenCompilerConfig> iterator = list.iterator();
        while (iterator.hasNext()) {
            pcfg = mergeConfig(iterator.next(), pcfg);
        }
        MavenCompilerConfig config = mergeConfig(pcfg.pluginManagement(), pcfg.plugin());
        config = new MavenCompilerConfig(config.present(), config.version(), config.args(), config.id(), config.encoding(), config.source() == null ? pcfg.mavenCompilerSourceProperty() : config.source(), config.target() == null ? pcfg.mavenCompilerTargetProperty() : config.target(), config.release() == null ? pcfg.mavenCompilerReleaseProperty() : config.release());

        LOGGER.trace("Found compiler configuration {} ({})", config, id);
        return config.toArray();
    }

    /**
     * Find the Maven compiler plugin in a collection of {@link Plugin plugins}.
     *
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

    /**
     * Fetch the project configuration from a {@link Model}.
     *
     * @param model The model instance.
     * @return The configuration.
     */
    private ProjectMavenCompilerConfig fetchProjectConfig(Model model) {
        Objects.requireNonNull(model);

        Optional<Build> optional = Optional.ofNullable(model.getBuild());
        MavenCompilerConfig plugin = optional
                .map(Build::getPlugins)
                .flatMap(this::findMavenCompilerPlugin)
                .map(this::fetchPluginConfig)
                .orElseGet(MavenCompilerConfig::new);
        MavenCompilerConfig pluginManagement = optional
                .map(Build::getPluginManagement)
                .map(PluginManagement::getPlugins)
                .flatMap(this::findMavenCompilerPlugin)
                .map(this::fetchPluginConfig)
                .orElseGet(MavenCompilerConfig::new);
        String source = model.getProperties().getProperty("maven.compiler.source");
        String target = model.getProperties().getProperty("maven.compiler.target");
        String release = model.getProperties().getProperty("maven.compiler.release");

        return new ProjectMavenCompilerConfig(plugin, pluginManagement, source, target, release);
    }

    /**
     * Get the compiler configuration from a {@link Plugin}.
     *
     * @param compiler The plugin instance.
     * @return The configuration.
     */
    private MavenCompilerConfig fetchPluginConfig(Plugin compiler) {
        Objects.requireNonNull(compiler);

        return Optional.of(compiler)
                .map(plugin -> {
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

                        // Nothing was written
                        if (cargs.length == 0) {
                            cargs = null;
                        }
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
                    String release = optional
                            .map(dom -> dom.getChild("release"))
                            .map(Xpp3Dom::getValue)
                            .orElse(null);

                    return new MavenCompilerConfig(true, plugin.getVersion(), cargs, cid, encoding, source, target, release);
                })
                .orElseGet(MavenCompilerConfig::new);
    }

    /**
     * Merge project configurations.
     *
     * @param parent Parent configuration.
     * @param child Child configuration.
     * @return The merged configuration.
     */
    private ProjectMavenCompilerConfig mergeConfig(ProjectMavenCompilerConfig parent, ProjectMavenCompilerConfig child) {
        Objects.requireNonNull(parent);
        Objects.requireNonNull(child);

        MavenCompilerConfig plugin = mergeConfig(parent.plugin(), child.plugin());
        MavenCompilerConfig pluginManagement = mergeConfig(parent.pluginManagement(), child.pluginManagement());
        String mavenCompilerSource = child.mavenCompilerSourceProperty() == null ? parent.mavenCompilerSourceProperty() : child.mavenCompilerSourceProperty();
        String mavenCompilerTarget = child.mavenCompilerTargetProperty() == null ? parent.mavenCompilerTargetProperty() : child.mavenCompilerTargetProperty();
        String mavenCompilerRelease = child.mavenCompilerReleaseProperty() == null ? parent.mavenCompilerReleaseProperty() : child.mavenCompilerReleaseProperty();
        return new ProjectMavenCompilerConfig(plugin, pluginManagement, mavenCompilerSource, mavenCompilerTarget, mavenCompilerRelease);
    }

    /**
     * Merge compiler configurations.
     *
     * @param parent Parent configuration.
     * @param child Child configuration.
     * @return The merged configuration.
     */
    private MavenCompilerConfig mergeConfig(MavenCompilerConfig parent, MavenCompilerConfig child) {
        Objects.requireNonNull(parent);
        Objects.requireNonNull(child);

        if (!child.present()) {
            return parent.present() ? parent : new MavenCompilerConfig();
        }

        if (!parent.present()) {
            return child;
        }

        String version = getMergedField(parent, child, MavenCompilerConfig::version);
        byte[] args = getMergedField(parent, child, MavenCompilerConfig::args);
        String id = getMergedField(parent, child, MavenCompilerConfig::id);
        String encoding = getMergedField(parent, child, MavenCompilerConfig::encoding);
        String source = getMergedField(parent, child, MavenCompilerConfig::source);
        String target = getMergedField(parent, child, MavenCompilerConfig::target);
        String release = getMergedField(parent, child, MavenCompilerConfig::release);

        return new MavenCompilerConfig(true, version, args, id, encoding, source, target, release);
    }

    /**
     * Utility method to get a field value, potentially fetching from parent if no value is set.
     *
     * @param <T> Field type.
     * @param parent Parent instance.
     * @param child Child instance.
     * @param getter The function fetching the field.
     * @return The value.
     */
    private <T> T getMergedField(MavenCompilerConfig parent, MavenCompilerConfig child, Function<? super MavenCompilerConfig, ? extends T> getter) {
        Objects.requireNonNull(parent);
        Objects.requireNonNull(child);
        Objects.requireNonNull(getter);

        T t0 = getter.apply(child);

        return t0 == null ? getter.apply(parent) : t0;
    }

    /**
     * A Maven compiler plugin configuration record.
     */
    private static record MavenCompilerConfig(boolean present, String version, byte[] args, String id, String encoding, String source, String target, String release) {

        /**
         * Create an empty configuration.
         */
        public MavenCompilerConfig() {
            this(false, null, null, null, null, null, null, null);
        }

        @Override
        public byte[] args() {
            return args == null ? null : args.clone();
        }

        /**
         * Wrap the fields in an array.
         *
         * @return The array.
         */
        public Object[] toArray() {
            return new Object[] {present, version, args, id, encoding, source, target, release};
        }

        /*
         * Records do not do deep array comparison 
         */

        @Override
        public int hashCode() {
            int result = 37;
            result = result * 17 + Boolean.hashCode(present);
            result = result * 17 + Objects.hash(version);
            result = result * 17 + Arrays.hashCode(args);
            result = result * 17 + Objects.hash(id);
            result = result * 17 + Objects.hash(encoding);
            result = result * 17 + Objects.hash(source);
            result = result * 17 + Objects.hash(target);
            result = result * 17 + Objects.hash(release);
            return result;
        }

        @Override
        public boolean equals(Object object) {
            if (object instanceof MavenCompilerConfig other) {
                return present == other.present()
                        && Objects.equals(version, other.version())
                        && Arrays.equals(args, other.args())
                        && Objects.equals(id, other.id())
                        && Objects.equals(encoding, other.encoding())
                        && Objects.equals(source, other.source())
                        && Objects.equals(target, other.target())
                        && Objects.equals(release, other.release());
            }
            return false;
        }

        @Override
        public String toString() {
            return new StringBuilder()
                    .append(this.getClass().getSimpleName())
                    .append("[present=")
                    .append(present)
                    .append(", version=")
                    .append(version)
                    .append(", args=")
                    .append(Arrays.toString(args))
                    .append(", id=")
                    .append(id)
                    .append(", encoding=")
                    .append(encoding)
                    .append(", source=")
                    .append(source)
                    .append(", target=")
                    .append(target)
                    .append(", release=")
                    .append(release)
                    .append(']')
                    .toString();
        }
    }

    /**
     * A POM compiler configuration record.
     */
    private static record ProjectMavenCompilerConfig(MavenCompilerConfig plugin, MavenCompilerConfig pluginManagement, String mavenCompilerSourceProperty, String mavenCompilerTargetProperty, String mavenCompilerReleaseProperty) {
        // Nothing
    }
}
