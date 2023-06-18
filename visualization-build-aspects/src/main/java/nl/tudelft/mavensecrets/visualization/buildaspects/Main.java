package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.CompilerConfigData;
import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.Id;
import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.JavaModuleData;
import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.JavaVersionData;

/**
 * The main class.
 */
public class Main {

    private static final Logger LOGGER = LogManager.getLogger(Main.class);
    private static final File CWD = new File(".");
    private static final Pattern UTF_PATTERN = Pattern.compile("^([uU][tT][fF])(\\d+.*)$");

    static {
        // Legacy driver registring because Maven shade does funny things
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    /**
     * The application entry point.
     *
     * @param args Command line arguments.
     */
    public static void main(@NotNull String[] args) {
        // Usage
        if (args.length != 3 && args.length != 5) {
            LOGGER.error("Usage: <hostname> <port> <database> [<username> <password>]");
            return;
        }

        // Read all data into memory
        // This is not efficient but workable for the size of the dataset
        List<? extends DataEntry> data;
        int archiveSize;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(getConnection(args))) {
            int indexSize = wrapper.querySingular("SELECT COUNT(*) FROM package_list", rs -> rs.getInt(1));
            int indexSizeDistinct = wrapper.querySingular("SELECT COUNT(*) FROM (SELECT DISTINCT ON (groupid, artifactid) COUNT(*) FROM package_list GROUP BY groupid, artifactid) AS tbl", rs -> rs.getInt(1));
            LOGGER.info("Indexed packages: {} ({} ({}%) distinct package(s))", indexSize, indexSizeDistinct, Util.ratio(indexSizeDistinct, indexSize));

            int sampleSize = wrapper.querySingular("SELECT COUNT(*) FROM selected_packages", rs -> rs.getInt(1));
            int sampleSizeDistinct = wrapper.querySingular("SELECT COUNT(*) FROM (SELECT DISTINCT ON (groupid, artifactid) COUNT(*) FROM selected_packages GROUP BY groupid, artifactid) AS tbl", rs -> rs.getInt(1));
            LOGGER.info("Selected packages: {} ({} ({}%) distinct package(s), {}% of distinct packages, {}%)", sampleSize, sampleSizeDistinct, Util.ratio(sampleSizeDistinct, sampleSize), Util.ratio(sampleSizeDistinct, indexSizeDistinct), Util.ratio(sampleSize, indexSize));

            int successSize = wrapper.querySingular("SELECT COUNT(*) FROM packages", rs -> rs.getInt(1));
            int failSize = sampleSize - successSize;
            LOGGER.info("Selected packages (resolution fail): {} ({}%)", failSize, Util.ratio(failSize, sampleSize));
            LOGGER.info("Selected packages (resolution success): {} ({}%)", successSize, Util.ratio(successSize, sampleSize));

            archiveSize = wrapper.querySingular("SELECT COUNT(*) FROM packages WHERE has_artifact = true", rs -> rs.getInt(1));
            LOGGER.info("Selected packages (resolution success, with archive): {} ({}%)", archiveSize, Util.ratio(archiveSize, successSize));

            /*
             * groupid (string)
             * artifactid (string)
             * version (string)
             * year (int)
             * has_artifact (bool) 
             * use_maven_compiler_plugin (bool)
             * maven_compiler_plugin_version (string)
             * compiler_args (binary)
             * compiler_id (string)
             * compiler_encoding (string)
             * compiler_version_source (string)
             * compiler_version_target (string)
             * use_java_modules (bool)
             * java_version_manifest_1 (string)
             * java_version_manifest_2 (string)
             * java_version_manifest_3 (string)
             * java_version_manifest_multirelease (string)
             * java_version_class_major (binary)
             * java_version_class_minor (binary)
             * java_version_class_map (binary)
             */

            String chonker = "SELECT packages.groupid, packages.artifactid, packages.version, DATE_PART('year', lastmodified) AS year, has_artifact, use_maven_compiler_plugin, maven_compiler_plugin_version, compiler_args, compiler_id, compiler_encoding, compiler_version_source, compiler_version_target, use_java_modules, java_version_manifest_1, java_version_manifest_2, java_version_manifest_3, java_version_manifest_multirelease, java_version_class_major, java_version_class_minor, java_version_class_map FROM packages INNER JOIN package_list ON packages.groupid = package_list.groupid AND packages.artifactid = package_list.artifactid AND packages.version = package_list.version";
            data = wrapper.queryPlural(chonker, rs -> {
                String groupid = rs.getString(1);
                String artifactid = rs.getString(2);
                String version = rs.getString(3);

                Id id;
                try {
                    id = new Id(groupid, artifactid, version);
                } catch (IllegalArgumentException exception) {
                    LOGGER.warn("Malformed id", exception);
                    return null;
                }

                int year = rs.getInt(4);
                if (year < 0) {
                    LOGGER.warn("Malformed release year: {} ({})", year, id);
                    year = 0;
                }

                boolean hasArchive = rs.getBoolean(5);

                boolean useMavenCompiler = rs.getBoolean(6);
                String mcpVersion = rs.getString(7);
                byte[] cArgsRaw = rs.getBytes(8);
                String cId = rs.getString(9);
                String cEncoding = rs.getString(10);
                String cSourceRaw = rs.getString(11);
                String cTargetRaw = rs.getString(12);

                boolean useModules = rs.getBoolean(13);

                String mf1Raw = rs.getString(14);
                String mf2Raw = rs.getString(15);
                String mf3Raw = rs.getString(16);
                boolean multiRelease = rs.getBoolean(17); // This field is NULL if the field is not set in the manifest, it can default to false
                byte[] majorRaw = rs.getBytes(18);
                byte[] minorRaw = rs.getBytes(19);
                byte[] classMapRaw = rs.getBytes(20);

                JavaModuleData jmd = parseJavaModuleData(id, useModules);
                JavaVersionData jvd = parseJavaVersionData(id, mf1Raw, mf2Raw, mf3Raw, multiRelease, majorRaw, minorRaw, classMapRaw);
                CompilerConfigData ccd = parseCompilerConfigData(id, useMavenCompiler, mcpVersion, cArgsRaw, cId, cEncoding, cSourceRaw, cTargetRaw);
                DataEntry entry = new DataEntry(id, hasArchive, year, jmd, jvd, ccd);
                return entry;
            });
        } catch (SQLException exception) {
            LOGGER.error("A database error occurred", exception);
            return;
        }

        long modules = data.parallelStream()
                .map(DataEntry::moduleData)
                .filter(JavaModuleData::hasModules)
                .count();
        LOGGER.info("Packages using Java modules: {} ({}%)", modules, Util.ratio(modules, archiveSize));
        
        long modulesOldJava = data.parallelStream()
                .filter(entry -> {
                    JavaVersion version = entry.versionData().versionClassCommon();
                    return version != null && version.compareTo(JavaVersion.JAVA_9) == -1;
                })
                .map(DataEntry::moduleData)
                .filter(JavaModuleData::hasModules)
                .count();
        LOGGER.info("Packages using Java modules (< Java 9): {} ({}%)", modulesOldJava, Util.ratio(modulesOldJava, archiveSize));

        File f;

        f = new File(CWD, "archive-version.csv");
        LOGGER.info("Saving version distribution to file {}", f.getAbsolutePath());
        dumpDistribution(data, DataEntry::hasArtifact, entry -> {
            JavaVersion version = entry.versionData().versionClassCommon();
            return version == null ? null : version.withoutMinorVersion();
        }, f);

        int[] years = data.stream()
                .map(DataEntry::year)
                .distinct()
                .mapToInt(Integer::intValue)
                .toArray();
        Arrays.sort(years);
        for (int year : years) {
            f = new File(CWD, "archive-version-" + year + ".csv");
            LOGGER.info("Saving version distribution (releases in {}) to file {}", year, f.getAbsolutePath());
            dumpDistribution(data, entry -> entry.hasArtifact() && entry.year() == year, entry -> {
                JavaVersion version = entry.versionData().versionClassCommon();
                return version == null ? null : version.withoutMinorVersion();
            }, f);
        }

        long multiVersion = data.stream()
                .map(DataEntry::versionData)
                .map(JavaVersionData::versionClassMap)
                .filter(x -> x != null)
                .count();
        LOGGER.info("Archives with multiple versions: {} ({}%)", multiVersion, Util.ratio(multiVersion, archiveSize));

        long multiVersionAndMultiRelease = data.stream()
                .map(DataEntry::versionData)
                .filter(jvd -> jvd.versionClassMap() != null)
                .filter(JavaVersionData::isMultiRelease)
                .count();
        LOGGER.info("Archives with multiple versions (Multi-Release: true): {} ({}%)", multiVersionAndMultiRelease, Util.ratio(multiVersionAndMultiRelease, multiVersion));

        long multiRelease = data.stream()
                .map(DataEntry::versionData)
                .filter(JavaVersionData::isMultiRelease)
                .count();
        long multiReleaseNoMultiVersion = data.stream()
                .map(DataEntry::versionData)
                .filter(JavaVersionData::isMultiRelease)
                .filter(jvd -> jvd.versionClassMap() == null)
                .count();
        LOGGER.info("Multi-Release archives without multiple versions: {} ({}%)", multiReleaseNoMultiVersion, Util.ratio(multiReleaseNoMultiVersion, multiRelease));

        Map<String, Integer> cmpMap = data.stream()
                .filter(DataEntry::hasArtifact)
                .map(entry -> {
                    JavaVersion mf = mergeManifestJavaVersion(entry);
                    JavaVersion clazz = entry.versionData().versionClassCommon();

                    return mf == null || clazz == null ? null : Map.entry(mf, clazz.withoutMinorVersion());
                })
                .filter(x -> x != null)
                .map(entry -> entry.getKey().compareTo(entry.getValue()))
                .collect(Collectors.groupingBy(x -> x < 0 ? "older" : (x > 0 ? "newer" : "same"), Collectors.summingInt(x -> 1)));
        List<Entry<String, Integer>> list = new ArrayList<>(cmpMap.entrySet());
        long jdkvAndCvPresent = list.stream()
                .map(Entry::getValue)
                .mapToInt(Integer::intValue)
                .sum();
        Comparator<Entry<String, Integer>> cmp = Entry.comparingByValue();
        list.sort(cmp.reversed());
        LOGGER.info("JDK version vs class version:");
        list.forEach(entry -> {
            LOGGER.info(" - {}: {} ({}%)", entry.getKey(), entry.getValue(), Util.ratio(entry.getValue(), jdkvAndCvPresent));
        });

        long compilerPlugin = data.stream()
                .map(DataEntry::compilerData)
                .filter(CompilerConfigData::present)
                .count();
        long noCompilerPlugin = data.size() - compilerPlugin; 
        LOGGER.info("POMs without Maven compiler plugin: {} ({}%)", noCompilerPlugin, Util.ratio(noCompilerPlugin, data.size()));

        
        f = new File(CWD, "source-encoding.csv");
        LOGGER.info("Saving Maven compiler plugin encoding distribution to file {}", f.getAbsolutePath());
        dumpDistribution(data, entry -> entry.compilerData().present(), entry -> {
            String encoding = entry.compilerData().encoding();
            if (encoding == null) {
                return null;
            }
            Matcher matcher = UTF_PATTERN.matcher(encoding);
            if (matcher.matches()) {
                encoding = matcher.group(1) + '-' + matcher.group(2);
            }
            return encoding.toUpperCase();
        }, f);

        f = new File(CWD, "compiler-id.csv");
        LOGGER.info("Saving Maven compiler plugin compiler distribution to file {}", f.getAbsolutePath());
        dumpDistribution(data, entry -> entry.compilerData().present(), entry -> entry.compilerData().id(), f);

        f = new File(CWD, "source-version.csv");
        LOGGER.info("Saving Maven compiler plugin source version distribution to file {}", f.getAbsolutePath());
        dumpDistribution(data, entry -> entry.compilerData().present(), entry -> entry.compilerData().source(), f);

        f = new File(CWD, "target-version.csv");
        LOGGER.info("Saving Maven compiler plugin target version distribution to file {}", f.getAbsolutePath());
        dumpDistribution(data, entry -> entry.compilerData().present(), entry -> entry.compilerData().target(), f);

        long onlySourceVersion = data.stream()
                .map(DataEntry::compilerData)
                .filter(CompilerConfigData::present)
                .filter(ccd -> ccd.target() == null)
                .filter(ccd -> ccd.source() != null)
                .count();
        LOGGER.info("Maven compiler plugin - packages with only source version set: {} ({}%)", onlySourceVersion, Util.ratio(onlySourceVersion, compilerPlugin));

        long onlyTargetVersion = data.stream()
                .map(DataEntry::compilerData)
                .filter(CompilerConfigData::present)
                .filter(ccd -> ccd.source() == null)
                .filter(ccd -> ccd.target() != null)
                .count();
        LOGGER.info("Maven compiler plugin - packages with only target version set: {} ({}%)", onlyTargetVersion, Util.ratio(onlyTargetVersion, compilerPlugin));

        long sourceTargetSet = data.stream()
                .map(DataEntry::compilerData)
                .filter(CompilerConfigData::present)
                .filter(ccd -> ccd.source() != null)
                .filter(ccd -> ccd.target() != null)
                .count();
        LOGGER.info("Maven compiler plugin - packages with both source and target version set: {} ({}%)", sourceTargetSet, Util.ratio(sourceTargetSet, compilerPlugin));

        long sourceTargetUnset = data.stream()
                .map(DataEntry::compilerData)
                .filter(CompilerConfigData::present)
                .filter(ccd -> ccd.source() == null)
                .filter(ccd -> ccd.target() == null)
                .count();
        LOGGER.info("Maven compiler plugin - packages with neither source and target version set: {} ({}%)", sourceTargetUnset, Util.ratio(sourceTargetUnset, compilerPlugin));

        long sourceTargetMismatch = data.stream()
                .map(DataEntry::compilerData)
                .filter(CompilerConfigData::present)
                .filter(ccd -> ccd.source() != null)
                .filter(ccd -> ccd.target() != null)
                .map(ccd -> ccd.source().equals(ccd.target()))
                .count();
        LOGGER.info("Maven compiler plugin - packages with source and target version mismatch: {} ({}% of packages with both source and target set, {}%)", sourceTargetMismatch, Util.ratio(sourceTargetMismatch, sourceTargetSet), Util.ratio(sourceTargetMismatch, compilerPlugin));

        f = new File(CWD, "command-line-args.csv");
        LOGGER.info("Saving Maven compiler plugin command line argument distribution to file {}", f.getAbsolutePath());
        dumpDistributionMultiValue(data, entry -> entry.compilerData().present(), entry -> {
            String[] array = entry.compilerData().args();
            return array == null ? Collections.emptySet() : Arrays.asList(cleanCompilerArgs(entry.id(), array));
        }, f);
    }

