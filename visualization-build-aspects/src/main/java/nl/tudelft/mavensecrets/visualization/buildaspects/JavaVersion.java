package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Java version class.
 */
public class JavaVersion implements Comparable<JavaVersion> {

    private static final Map<Short, JavaVersion> MAJOR_VERSION_MAP = new HashMap<>();
    private static final Pattern NAME_PATTERN = Pattern.compile("^(\\d+)(\\.\\d+)?(\\.\\d+)?(_\\d+)?( \\(.+\\))?$");

    /**
     * JDK 1.0
     *
     * @deprecated Older versions do not have distinct class versions.
     */
    @Deprecated
    public static final JavaVersion JAVA_0 = new JavaVersion("JDK 1.0", (short) 0x2D);

    /**
     * JDK 1.1
     *
     * @deprecated Older versions do not have distinct class versions.
     */
    @Deprecated
    public static final JavaVersion JAVA_1 = new JavaVersion("JDK 1.1", (short) 0x2D);

    /**
     * J2SE 1.2
     */
    public static final JavaVersion JAVA_2 = new JavaVersion("J2SE 1.2", (short) 0x2E);

    /**
     * J2SE 1.3
     */
    public static final JavaVersion JAVA_3 = new JavaVersion("J2SE 1.3", (short) 0x2F);

    /**
     * J2SE 1.4
     */
    public static final JavaVersion JAVA_4 = new JavaVersion("J2SE 1.4", (short) 0x30);

    /**
     * Java SE 1.5
     */
    public static final JavaVersion JAVA_5 = new JavaVersion("Java SE 5", (short) 0x31);

    /**
     * Java SE 1.6
     */
    public static final JavaVersion JAVA_6 = new JavaVersion("Java SE 6", (short) 0x32);

    /**
     * Java SE 1.7
     */
    public static final JavaVersion JAVA_7 = new JavaVersion("Java SE 7", (short) 0x33);

    /**
     * Java SE 1.8
     */
    public static final JavaVersion JAVA_8 = new JavaVersion("Java SE 8", (short) 0x34, true);

    /**
     * Java SE 9
     */
    public static final JavaVersion JAVA_9 = new JavaVersion("Java SE 9", (short) 0x35);

    /**
     * Java SE 10
     */
    public static final JavaVersion JAVA_10 = new JavaVersion("Java SE 10", (short) 0x36);

    /**
     * Java SE 11
     */
    public static final JavaVersion JAVA_11 = new JavaVersion("Java SE 11", (short) 0x37, true);

    /**
     * Java SE 12
     */
    public static final JavaVersion JAVA_12 = new JavaVersion("Java SE 12", (short) 0x38);

    /**
     * Java SE 13
     */
    public static final JavaVersion JAVA_13 = new JavaVersion("Java SE 13", (short) 0x39);

    /**
     * Java SE 14
     */
    public static final JavaVersion JAVA_14 = new JavaVersion("Java SE 14", (short) 0x3A);

    /**
     * Java SE 15
     */
    public static final JavaVersion JAVA_15 = new JavaVersion("Java SE 15", (short) 0x3B);

    /**
     * Java SE 16
     */
    public static final JavaVersion JAVA_16 = new JavaVersion("Java SE 16", (short) 0x3C);

    /**
     * Java SE 17
     */
    public static final JavaVersion JAVA_17 = new JavaVersion("Java SE 17", (short) 0x3D, true);

    /**
     * Java SE 18
     */
    public static final JavaVersion JAVA_18 = new JavaVersion("Java SE 18", (short) 0x3E);

    /**
     * Java SE 19
     */
    public static final JavaVersion JAVA_19 = new JavaVersion("Java SE 19", (short) 0x3F);

    /**
     * Java SE 20
     */
    public static final JavaVersion JAVA_20 = new JavaVersion("Java SE 20", (short) 0x40);

    /**
     * Java SE 21
     */
    public static final JavaVersion JAVA_21 = new JavaVersion("Java SE 21", (short) 0x41, true);

