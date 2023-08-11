package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import nl.tudelft.mavensecrets.Database;
import nl.tudelft.mavensecrets.Field;
import nl.tudelft.mavensecrets.Maven;
import nl.tudelft.mavensecrets.Package;

/**
 * An {@link Extractor} looking at line endings.
 */
public class LineEndingExtractor implements Extractor {

    private static final Logger LOGGER = LogManager.getLogger(LineEndingExtractor.class);
    private static final LineEndingCounter COUNTER_UNIX = new RegexLineEndingCounter(Pattern.compile("(?<!\r)\n"));
    private static final LineEndingCounter COUNTER_WINDOWS = new RegexLineEndingCounter(Pattern.compile("\r\n"));
    //private static final LineEndingCounter COUNTER_MACINTOSH = new RegexLineEndingCounter(Pattern.compile("\r(?!\n)"));

    private final Field[] fields = new Field[] {
            new Field("line_ending_lf", "BOOLEAN"),
            new Field("line_ending_crlf", "BOOLEAN"),
            new Field("line_ending_inconsistent", "BOOLEAN"),
            new Field("line_ending_inconsistent_in_file", "BOOLEAN")
    };

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) throws IOException, SQLException {
        // Preconditions
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);
        Objects.requireNonNull(pkgType);
        Objects.requireNonNull(db);

        Object[] results = new Object[fields.length];

        JarFile jar = pkg.jar();
        LOGGER.trace("Found jar: {} ({})", jar != null, pkg.id());

        // No archive
        if (jar == null) {
            return results;
        }

        // Find matching entries
        List<JarEntry> entries = jar.stream()
                .filter(entry -> !entry.isDirectory())
                .filter(entry -> shouldProcess(entry.getRealName()))
                .toList();

        LOGGER.trace("Found {} file(s) to inspect ({})", entries.size(), pkg.id());

        // No files
        if (entries.isEmpty()) {
            return results;
        }

        // Default values
        Arrays.fill(results, false);

        // Create a buffer here so we do not have to allocate space in every iteration
        char[] buf = new char[1 << 10];

        for (JarEntry entry : entries) {
            String string;

            // Read the input as ASCII
            try (InputStream stream = jar.getInputStream(entry); Reader reader = new InputStreamReader(stream, StandardCharsets.US_ASCII)) {
                StringBuilder builder = new StringBuilder();
                int len;
                while ((len = reader.read(buf)) != -1) {
                    builder.append(buf, 0, len);
                }
                string = builder.toString();
            } catch (IOException exception) {
                LOGGER.warn("Could not read entry {} ({})", entry.getRealName(), pkg.id(), exception);
                continue;
            }

            long lf = COUNTER_UNIX.countOccurrences(string);
            long crlf = COUNTER_WINDOWS.countOccurrences(string);

            // This is a bit unfortunate
            results[0] = ((boolean) results[0]) || lf != 0;
            results[1] = ((boolean) results[1]) || crlf != 0;

            if (lf == 0 && crlf == 0) {
                LOGGER.trace("Found entry without line endings: {} ({})", entry.getRealName(), pkg.id());
            }
            else if (lf != 0 && crlf != 0) {
                LOGGER.trace("Found entry with different line endings: {} ({})", entry.getRealName(), pkg.id());
                results[3] = true;
            }
        }

        results[2] = ((boolean) results[0]) && ((boolean) results[1]);

        return results;
    }

    /**
     * Check if the archive entry with the given path should have its line endings counted.
     *
     * @param path Target path.
     * @return If the entry should be processed by this extractor.
     * @throws NullPointerException If <code>path</code> is <code>null</code>.
     */
    private boolean shouldProcess(String path) {
        // Preconditions
        Objects.requireNonNull(path);

        // TODO: Implementation
        return true;
    }

    /**
     * A line ending counter, counting how often a line ending occurs.
     */
    @FunctionalInterface
    private static interface LineEndingCounter {

        /**
         * Count the number of line endings in a given string.
         *
         * @param string Input string.
         * @return The number of occurrences.
         * @throws NullPointerException If <code>string</code> is <code>null</code>.
         */
        long countOccurrences(String string);
    }

    /**
     * A {@link LineEndingCounter} that matches line endings on a regular expression.
     */
    private static class RegexLineEndingCounter implements LineEndingCounter {

        private final Pattern pattern;

        /**
         * Create a line ending counter for a given {@link Pattern}.
         *
         * @param pattern The pattern.
         * @throws NullPointerException If <code>pattern</code> is <code>null</code>.
         */
        public RegexLineEndingCounter(Pattern pattern) {
            this.pattern = Objects.requireNonNull(pattern);
        }

        @Override
        public long countOccurrences(String string) {
            // Preconditions
            Objects.requireNonNull(string);

            Matcher matcher = pattern.matcher(string);

            long count = 0;
            while (matcher.find()) {
                count++;
            }
            return count;
        }
    }
}
