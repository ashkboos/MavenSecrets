package nl.tudelft.mavensecrets.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Reader;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.DumperOptions.FlowStyle;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;
import org.yaml.snakeyaml.reader.UnicodeReader;
import org.yaml.snakeyaml.representer.Representer;

import nl.tudelft.mavensecrets.extractors.Extractor;
import nl.tudelft.mavensecrets.config.Config.Database;
import nl.tudelft.mavensecrets.config.MemoryConfig.MemoryDatabase;

/**
 * A YAML-based {@link Config} loader.
 */
public class YamlConfig {

    private static final Logger LOGGER = LogManager.getLogger(YamlConfig.class);
    private static final Yaml YAML;

    static {
        LoaderOptions loader = new LoaderOptions();
        // Not really safe but it's not really an attack vector
        loader.setMaxAliasesForCollections(Integer.MAX_VALUE);
        loader.setCodePointLimit(Integer.MAX_VALUE);
        loader.setProcessComments(true);

        DumperOptions dumper = new DumperOptions();
        dumper.setDefaultFlowStyle(FlowStyle.BLOCK);

        SafeConstructor constructor = new SafeConstructor(loader);
        Representer representer = new Representer(dumper);

        YAML = new Yaml(constructor, representer, dumper, loader);
    }

    private YamlConfig() {
        // Nothing
    }

    /**
     * Load a {@link Config} from file.
     *
     * @param file File.
     * @return The loaded configuration.
     * @throws IOException If an I/O error occurs.
     */
    public static Config fromFile(File file) throws IOException {
        Objects.requireNonNull(file);

        Map<?, ?> map;
        try (Reader reader = new UnicodeReader(new FileInputStream(file))) {
            Object object = YAML.load(reader);
            map = object instanceof Map ? (Map<?, ?>) object : null;
        }

        Collection<Extractor> collection = Optional.ofNullable(map)
                .map(m -> m.get("extractors"))
                .map(object -> object instanceof Collection ? (Collection<?>) object : null)
                .stream()
                .flatMap(Collection::stream)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .distinct()
                .map(YamlConfig::createExtractor)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .toList();

        int threads = Optional.ofNullable(map)
                .map(x -> x.get("threads"))
                .map(x -> x instanceof Number ? (Number) x : null)
                .map(Number::intValue)
                .stream()
                .filter(x -> x > 0)
                .findFirst()
                .orElse(8); // Default

        Database db = Optional.ofNullable(map)
                .map(x -> x.get("database"))
                .map(x -> x instanceof Map ? (Map<?, ?>) x : null)
                .map(x -> {
                    Object p = x.get("hostname");
                    Object q = x.get("port");
                    Object r = x.get("name");
                    Object s = x.get("username");
                    Object t = x.get("password");

                    if (p instanceof String hostname && q instanceof Number port && r instanceof String name) {
                        return new MemoryDatabase(hostname, port.intValue(), name, s instanceof String ? (String) s : null, t instanceof String ? (String) t : null);
                    }

                    return null;
                })
                .orElseGet(() -> new MemoryDatabase("localhost", 5432, "postgres", null, null));

        @SuppressWarnings("unchecked")
        Collection<String> indices = Optional.ofNullable(map)
                .map(x -> x.get("indexfile"))
                .map(x -> {
                    if (x instanceof Collection<?> c) {
                        return (Collection<Object>) c;
                    }
                    else if (x instanceof String s) {
                        return Collections.singleton(s);
                    }
                    else {
                        return null;
                    }
                })
                .stream()
                .flatMap(Collection::stream)
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .distinct()
                .toList();

        File m2 = Optional.ofNullable(map)
                .map(x -> x.get("m2-dir"))
                .map(x -> x instanceof String ? (String) x : null)
                .map(File::new)
                .orElseGet(() -> new File(System.getProperty("user.home"), ".m2/repository"));

        long seed = Optional.ofNullable(map)
                .map(x -> x.get("seed"))
                .map(x -> x instanceof Number ? (Number) x : null)
                .map(Number::longValue)
                .orElseGet(System::currentTimeMillis);

        float samplePercent = Optional.ofNullable(map)
                .map(x -> x.get("sample-percentage"))
                .map(x -> x instanceof Number ? (Number) x : null)
                .map(Number::floatValue)
                .stream()
                .filter(x -> x > 0 && x <= 100)
                .findFirst()
                .orElse(0.1f);

        return new MemoryConfig(collection, threads, db, indices, m2, seed, samplePercent);
    }

    /**
     * Attempt creating an extractor instance.
     *
     * @param name The extractor's full name.
     * @return The extractor wrapped in an {@link Optional}.
     */
    private static Optional<Extractor> createExtractor(String name) {
        Objects.requireNonNull(name);

        try {
            // Assume no-args constructor
            Object instance = Class.forName(name)
                    .getConstructor()
                    .newInstance();

            return Optional.of((Extractor) instance);
        } catch (ClassCastException | ReflectiveOperationException exception) {
            LOGGER.warn("Could not create extractor '{}'", name, exception);
            return Optional.empty();
        }
    }
}
