package nl.tudelft.mavensecrets.visualization.buildaspects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ThrowingFunction<T, U, V extends Throwable> {

    @Nullable
    U apply(@Nullable T t) throws V;

    @NotNull
    static <T, U extends Throwable> ThrowingFunction<T, T, U> identity() {
        return t -> t;
    }
}
