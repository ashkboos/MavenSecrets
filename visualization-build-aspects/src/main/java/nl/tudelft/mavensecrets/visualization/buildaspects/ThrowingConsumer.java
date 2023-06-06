package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.util.Objects;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@FunctionalInterface
public interface ThrowingConsumer<T, U extends Throwable> {
    void accept(@Nullable T t) throws U;

    @NotNull
    default ThrowingConsumer<T, U> andThen(@NotNull Consumer<? super T> after) {
        // Preconditions
        Objects.requireNonNull(after);

        return t -> {
            accept(t);
            after.accept(t);
        };
    }

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
