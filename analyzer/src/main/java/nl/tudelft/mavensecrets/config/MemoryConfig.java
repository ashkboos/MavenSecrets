package nl.tudelft.mavensecrets.config;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

import nl.tudelft.Extractor;
import nl.tudelft.mavensecrets.config.Config.Database;

/**
 * An in-memory {@link Config}.
 */
public record MemoryConfig(Collection<? extends Extractor> extractors, int threads, Database databaseConfig, Collection<? extends String> indices, File repository, long seed) implements Config {

    /**
     * Create a configuration instance.
     *
     * @param extractors Extractor instances to run.
     * @param threads Thread pool size.
     * @param databaseConfig Database configuration.
     * @param indices Index file names to run.
     */
    public MemoryConfig(Collection<? extends Extractor> extractors, int threads, Database databaseConfig, Collection<? extends String> indices, File repository, long seed) {
        this.extractors = Collections.unmodifiableCollection(new ArrayList<>(Objects.requireNonNull(extractors)));
        this.threads = threads;
        this.databaseConfig = Objects.requireNonNull(databaseConfig);
        this.indices = Collections.unmodifiableCollection(new ArrayList<>(Objects.requireNonNull(indices)));
        this.repository = Objects.requireNonNull(repository);
        this.seed = seed;
        if (threads <= 0) {
            throw new IllegalArgumentException("Invalid thread count: " + threads);
        }
    }

    @Override
    public Collection<? extends Extractor> getExtractors() {
        return extractors();
    }

    @Override
    public int getThreads() {
        return threads();
    }

    @Override
    public Database getDatabaseConfig() {
        return databaseConfig();
    }

    @Override
    public Collection<? extends String> getIndexFiles() {
        return indices();
    }

    @Override
    public File getLocalRepository() {
        return repository();
    }

    /**
     * @return 
     */
    @Override
    public long getSeed() {
        return seed();
    }


    /**
     * An in-memory {@link Database} configuration.
     */
    public static record MemoryDatabase(String hostname, int port, String name, String username, String password) implements Database {

        /**
         * Create a configuration instance.
         *
         * @param hostname Database hostname.
         * @param port Database port.
         * @param name Database name.
         * @param username Username or <code>null</code> for no credentials.
         * @param password Password or <code>null</code> for no credentials.
         */
        public MemoryDatabase(String hostname, int port, String name, String username, String password) {
            this.hostname = Objects.requireNonNull(hostname);
            this.port = port;
            this.name = Objects.requireNonNull(name);
            this.username = username;
            this.password = password;
        }

        @Override
        public String getHostname() {
            return hostname();
        }

        @Override
        public int getPort() {
            return port();
        }

        @Override
        public String getName() {
            return name();
        }

        @Override
        public String getUsername() {
            return username();
        }

        @Override
        public String getPassword() {
            return password();
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder()
                    .append(this.getClass().getSimpleName())
                    .append("[hostname=")
                    .append(hostname)
                    .append(", port=")
                    .append(port)
                    .append(", name=")
                    .append(name);

            if (username != null) {
                sb
                        .append(", username=")
                        .append(username)
                        .append(", password=********"); // Field should be masked
            }

            return sb
                    .append(']')
                    .toString();
        }
    }
}