    @NotNull
    private static Connection getConnection(@NotNull String[] args) throws SQLException {
        // Preconditions
        Objects.requireNonNull(args);
        for (String arg : args) {
            Objects.requireNonNull(arg);
        }
        if (args.length != 3 && args.length != 5) {
            throw new IllegalArgumentException("Invalid usage");
        }

        LOGGER.info("Connecting to database");
        // This is unsafe
        return DriverManager.getConnection("jdbc:postgresql://" + args[0] + ':' + args[1] + '/' + args[2], args.length == 5 ? args[3] : null, args.length == 5 ? args[4] : null);
    }

    @NotNull
    private static JavaModuleData parseJavaModuleData(@NotNull Id context, boolean useModules) {
        // Preconditions
        Objects.requireNonNull(context);

        return new JavaModuleData(useModules);
    }

    @NotNull
    private static JavaVersionData parseJavaVersionData(@NotNull Id context, @Nullable String mf1Raw, @Nullable String mf2Raw, @Nullable String mf3Raw, boolean multiRelease, @Nullable byte[] majorRaw, @Nullable byte[] minorRaw, @Nullable byte[] classMapRaw) {
        // Preconditions
        Objects.requireNonNull(context);

        JavaVersion mf1 = mf1Raw == null ? null : JavaVersion.fromString(mf1Raw)
                .orElse(null);
        JavaVersion mf2 = mf2Raw == null ? null : JavaVersion.fromString(mf2Raw)
                .orElse(null);
        JavaVersion mf3 = mf3Raw == null ? null : JavaVersion.fromString(mf3Raw)
                .orElse(null);

        Optional<? extends JavaVersion> optional = Optional.empty();
        if ((majorRaw == null ^ minorRaw == null) || (majorRaw != null && (majorRaw.length != 2 || minorRaw.length != 2))) {
            LOGGER.warn("Malformed class version bytes: {}, {} ({})", Arrays.toString(majorRaw), Arrays.toString(minorRaw), context);
        }
        else if (majorRaw != null) {
            optional = JavaVersion.fromClassVersion(majorRaw, minorRaw);
        }

        JavaVersion classVersion = optional.orElse(null);

        Map<JavaVersion, Integer> classMap = classMapRaw == null ? null : classVersionMapFromBytes(context, classMapRaw);
        return new JavaVersionData(mf1, mf2, mf3, multiRelease, classVersion, classMap);
    }

