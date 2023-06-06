package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record DataEntry(@NotNull Id id, boolean hasArtifact, int year, @NotNull JavaModuleData moduleData, @NotNull JavaVersionData versionData, @NotNull CompilerConfigData compilerData) {

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

    public static record Id(@NotNull String groupId, @NotNull String artifactId, @NotNull String version) {

        private static final Pattern COMPONENT_PATTERN = Pattern.compile("^[^: ]+$");

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

    public static record JavaModuleData(boolean hasModules) {
        // Nothing
    }

    public static record JavaVersionData(@Nullable JavaVersion versionCreatedBy, @Nullable JavaVersion versionBuildJdk, @Nullable JavaVersion versionBuildJdkSpec, @Nullable Boolean isMultiRelease, @Nullable JavaVersion versionClassCommon, @Nullable Map<JavaVersion, Integer> versionClassMap) {

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

    public static record CompilerConfigData(boolean present, @NotNull String version, @Nullable String[] args, @Nullable String id, @Nullable String encoding, @Nullable JavaVersion source, @Nullable JavaVersion target) {

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
