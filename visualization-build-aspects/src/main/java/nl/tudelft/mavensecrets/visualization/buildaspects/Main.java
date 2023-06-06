package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.CompilerConfigData;
import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.Id;
import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.JavaModuleData;
import nl.tudelft.mavensecrets.visualization.buildaspects.DataEntry.JavaVersionData;

public class Main {

    /*
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

    private static final Logger LOGGER = LogManager.getLogger(Main.class);

    static {
        // Legacy driver registring because Maven shade does funny things
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    @SuppressWarnings("unused")
    public static void main(@NotNull String[] args) {
        // Usage
        if (args.length != 3 && args.length != 5) {
            LOGGER.error("Usage: <hostname> <port> <database> [<username> <password>]");
            return;
        }

        List<? extends DataEntry> data;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(getConnection(args))) {
            int indexSize = wrapper.querySingular("SELECT COUNT(*) FROM package_list", rs -> rs.getInt(1));
            LOGGER.info("Indexed packages: {}", indexSize);

            int sampleSize = wrapper.querySingular("SELECT COUNT(*) FROM selected_packages", rs -> rs.getInt(1));
            LOGGER.info("Selected packages: {} ({}%)", sampleSize, Util.ratio(sampleSize, indexSize));

            int successSize = wrapper.querySingular("SELECT COUNT(*) FROM packages", rs -> rs.getInt(1));
            int failSize = sampleSize - successSize;
            LOGGER.info("Selected packages (resolution fail): {} ({}%)", failSize, Util.ratio(failSize, sampleSize));
            LOGGER.info("Selected packages (resolution success): {} ({}%)", successSize, Util.ratio(successSize, sampleSize));

            int archiveSize = wrapper.querySingular("SELECT COUNT(*) FROM packages WHERE has_artifact = true", rs -> rs.getInt(1));
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

        // TODO: Data extraction
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
        JavaVersion cSource = cSourceRaw == null ? null : JavaVersion.fromString(cSourceRaw, false).orElseGet(() -> {
            LOGGER.warn("Malformed compiler source version {} ({})", cSourceRaw, context);
            return null;
        });
        JavaVersion cTarget = cTargetRaw == null ? null : JavaVersion.fromString(cTargetRaw, false).orElseGet(() -> {
            LOGGER.warn("Malformed compiler target version {} ({})", cTargetRaw, context);
            return null;
        });

        return new CompilerConfigData(useMavenCompiler, mcpVersion, cArgs, cId, cEncoding, cSource, cTarget);
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
}
