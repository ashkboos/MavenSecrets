package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.io.ByteArrayInputStream;
import java.io.DataInput;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Stream;

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
    public static Map<JavaVersion, Integer> mergeMajorVersions(@NotNull Map<? extends JavaVersion, ? extends Integer> map) {
        // Preconditions
        Objects.requireNonNull(map);

        Map<JavaVersion, Integer> result = new HashMap<>();
        for (Entry<? extends JavaVersion, ? extends Integer> entry : map.entrySet()) {
            // Not expecting any overflows but if there are any we will know
            result.merge(entry.getKey() == null ? null : entry.getKey().withoutMinorVersion(), entry.getValue(), Math::addExact);
        }
        return result;
    }

    @NotNull
    public static <T> Comparator<? super T> nullsLast(@NotNull Comparator<? super T> comparator) {
        // Preconditions
        Objects.requireNonNull(comparator);

        return (t1, t2) -> {
            if (t1 == t2) {
                return 0;
            }
            if (t1 == null) {
                return 1;
            }
            if (t2 == null) {
                return -1;
            }
            return comparator.compare(t1, t2);
        };
    }

    @NotNull
    public static <T, U> Map<U, Integer> nullSafeGroupByAndCount(@NotNull Stream<? extends T> stream, @NotNull Function<? super T, ? extends U> classifier) {
        // Preconditions
        Objects.requireNonNull(stream);
        Objects.requireNonNull(classifier);

        return stream.collect(HashMap::new, (map, t) -> {
            U u = classifier.apply(t);
            synchronized (map) {
                map.merge(u, 1, Math::addExact);
            }
        }, (map1, map2) -> {
            synchronized (map1) {
                synchronized (map2) {
                    for (Entry<U, Integer> entry : map2.entrySet()) {
                        map1.merge(entry.getKey(), entry.getValue(), Math::addExact);
                    }
                }
            }
        });
    }

    @NotNull
    public static double ratio(long a, long b) {
        return ((double) Math.round((((double) a) / b) * 10000D)) / 100D;
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
