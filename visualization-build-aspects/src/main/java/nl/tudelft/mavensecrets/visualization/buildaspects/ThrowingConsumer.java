package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link Consumer} that can throw a checked {@link Exception}.
 *
 * @param <T> Input type.
 * @param <U> Exception type.
 */
@FunctionalInterface
public interface ThrowingConsumer<T, U extends Throwable> {

    /**
     * Perform an operation on the input.
     *
     * @param t Object to accept.
     * @throws U If an error occurs.
     */
    void accept(@Nullable T t) throws U;

    /**
     * Append another {@link Consumer}.
     *
     * @param after Other consumer.
     * @return The joined consumer.
     * @see #andThen(ThrowingConsumer)
     */
    @NotNull
    default ThrowingConsumer<T, U> andThen(@NotNull Consumer<? super T> after) {
        // Preconditions
        Objects.requireNonNull(after);

        return t -> {
            accept(t);
            after.accept(t);
        };
    }

    /**
     * Append another {@link ThrowingConsumer}.
     *
     * @param after Other consumer.
     * @return The joined consumer.
     * @see #andThen(Consumer)
     */
    @NotNull
    default ThrowingConsumer<T, U> andThen(@NotNull ThrowingConsumer<? super T, ? extends U> after) {
        // Preconditions
        Objects.requireNonNull(after);

        return t -> {
            accept(t);
            after.accept(t);
        };
    }
}
