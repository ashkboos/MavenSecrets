package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A data entry.
 */
public record DataEntry(@NotNull Id id, boolean hasArtifact, int year, @NotNull JavaModuleData moduleData, @NotNull JavaVersionData versionData, @NotNull CompilerConfigData compilerData) {

    /**
     * Create an entry.
     *
     * @param id Artifact id.
     * @param hasArtifact If the artifact has an openable archive.
     * @param year The release year of the artifact.
     * @param moduleData Java module data.
     * @param versionData Java version data.
     * @param compilerData Compiler configuration data.
     */
    public DataEntry(@NotNull Id id, boolean hasArtifact, int year, @NotNull JavaModuleData moduleData, @NotNull JavaVersionData versionData, @NotNull CompilerConfigData compilerData) {
        this.id = Objects.requireNonNull(id);
        this.hasArtifact = hasArtifact;
        this.year = year;
        this.moduleData = Objects.requireNonNull(moduleData);
        this.versionData = Objects.requireNonNull(versionData);
        this.compilerData = Objects.requireNonNull(compilerData);
        if (year < 0) {
            throw new IllegalArgumentException("Invalid year: " + year);
        }
    }

    /**
     * An artifact id.
     */
    public static record Id(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {

        private static final Pattern COMPONENT_PATTERN = Pattern.compile("^[^: ]+$");

        /**
         * Create an id instance.
         *
         * @param groupId The group id.
         * @param artifactId The artifact id.
         * @param version The version.
         */
        public Id(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {
            this.groupId = Objects.requireNonNull(groupId);
            this.artifactId = Objects.requireNonNull(artifactId);
            this.version = Objects.requireNonNull(version);

            if (!COMPONENT_PATTERN.matcher(groupId).matches()) {
                throw new IllegalArgumentException("Invalid group id: " + groupId);
            }
            if (!COMPONENT_PATTERN.matcher(artifactId).matches()) {
                throw new IllegalArgumentException("Invalid artifact id: " + artifactId);
            }
            if (!COMPONENT_PATTERN.matcher(version).matches()) {
                throw new IllegalArgumentException("Invalid version: " + version);
            }
        }

        @NotNull
        @Override
        public String toString() {
            return new StringBuilder().append(groupId()).append(':').append(artifactId()).append(':').append(version()).toString();
        }
    }

    /**
     * Java module data.
     */
    public static record JavaModuleData(boolean hasModules) {
        // Nothing
    }

    /**
     * Java version data.
     */
    public static record JavaVersionData(@Nullable JavaVersion versionCreatedBy, @Nullable JavaVersion versionBuildJdk, @Nullable JavaVersion versionBuildJdkSpec, @Nullable Boolean isMultiRelease, @Nullable JavaVersion versionClassCommon, @Nullable Map<JavaVersion, Integer> versionClassMap) {

        /**
         * Create a data instance.
         *
         * @param versionCreatedBy The version from the <code>Created-By</code> manifest entry.
         * @param versionBuildJdk The version from the <code>Build-Jdk</code> manifest entry.
         * @param versionBuildJdkSpec The version from the <code>Build-Jdk-Spec</code> manifest entry.
         * @param isMultiRelease If this is a multi-release jar from the <code>Multi-Release</code> manifest entry.
         * @param versionClassCommon The most common <code>class</code> version.
         * @param versionClassMap The class version distribution.
         */
        public JavaVersionData(@Nullable JavaVersion versionCreatedBy, @Nullable JavaVersion versionBuildJdk, @Nullable JavaVersion versionBuildJdkSpec, @Nullable Boolean isMultiRelease, @Nullable JavaVersion versionClassCommon, @Nullable Map<JavaVersion, Integer> versionClassMap) {
            if (versionClassMap != null && !Util.isValidVersionMap(versionClassMap)) {
                throw new IllegalArgumentException("Invalid version map: " + versionClassMap);
            }
            this.versionCreatedBy = versionCreatedBy;
            this.versionBuildJdk = versionBuildJdk;
            this.versionBuildJdkSpec = versionBuildJdkSpec;
            this.isMultiRelease = isMultiRelease;
            this.versionClassCommon = versionClassCommon;
            this.versionClassMap = versionClassMap == null ? null : new HashMap<>(versionClassMap);
        }

        @Nullable
        @Override
        public Map<JavaVersion, Integer> versionClassMap() {
            return versionClassMap == null ? null : Collections.unmodifiableMap(versionClassMap);
        }
    }

    /**
     * Compiler configuration data.
     */
    public static record CompilerConfigData(boolean present, @NotNull String version, @Nullable String[] args, @Nullable String id, @Nullable String encoding, @Nullable JavaVersion source, @Nullable JavaVersion target) {

        /**
         * Create a data instance.
         *
         * @param present If the Maven compiler plugin is present.
         * @param version The Maven compiler plugin version.
         * @param args The compiler arguments taken from <code>compilerArgs</code>.
         * @param id The compiler id taken from <code>compilerId</code>.
         * @param encoding The source encoding taken from <code>encoding</code>.
         * @param source The source version taken from <code>source</code>.
         * @param target The target version taken from <code>target</code>.
         */
        public CompilerConfigData(boolean present, @Nullable String version, @Nullable String[] args, @Nullable String id, @Nullable String encoding, @Nullable JavaVersion source, @Nullable JavaVersion target) {
            this.present = present;
            this.version = version;
            this.args = args;
            this.id = id;
            this.encoding = encoding;
            this.source = source;
            this.target = target;
            if (args != null) {
                for (String arg : args) {
                    Objects.requireNonNull(arg);
                }
            }
        }
    }
}
