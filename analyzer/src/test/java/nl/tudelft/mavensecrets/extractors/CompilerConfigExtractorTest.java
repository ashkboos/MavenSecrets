package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.apache.maven.model.Build;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginManagement;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import nl.tudelft.Database;
import nl.tudelft.Extractor;
import nl.tudelft.Field;
import nl.tudelft.Maven;
import nl.tudelft.Package;
import nl.tudelft.PackageId;
import nl.tudelft.mavensecrets.NopResolver;
import nl.tudelft.mavensecrets.resolver.Resolver;

public class CompilerConfigExtractorTest {

    private static Extractor extractor = null;
    private static PackageId pid = null;
    private static String pkgName = null;
    private static Database db = null;

    @Test
    public void test_fields_valid() {
        Field[] fields = extractor.fields();

        Assertions.assertNotNull(fields);
        Set<String> names = new HashSet<>();
        for (Field field : fields) {
            Assertions.assertNotNull(field);
            Assertions.assertNotNull(field.name());
            Assertions.assertNotNull(field.type());
            Assertions.assertTrue(names.add(field.name().toLowerCase()), "Duplicate field name: " + field.name());
        }
    }

    @Test
    public void test_correct_number_of_fields() throws IOException, SQLException {
        Resolver resolver = createResolver(artifact -> {
            throw new ArtifactResolutionException(null);
        });
        Maven maven = new Maven(resolver);

        try (Package pkg = createPackage(new Model())) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(extractor.fields().length, results.length);
        }
    }

    @Test
    public void test_no_parent() throws IOException, SQLException {
        Resolver resolver = createResolver(artifact -> {
            throw new ArtifactResolutionException(null);
        });
        Maven maven = new Maven(resolver);
        try (Package pkg = createPackage(new Model())) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {false, null, null, null, null, null, null}, results);
        }
    }

    @Test
    public void test_no_parent_with_plugin() throws IOException, SQLException {
        Resolver resolver = createResolver(artifact -> {
            throw new ArtifactResolutionException(null);
        });
        Maven maven = new Maven(resolver);

        Model model = new Model();
        model.setBuild(createBuild(createPlugin(true, createConfig(List.of("a", "b", "c"), "javac", "UTF-8", "1.8", "17")), null));
        
        try (Package pkg = createPackage(model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {true, "3.11.0", new byte[] {0, 1, 97, 0, 1, 98, 0, 1, 99}, "javac", "UTF-8", "1.8", "17"}, results);
        }
    }

    @Test
    public void test_no_parent_with_plugin_no_group_id() throws IOException, SQLException {
        Resolver resolver = createResolver(artifact -> {
            throw new ArtifactResolutionException(null);
        });
        Maven maven = new Maven(resolver);

        Model model = new Model();
        model.setBuild(createBuild(createPlugin(false, null), null));
        
        try (Package pkg = createPackage(model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {true, "3.11.0", null, null, null, null, null}, results);
        }
    }

    @Test
    public void test_parent_merge_1() throws IOException, SQLException {
        Map<Artifact, Model> map = new HashMap<>();
        Resolver resolver = createResolver(artifact -> {
            Model model = map.get(artifact);
            if (model != null) {
                return model;
            }
            throw new ArtifactResolutionException(null);
        });
        Maven maven = new Maven(resolver);

        Model parent = new Model();
        parent.setGroupId("my-group-id");
        parent.setArtifactId("my-artifact-id");
        parent.setVersion("1.0");
        parent.setBuild(createBuild(createPlugin(true, null), null));
        map.put(resolver.createArtifact(parent.getGroupId(), parent.getArtifactId(), parent.getVersion()), parent);

        Model child = new Model();
        Parent p = new Parent();
        p.setGroupId(parent.getGroupId());
        p.setArtifactId(parent.getArtifactId());
        p.setVersion(parent.getVersion());
        child.setParent(p);

        try (Package pkg = createPackage(child)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {true, "3.11.0", null, null, null, null, null}, results);
        }
    }

    @Test
    public void test_parent_merge_2() throws IOException, SQLException {
        Map<Artifact, Model> map = new HashMap<>();
        Resolver resolver = createResolver(artifact -> {
            Model model = map.get(artifact);
            if (model != null) {
                return model;
            }
            throw new ArtifactResolutionException(null);
        });
        Maven maven = new Maven(resolver);

        Plugin p0 = createPlugin(true, createConfig(List.of("a", "b", "c"), null, null, null, null));
        Plugin p1 = createPlugin(true, createConfig(null, "javac", null, null, null));
        Plugin p2 = createPlugin(true, createConfig(null, null, "UTF-8", null, null));
        Plugin p3 = createPlugin(true, createConfig(null, null, null, "1.8", null));

        Model parent = new Model();
        parent.setGroupId("my-group-id");
        parent.setArtifactId("my-artifact-id");
        parent.setVersion("1.0");
        parent.setBuild(createBuild(p0, p1));
        parent.getProperties().setProperty("maven.compiler.target", "17");
        map.put(resolver.createArtifact(parent.getGroupId(), parent.getArtifactId(), parent.getVersion()), parent);

        Parent p = new Parent();
        p.setGroupId(parent.getGroupId());
        p.setArtifactId(parent.getArtifactId());
        p.setVersion(parent.getVersion());
        Model child = new Model();
        child.setParent(p);
        child.setBuild(createBuild(p2, p3));

        try (Package pkg = createPackage(child)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {true, "3.11.0", new byte[] {0, 1, 97, 0, 1, 98, 0, 1, 99}, "javac", "UTF-8", "1.8", "17"}, results);
        }
    }

    @Test
    public void test_parent_merge_plugin_plugin() throws IOException, SQLException {
        Map<Artifact, Model> map = new HashMap<>();
        Resolver resolver = createResolver(artifact -> {
            Model model = map.get(artifact);
            if (model != null) {
                return model;
            }
            throw new ArtifactResolutionException(null);
        });
        Maven maven = new Maven(resolver);

        Plugin p0 = createPlugin(true, createConfig(null, null, null, null, "1"));
        Plugin p1 = createPlugin(true, createConfig(null, null, null, null, "2"));

        Model parent = new Model();
        parent.setGroupId("my-group-id");
        parent.setArtifactId("my-artifact-id-0");
        parent.setVersion("1.0");
        parent.setBuild(createBuild(p0, null));
        map.put(resolver.createArtifact(parent.getGroupId(), parent.getArtifactId(), parent.getVersion()), parent);

        Model child = new Model();
        Parent p = new Parent();
        p.setGroupId(parent.getGroupId());
        p.setArtifactId(parent.getArtifactId());
        p.setVersion(parent.getVersion());
        child.setParent(p);
        child.setBuild(createBuild(p1, null));

        try (Package pkg = createPackage(child)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {true, "3.11.0", null, null, null, null, "2"}, results);
        }
    }

    @Test
    public void test_parent_merge_plugin_pluginmanagement() throws IOException, SQLException {
        Map<Artifact, Model> map = new HashMap<>();
        Resolver resolver = createResolver(artifact -> {
            Model model = map.get(artifact);
            if (model != null) {
                return model;
            }
            throw new ArtifactResolutionException(null);
        });
        Maven maven = new Maven(resolver);

        Plugin p0 = createPlugin(true, createConfig(null, null, null, null, "1"));
        Plugin p1 = createPlugin(true, createConfig(null, null, null, null, "2"));

        Model model = new Model();
        model.setGroupId("my-group-id");
        model.setArtifactId("my-artifact-id");
        model.setVersion("1.0");
        model.setBuild(createBuild(p0, p1));
        map.put(resolver.createArtifact(model.getGroupId(), model.getArtifactId(), model.getVersion()), model);

        try (Package pkg = createPackage(model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {true, "3.11.0", null, null, null, null, "1"}, results);
        }
    }

    @Test
    public void test_parent_merge_pluginmanagement_pluginmanagement() throws IOException, SQLException {
        Map<Artifact, Model> map = new HashMap<>();
        Resolver resolver = createResolver(artifact -> {
            Model model = map.get(artifact);
            if (model != null) {
                return model;
            }
            throw new ArtifactResolutionException(null);
        });
        Maven maven = new Maven(resolver);

        Plugin p0 = createPlugin(true, createConfig(null, null, null, null, "1"));
        Plugin p1 = createPlugin(true, createConfig(null, null, null, null, "2"));

        Model parent = new Model();
        parent.setGroupId("my-group-id");
        parent.setArtifactId("my-artifact-id-0");
        parent.setVersion("1.0");
        parent.setBuild(createBuild(null, p0));
        map.put(resolver.createArtifact(parent.getGroupId(), parent.getArtifactId(), parent.getVersion()), parent);

        Model child = new Model();
        Parent p = new Parent();
        p.setGroupId(parent.getGroupId());
        p.setArtifactId(parent.getArtifactId());
        p.setVersion(parent.getVersion());
        child.setParent(p);
        child.setBuild(createBuild(null, p1));

        try (Package pkg = createPackage(child)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {true, "3.11.0", null, null, null, null, "2"}, results);
        }
    }

    @Test
    public void test_parent_merge_pluginmanagement_property() throws IOException, SQLException {
        Map<Artifact, Model> map = new HashMap<>();
        Resolver resolver = createResolver(artifact -> {
            Model model = map.get(artifact);
            if (model != null) {
                return model;
            }
            throw new ArtifactResolutionException(null);
        });
        Maven maven = new Maven(resolver);

        Plugin p0 = createPlugin(true, createConfig(null, null, null, null, "1"));

        Model model = new Model();
        model.setGroupId("my-group-id");
        model.setArtifactId("my-artifact-id");
        model.setVersion("1.0");
        model.setBuild(createBuild(null, p0));
        model.getProperties().setProperty("maven.compiler.target", "2");
        map.put(resolver.createArtifact(model.getGroupId(), model.getArtifactId(), model.getVersion()), model);

        try (Package pkg = createPackage(model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {true, "3.11.0", null, null, null, null, "1"}, results);
        }
    }

    @Test
    public void test_parent_merge_property_property() throws IOException, SQLException {
        Map<Artifact, Model> map = new HashMap<>();
        Resolver resolver = createResolver(artifact -> {
            Model model = map.get(artifact);
            if (model != null) {
                return model;
            }
            throw new ArtifactResolutionException(null);
        });
        Maven maven = new Maven(resolver);

        Model parent = new Model();
        parent.setGroupId("my-group-id");
        parent.setArtifactId("my-artifact-id-0");
        parent.setVersion("1.0");
        parent.getProperties().setProperty("maven.compiler.target", "1");
        map.put(resolver.createArtifact(parent.getGroupId(), parent.getArtifactId(), parent.getVersion()), parent);

        Model child = new Model();
        Parent p = new Parent();
        p.setGroupId(parent.getGroupId());
        p.setArtifactId(parent.getArtifactId());
        p.setVersion(parent.getVersion());
        child.setParent(p);
        child.getProperties().setProperty("maven.compiler.target", "2");

        try (Package pkg = createPackage(child)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertArrayEquals(new Object[] {false, null, null, null, null, null, "2"}, results);
        }
    }

    @BeforeAll
    public static void setup() {
        extractor = new CompilerConfigExtractor();
        pid = new PackageId("my-group", "my-artifact", "1.0");
        pkgName = "";
        db = Mockito.mock(Database.class);
    }

    @AfterAll
    public static void teardown() throws IOException {
        extractor = null;
        pid = null;
        pkgName = null;
        db = null;
    }

    private static Package createPackage(Model model) {
        return new Package(pid, null, model);
    }

    private static Resolver createResolver(LoadPomFunction function) {
        Objects.requireNonNull(function);

        return new NopResolver() {

            @Override
            public Artifact createArtifact(String groupId, String artifactId, String version) {
                return new DefaultArtifact(groupId, artifactId, "", version);
            }

            @Override
            public Model loadPom(Artifact artifact) throws ArtifactResolutionException, IOException {
                Objects.requireNonNull(artifact);

                return function.apply(artifact);
            }
        };
    }

    private static Build createBuild(Plugin plugin, Plugin pluginManagement) {
        Build build = new Build();
        if (plugin != null) {
            build.addPlugin(plugin);
        }
        if (pluginManagement != null) {
            PluginManagement pm = new PluginManagement();
            pm.addPlugin(pluginManagement);
            build.setPluginManagement(pm);
        }
        return build;
    }

    private static Plugin createPlugin(boolean hasGroupId, Xpp3Dom config) {
        Plugin plugin = new Plugin();
        if (hasGroupId) {
            plugin.setGroupId("org.apache.maven.plugins");
        }
        plugin.setArtifactId("maven-compiler-plugin");
        plugin.setVersion("3.11.0");
        if (config != null) {
            plugin.setConfiguration(config);
        }
        return plugin;
    }

    private static Xpp3Dom createConfig(List<String> cargs, String id, String encoding, String source, String target) {
        Xpp3Dom cfg = new Xpp3Dom("configuration");
        if (cargs != null) {
            Xpp3Dom args = new Xpp3Dom("compilerArgs");
            cargs.forEach(arg -> args.addChild(createEntry("arg", arg)));
            cfg.addChild(args);
        }
        if (id != null) {
            cfg.addChild(createEntry("compilerId", id));
        }
        if (encoding != null) {
            cfg.addChild(createEntry("encoding", encoding));
        }
        if (source != null) {
            cfg.addChild(createEntry("source", source));
        }
        if (target != null) {
            cfg.addChild(createEntry("target", target));
        }
        return cfg;
    }

    private static Xpp3Dom createEntry(String name, String value) {
        Objects.requireNonNull(name);
        Objects.requireNonNull(value);

        Xpp3Dom entry = new Xpp3Dom(name);
        entry.setValue(value);
        return entry;
    }

    @FunctionalInterface
    private static interface LoadPomFunction {
        Model apply(Artifact artifact) throws ArtifactResolutionException, IOException;
    }
}
