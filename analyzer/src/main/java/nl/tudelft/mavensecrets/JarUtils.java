package nl.tudelft.mavensecrets;

import java.util.Arrays;
import java.util.Optional;

public final class JarUtils {
    public static Optional<String> packageFromPath(String path) {
        if (!path.toLowerCase().endsWith(".class") || path.contains(" "))
            return Optional.empty();

        var split = path.split("/");
        return Arrays.stream(split).limit(split.length - 1).reduce((i, j) -> i + "." + j);
    }
}
