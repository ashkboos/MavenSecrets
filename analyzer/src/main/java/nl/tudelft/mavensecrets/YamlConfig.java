package nl.tudelft.mavensecrets;

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

import nl.tudelft.Extractor;

public class YamlConfig implements Config {

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

    private final Collection<? extends Extractor> extractors;
    private final int threads;

    private YamlConfig(Collection<? extends Extractor> extractors, int threads) {
        this.extractors = Objects.requireNonNull(extractors);
        this.threads = threads;
    }

    @Override
    public Collection<? extends Extractor> getExtractors() {
        return Collections.unmodifiableCollection(extractors);
    }

    /**
     * @return 
     */
    @Override
    public int getThreads() {
        return threads;
    }

    public static Config fromFile(File file) throws IOException {
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

        return new YamlConfig(collection, threads);
    }

    private static Optional<Extractor> createExtractor(String name) {
        Objects.requireNonNull(name);

        try {
            // Assume no-args constructor
            Object instance = Class.forName(name)
                    .getConstructor()
                    .newInstance();

            return Optional.of((Extractor) instance);
        } catch (ClassCastException | ReflectiveOperationException exception) {
            LOGGER.warn("Could not create extractor '" + name + "'", exception);
            return Optional.empty();
        }
    }
}
