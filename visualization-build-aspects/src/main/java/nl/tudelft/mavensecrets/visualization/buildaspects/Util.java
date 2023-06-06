package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Util {

    private Util() {
        // Nothing
    }

    public static boolean isValidVersionMap(@Nullable Map<? extends JavaVersion, ? extends Integer> map) {
        if (map == null) {
            return false;
        }

        for (Entry<? extends JavaVersion, ? extends Integer> entry : map.entrySet()) {
            if (entry.getKey() == null) {
                return false;
            }
            if (entry.getValue() == null) {
                return false;
            }
            if (entry.getValue().intValue() < 0) {
                return false;
            }
        }
        return true;
    }

    @NotNull
    public Map<? extends JavaVersion, ? extends Integer> mergeMajorVersions(@NotNull Map<? extends JavaVersion, ? extends Integer> map) {
        // Preconditions
        Objects.requireNonNull(map);

        Map<JavaVersion, Integer> result = new HashMap<>();
        for (Entry<? extends JavaVersion, ? extends Integer> entry : map.entrySet()) {
            // Not expecting any overflows but if there are any we will know
            result.merge(entry.getKey().withoutMinorVersion(), entry.getValue(), Math::addExact);
        }
        return result;
    }

    @NotNull
    public static double ratio(int a, int b) {
        return ((double) Math.round((((double) a) / b) * 100D));
    }

    public static short shortFromBytes(@NotNull byte[] bytes) {
        return readBytes(bytes, DataInput::readShort);
    }

    public static int intFromBytes(@NotNull byte[] bytes) {
        return readBytes(bytes, DataInput::readInt);
    }

    @Nullable
    public static <T> T readBytes(@NotNull byte[] bytes, @NotNull ThrowingFunction<DataInputStream, T, IOException> function) {
        // Preconditions
        Objects.requireNonNull(bytes);
        Objects.requireNonNull(function);

        T t;
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            t = function.apply(in);
            if (in.available() != 0) {
                throw new IllegalArgumentException("Trailing data");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Malformed data", exception);
        }
        return t;
    }
}