    @NotNull
    private static CompilerConfigData parseCompilerConfigData(@NotNull Id context, boolean useMavenCompiler, @Nullable String mcpVersion, @Nullable byte[] cArgsRaw, @Nullable String cId, @Nullable String cEncoding, @Nullable String cSourceRaw, @Nullable String cTargetRaw) {
        // Preconditions
        Objects.requireNonNull(context);

        String[] cArgs = cArgsRaw == null ? null : compilerArgsFromBytes(context, cArgsRaw);
        JavaVersion cSource = cSourceRaw == null ? null : JavaVersion.fromString(cSourceRaw).orElseGet(() -> {
            LOGGER.warn("Malformed compiler source version {} ({})", cSourceRaw, context);
            return null;
        });
        JavaVersion cTarget = cTargetRaw == null ? null : JavaVersion.fromString(cTargetRaw).orElseGet(() -> {
            LOGGER.warn("Malformed compiler target version {} ({})", cTargetRaw, context);
            return null;
        });

        return new CompilerConfigData(useMavenCompiler, mcpVersion, cArgs, cId, cEncoding, cSource, cTarget);
    }

    @NotNull
    private static String[] cleanCompilerArgs(@NotNull Id context, @NotNull String[] args) {
        // Preconditions
        Objects.requireNonNull(context);
        Objects.requireNonNull(args);
        for (String arg : args) {
            Objects.requireNonNull(arg);
        }

        return Arrays.stream(args)
                .map(string -> string.split("\\s+"))
                .flatMap(Arrays::stream)
                .map(string -> {
                    // @<filename>
                    if (string.startsWith("@")) {
                        return "@";
                    }

                    // -A<key>[=<value>]
                    else if (string.startsWith("-A")) {
                        return "-A";
                    }

                    // -J<option>
                    else if (string.startsWith("-J")) {
                        return "-J";
                    }

                    // Default
                    else {
                        return string;
                    }
                })
                .map(string -> {
                    int i = string.indexOf('=');
                    int j = string.indexOf(':');

                    // No key-value flag
                    if (i == -1 && j == -1) {
                        return string;
                    }

                    // <key>=<value>
                    if (i != -1 && j == -1) {
                        return string.substring(0, i);
                    }

                    // <key>:<value>
                    if (i == -1 && j != -1) {
                        return string.substring(0, j);
                    }

                    // Both '=' and ':'
                    else {
                        return string.substring(0, Math.min(i, j));
                    }
                })
                .filter(string -> string.startsWith("-"))
                .toArray(String[]::new);
    }