    private final String name;
    private final short major;
    private final Short minor;
    private final boolean lts;

    private JavaVersion(@NotNull String name, short major) {
        this(name, major, false);
    }

    private JavaVersion(@NotNull String name, short major, boolean lts) {
        this.name = Objects.requireNonNull(name);
        this.major = major;
        this.minor = null;
        this.lts = lts;

        MAJOR_VERSION_MAP.put(major, this);
    }

    private JavaVersion(@NotNull JavaVersion parent, short minor) {
        Objects.requireNonNull(parent);

        this.name = parent.getName();
        this.major = parent.getMajorVersion();
        this.minor = minor;
        this.lts = parent.isLongTermSupportVersion();
    }

    /**
     * Get the name of the version.
     *
     * @return The name.
     */
    @NotNull
    public String getName() {
        return name;
    }

    /**
     * Get the major version number.
     * Note that this is an unsigned short.
     *
     * @return The major version.
     */
    public short getMajorVersion() {
        return major;
    }

    /**
     * Get if the version has a minor version number.
     *
     * @return If there is a minor version.
     * @see #getMinorVersion()
     */
    public boolean hasMinorVersion() {
        return minor != null;
    }

    /**
     * Get the minor version number.
     * Note that this is an unsigned short.
     *
     * @return The minor version.
     * @throws IllegalStateException If no minor version is set.
     * @see #hasMinorVersion()
     */
    public short getMinorVersion() {
        // Preconditions
        if (!hasMinorVersion()) {
            throw new IllegalStateException("No minor version");
        }

        return minor.shortValue();
    }

    /**
     * Get if the version is a long-term support version.
     *
     * @return If the version is a long-term support version.
     */
    public boolean isLongTermSupportVersion() {
        return lts;
    }

    /**
     * Get the version with a minor version.
     *
     * @param minor Minor version.
     * @return The version.
     */
    @NotNull
    public JavaVersion withMinorVersion(short minor) {
        return new JavaVersion(this, minor);
    }

    /**
     * Get the version without a minor version.
     *
     * @return The version.
     */
    @NotNull
    public JavaVersion withoutMinorVersion() {
        return MAJOR_VERSION_MAP.get(getMajorVersion());
    }

