package nl.tudelft.mavensecrets.config;

import java.io.File;
import java.util.Collection;

import nl.tudelft.Extractor;

/**
 * The program's configuration.
 */
public interface Config {

    /**
     * Get a collection of {@link Extractor Extractors} to run.
     *
     * @return The extractor instances.
     */
    Collection<? extends Extractor> getExtractors();

    /**
     * Get the thread pool size.
     *
     * @return The number of threads to use.
     */
    int getThreads();

    /**
     * Get the database configuration.
     *
     * @return The database configuration.
     */
    Database getDatabaseConfig();

    /**
     * Get the index files to run.
     *
     * @return The index file names.
     */
    Collection<? extends String> getIndexFiles();

    /**
     * Get the local repository location.
     *
     * @return The local repository to use.
     */
    File getLocalRepository();

    /**
     * Get the seed for the random sampling.
     *
     * @return The seed.
     */
    long getSeed();


    /**
     * Get the percentage of the dataset to be sampled.
     *
     * @return The sample percentage.
     */
    float getSamplePercent();

    /**
     * A database configuration.
     */
    public static interface Database {

        /**
         * Get the hostname.
         *
         * @return The hostname.
         */
        String getHostname();

        /**
         * Get the port.
         *
         * @return The port.
         */
        int getPort();

        /**
         * Get the database name.
         *
         * @return The name.
         */
        String getName();

        /**
         * Get the database username or <code>null</code> if no credentials are required.
         *
         * @return The username.
         */
        String getUsername();

        /**
         * Get the database password or <code>null</code> if no credentials are required.
         *
         * @return The password.
         */
        String getPassword();
    } 
}