    @Nullable
    private static String[] compilerArgsFromBytes(@NotNull Id context, @NotNull byte[] bytes) {
        // Preconditions
        Objects.requireNonNull(context);
        Objects.requireNonNull(bytes);

        try {
            return Util.readBytes(bytes, in -> {
                List<String> list = new ArrayList<>();
                while (in.available() > 0) {
                    list.add(in.readUTF());
                }
                return list.toArray(String[]::new);
            });
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Malformed compiler arguments ({})", context, exception);
            return null;
        }
    }

    @Nullable
    private static Map<JavaVersion, Integer> classVersionMapFromBytes(@NotNull Id context, @NotNull byte[] bytes) {
        // Preconditions
        Objects.requireNonNull(context);
        Objects.requireNonNull(bytes);

        try {
            return Util.readBytes(bytes, in -> {
                Map<JavaVersion, Integer> map = new HashMap<>();
                while (in.available() > 0) {
                    byte[] major = in.readNBytes(2);
                    byte[] minor = in.readNBytes(2);
                    int num = in.readInt();
    
                    if (num < 0) {
                        LOGGER.warn("Invalid occurrence count: {} ({})", num, context);
                        continue;
                    }
    
                    JavaVersion.fromClassVersion(major, minor)
                            .ifPresent(jv -> {
                                if (map.putIfAbsent(jv, num) != null) {
                                    LOGGER.warn("Duplicate java version in map: {} ({})", jv, context);
                                }
                            });
                }
                return map;
            });
        } catch (IllegalArgumentException exception) {
            LOGGER.warn("Malformed version map ({})", context, exception);
            return null;
        }
    }