    @Override
    public int compareTo(@NotNull JavaVersion other) {
        // Preconditions
        Objects.requireNonNull(other);

        int result = 0;
        result = Short.compare(major, other.getMajorVersion());
        if (result == 0) {
            boolean hasMinor = hasMinorVersion();
            boolean otherHasMinor = other.hasMinorVersion();

            if (hasMinor) {
                result = otherHasMinor ? Short.compareUnsigned(getMinorVersion(), other.getMinorVersion()) : 1;
            } else {
                result = otherHasMinor ? -1 : 0;
            }
        }
        return result;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minor, major);
    }

    @Override
    public boolean equals(@Nullable Object object) {
        if (object instanceof JavaVersion other) {
            return name.equals(other.getName())
                    && major == other.getMajorVersion()
                    && Objects.equals(minor, other.hasMinorVersion() ? other.getMinorVersion() : null)
                    && lts == other.isLongTermSupportVersion();
        }
        return false;
    }

    @NotNull
    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder()
                .append(name);

        if (hasMinorVersion()) {
            builder
                .append(" (minor: ")
                .append(getMinorVersion())
                .append(')');
        }

        return builder.toString();
    }

    /**
     * Get the version from class header bytes.
     *
     * @param major Major version bytes.
     * @param minor Minor version bytes.
     * @return the version wrapped in an {@link Optional}.
     */
    @NotNull
    public static Optional<? extends JavaVersion> fromClassVersion(@NotNull byte[] major, @NotNull byte[] minor) {
        // Preconditions
        Objects.requireNonNull(major);
        Objects.requireNonNull(minor);
        if (major.length != 2) {
            throw new IllegalArgumentException("Invalid major version length: " + major.length);
        }
        if (minor.length != 2) {
            throw new IllegalArgumentException("Invalid minor version length: " + minor.length);
        }

        short majorNum = Util.shortFromBytes(major);
        short minorNum = Util.shortFromBytes(minor);

        JavaVersion majorVersion = MAJOR_VERSION_MAP.get(majorNum);

        return majorVersion == null ? Optional.empty() : Optional.of(new JavaVersion(majorVersion, minorNum));
    }

    /**
     * Get the version from a string.
     * The string is formatted as <code>major(.minor)?(.micro)?(_patch)?( \(text\))?</code>.
     *
     * @param string Input string.
     * @return the version wrapped in an {@link Optional}.
     */
    @NotNull
    public static Optional<? extends JavaVersion> fromString(@NotNull String string) {
        // Preconditions
        Objects.requireNonNull(string);

        // major(.minor)?(.micro)?(_patch)?( \(text\))?

        Matcher matcher = NAME_PATTERN.matcher(string);
        if (matcher.matches()) {
            String majorArg1 = matcher.group(1);
            String majorArg2 = matcher.group(2);
            String majorArg = majorArg2 != null && !majorArg2.equals(".0") ? (majorArg1 + majorArg2) : majorArg1;

            JavaVersion majorVersion;
            switch (majorArg) {
                case "1": // 1.0 but .0 gets cut off
                    majorVersion = JavaVersion.JAVA_0;
                    break;
                case "1.1":
                    majorVersion = JavaVersion.JAVA_1;
                    break;
                case "1.2":
                case "2":
                    majorVersion = JavaVersion.JAVA_2;
                    break;
                case "1.3":
                case "3":
                    majorVersion = JavaVersion.JAVA_3;
                    break;
                case "1.4":
                case "4":
                    majorVersion = JavaVersion.JAVA_4;
                    break;
                case "1.5":
                case "5":
                    majorVersion = JavaVersion.JAVA_5;
                    break;
                case "1.6":
                case "6":
                    majorVersion = JavaVersion.JAVA_6;
                    break;
                case "1.7":
                case "7":
                    majorVersion = JavaVersion.JAVA_7;
                    break;
                case "1.8":
                case "8":
                    majorVersion = JavaVersion.JAVA_8;
                    break;
                case "1.9":
                case "9":
                    majorVersion = JavaVersion.JAVA_9;
                    break;
                case "1.10":
                case "10":
                    majorVersion = JavaVersion.JAVA_10;
                    break;
                case "1.11":
                case "11":
                    majorVersion = JavaVersion.JAVA_11;
                    break;
                case "1.12":
                case "12":
                    majorVersion = JavaVersion.JAVA_12;
                    break;
                case "1.13":
                case "13":
                    majorVersion = JavaVersion.JAVA_13;
                    break;
                case "1.14":
                case "14":
                    majorVersion = JavaVersion.JAVA_14;
                    break;
                case "1.15":
                case "15":
                    majorVersion = JavaVersion.JAVA_15;
                    break;
                case "1.16":
                case "16":
                    majorVersion = JavaVersion.JAVA_16;
                    break;
                case "1.17":
                case "17":
                    majorVersion = JavaVersion.JAVA_17;
                    break;
                case "1.18":
                case "18":
                    majorVersion = JavaVersion.JAVA_18;
                    break;
                case "1.19":
                case "19":
                    majorVersion = JavaVersion.JAVA_19;
                    break;
                case "1.20":
                case "20":
                    majorVersion = JavaVersion.JAVA_20;
                    break;
                case "1.21":
                case "21":
                    majorVersion = JavaVersion.JAVA_21;
                    break;
                default:
                    return Optional.empty();
            }

            String minorArg = matcher.group(4);
            if (minorArg == null) {
                return Optional.of(majorVersion);
            }

            try {
                short minorNum = Short.parseShort(minorArg.substring(1));
                return Optional.of(majorVersion.withMinorVersion(minorNum));
            } catch (NumberFormatException exception) {
                // Fall-through
            }
        }
        return Optional.empty();
    }
}
