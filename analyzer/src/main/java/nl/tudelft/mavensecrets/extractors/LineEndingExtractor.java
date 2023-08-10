package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Collections;
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

public class LineEndingExtractor implements Extractor {

    private static final Logger LOGGER = LogManager.getLogger(LineEndingExtractor.class);
    private static final LineEndingCounter COUNTER_UNIX = new RegexLineEndingCounter(Pattern.compile("(?<!\r)\n"));
    private static final LineEndingCounter COUNTER_WINDOWS = new RegexLineEndingCounter(Pattern.compile("\r\n"));
    private static final LineEndingCounter COUNTER_MACINTOSH = new RegexLineEndingCounter(Pattern.compile("\r(?!\n)"));

    private final Field[] fields = new Field[0];

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

            // TODO: Implementation
            List<LineEndingCounter> counters = Collections.emptyList();

            for (LineEndingCounter counter : counters) {
                @SuppressWarnings("unused") long occurrences = counter.countOccurrences(string);

                // TODO: Handle results
            }
        }

        return results;
    }

    private boolean shouldProcess(String path) {
        // Preconditions
        Objects.requireNonNull(path);

        // TODO: Implementation
        return true;
    }

    @FunctionalInterface
    private static interface LineEndingCounter {
        long countOccurrences(String string);
    }

    private static class RegexLineEndingCounter implements LineEndingCounter {

        private final Pattern pattern;

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