    @Nullable
    private static JavaVersion mergeManifestJavaVersion(@NotNull DataEntry entry) {
        // Preconditions
        Objects.requireNonNull(entry);

        JavaVersionData jvd = entry.versionData();
        JavaVersion v1 = jvd.versionCreatedBy();
        JavaVersion v2 = jvd.versionBuildJdk();
        JavaVersion v3 = jvd.versionBuildJdkSpec();

        boolean[] naggable = {true}; // Jank
        return Arrays.stream(new JavaVersion[] {v1, v2, v3})
                .filter(x -> x != null)
                .map(JavaVersion::withoutMinorVersion)
                .reduce((ver1, ver2) -> {
                    if (ver1.equals(ver2)) {
                        return ver1;
                    }
                    if (naggable[0]) {
                        naggable[0] = false;
                        LOGGER.warn("Java version mismatch in manifest ({})", entry.id());
                    }
                    return ver1.compareTo(ver2) > 0 ? ver1 : ver2;
                })
                .orElse(null);
    }

    private static <T extends Comparable<T>> void dumpDistribution(@NotNull Collection<? extends DataEntry> data, @NotNull Predicate<? super DataEntry> predicate, @NotNull Function<? super DataEntry, ? extends T> classifier, @Nullable File file) {
        // Preconditions
        Objects.requireNonNull(data);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(classifier);

        dumpDistributionMultiValue(data, predicate, classifier.andThen(Collections::singleton), file);
    }

