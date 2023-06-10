package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.util.Objects;
import java.util.function.Function;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link Function} that can throw a checked {@link Exception}.
 *
 * @param <T> Input type.
 * @param <U> Output type.
 * @param <V> Exception type.
 */
@FunctionalInterface
public interface ThrowingFunction<T, U, V extends Throwable> {

    /**
     * Apply the function on the input.
     *
     * @param t Object to accept.
     * @return The output.
     * @throws V If an error occurs.
     */
    @Nullable
    U apply(@Nullable T t) throws V;

    /**
     * Prepend another function.
     *
     * @param <W> Input type.
     * @param before Other function.
     * @return The joined function.
     * @see #compose(ThrowingFunction)
     */
    @NotNull
    default <W> ThrowingFunction<W, U, V> compose(@NotNull Function<? super W, ? extends T> before) {
        // Preconditions
        Objects.requireNonNull(before);

        return w -> apply(before.apply(w));
    }

    /**
     * Prepend another function.
     *
     * @param <W> Input type.
     * @param before Other function.
     * @return The joined function.
     * @see #compose(Function)
     */
    @NotNull
    default <W> ThrowingFunction<W, U, V> compose(@NotNull ThrowingFunction<? super W, ? extends T, ? extends V> before) {
        // Preconditions
        Objects.requireNonNull(before);

        return w -> apply(before.apply(w));
    }

    /**
     * Append another function.
     *
     * @param <W> Output type.
     * @param after Other function.
     * @return The joined function.
     * @see #andThen(ThrowingFunction)
     */
    @NotNull
    default <W> ThrowingFunction<T, W, V> andThen(@NotNull Function<? super U, ? extends W> after) {
        // Preconditions
        Objects.requireNonNull(after);

        return t -> after.apply(apply(t));
    }

    /**
     * Append another function.
     *
     * @param <W> Output type.
     * @param after Other function.
     * @return The joined function.
     * @see #andThen(Function)
     */
    @NotNull
    default <W> ThrowingFunction<T, W, V> andThen(@NotNull ThrowingFunction<? super U, ? extends W, ? extends V> after) {
        // Preconditions
        Objects.requireNonNull(after);

        return t -> after.apply(apply(t));
    }

    /**
     * Get the identity function.
     *
     * @param <T> Input type.
     * @param <U> Exception type.
     * @return The function.
     */
    @NotNull
    static <T, U extends Throwable> ThrowingFunction<T, T, U> identity() {
        return t -> t;
    }
}