    private static <T extends Comparable<T>> void dumpDistributionMultiValue(@NotNull Collection<? extends DataEntry> data, @NotNull Predicate<? super DataEntry> predicate, @NotNull Function<? super DataEntry, ? extends Collection<? extends T>> classifier, @NotNull File file) {
        // Preconditions
        Objects.requireNonNull(data);
        Objects.requireNonNull(predicate);
        Objects.requireNonNull(classifier);

        // This entire method is inefficient

        // Workaround because Collectors#groupingBy(...) does not allow null keys
        Map<T, Integer> map = Util.nullSafeGroupByAndCount(data.parallelStream()
                .filter(predicate)
                .flatMap(classifier.andThen(Collection::stream))
                , Function.identity());

        List<Entry<T, Integer>> list = new ArrayList<>(map.entrySet());
        Comparator<Entry<T, Integer>> cmp = Entry.comparingByValue();
        list.sort(cmp.reversed());

        long size = data.stream()
                .filter(predicate)
                .count();

        try (OutputStream out = new FileOutputStream(file); CSVPrinter printer = new CSVPrinter(new OutputStreamWriter(out, StandardCharsets.UTF_8), CSVFormat.EXCEL)) {
            for (Entry<T, Integer> entry : list) {
                printer.print(entry.getKey());
                printer.print(entry.getValue());
                printer.print(Util.ratio(entry.getValue(), size));
                printer.println();
            }
        } catch (IOException exception) {
            LOGGER.error("Could not write to file {}", file.getAbsolutePath(), exception);
        }
    }
}
