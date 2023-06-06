package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * A {@link Connection} wrapper to make queries easier.
 */
public class ConnectionWrapper implements AutoCloseable {

    private final Connection connection;
    private boolean closed = false;

    /**
     * Create a wrapper instance.
     *
     * @param connection Target connection.
     */
    public ConnectionWrapper(@NotNull Connection connection) {
        this.connection = Objects.requireNonNull(connection);
    }

    /**
     * Query a singular result.
     * Note that the result set cursor is already pointed at the current entry.
     *
     * @param <T> Result type.
     * @param query SQL query.
     * @param rsMapper Result mapper.
     * @return The result.
     * @throws SQLException If an SQL error occurs.
     * @see #querySingular(String, ThrowingConsumer, ThrowingFunction)
     */
    @Nullable
    public <T> T querySingular(@NotNull String query, @NotNull ThrowingFunction<? super ResultSet, ? extends T, ? extends SQLException> rsMapper) throws SQLException {
        return querySingular(query, ps -> {}, rsMapper);
    }

    /**
     * Query a singular result.
     * Note that the result set cursor is already pointed at the current entry.
     *
     * @param <T> Result type.
     * @param query SQL query.
     * @param psMapper Statement mapper.
     * @param rsMapper Result mapper.
     * @return The result.
     * @throws SQLException If an SQL error occurs.
     */
    @Nullable
    public <T> T querySingular(@NotNull String query, @NotNull ThrowingConsumer<? super PreparedStatement, ? extends SQLException> psMapper, @NotNull ThrowingFunction<? super ResultSet, ? extends T, ? extends SQLException> rsMapper) throws SQLException {
        return query(query, psMapper, rs -> {
            if (rs.next()) {
                return rsMapper.apply(rs);
            }
            throw new RuntimeException("No row");
        });
    }

    /**
     * Query multiple rows.
     * Note that the result set cursor is already pointed at the current entry.
     *
     * @param <T> Result type.
     * @param query SQL query.
     * @param rsMapper Result mapper.
     * @return The result.
     * @throws SQLException If an SQL error occurs.
     * @see #queryPlural(String, ThrowingConsumer, ThrowingFunction)
     */
    @NotNull
    public <T> List<? extends T> queryPlural(@NotNull String query, @NotNull ThrowingFunction<? super ResultSet, ? extends T, ? extends SQLException> rsMapper) throws SQLException {
        return queryPlural(query, ps -> {}, rsMapper);
    }

    /**
     * Query multiple rows.
     * Note that the result set cursor is already pointed at the current entry.
     *
     * @param <T> Result type.
     * @param query SQL query.
     * @param psMapper Statement mapper.
     * @param rsMapper Result mapper.
     * @return The result.
     * @throws SQLException If an SQL error occurs.
     */
    @NotNull
    public <T> List<? extends T> queryPlural(@NotNull String query, @NotNull ThrowingConsumer<? super PreparedStatement, ? extends SQLException> psMapper, @NotNull ThrowingFunction<? super ResultSet, ? extends T, ? extends SQLException> rsMapper) throws SQLException {
        return query(query, psMapper, rs -> {
            List<T> list = new ArrayList<>();
            while (rs.next()) {
                T t = rsMapper.apply(rs);
                if (t != null) {
                    list.add(t);
                }
            }
            return list;
        });
    }

    /**
     * Execute a query.
     *
     * @param <T> Result type.
     * @param query SQL query.
     * @param rsMapper Result mapper.
     * @return The result.
     * @throws SQLException If an SQL error occurs.
     * @see #query(String, ThrowingConsumer, ThrowingFunction)
     */
    @Nullable
    public <T> T query(@NotNull String query, @NotNull ThrowingFunction<? super ResultSet, ? extends T, ? extends SQLException> rsMapper) throws SQLException {
        return query(query, ps -> {}, rsMapper);
    }

    /**
     * Execute a query.
     *
     * @param <T> Result type.
     * @param query SQL query.
     * @param psMapper Statement mapper.
     * @param rsMapper Result mapper.
     * @return The result.
     * @throws SQLException If an SQL error occurs.
     */
    @Nullable
    public <T> T query(@NotNull String query, @NotNull ThrowingConsumer<? super PreparedStatement, ? extends SQLException> psMapper, @NotNull ThrowingFunction<? super ResultSet, ? extends T, ? extends SQLException> rsMapper) throws SQLException {
        // Preconditions
        Objects.requireNonNull(query);
        Objects.requireNonNull(psMapper);
        Objects.requireNonNull(rsMapper);

        try (PreparedStatement statement = connection.prepareStatement(query)) {
            psMapper.accept(statement);
            try (ResultSet rs = statement.executeQuery()) {
                return rsMapper.apply(rs);
            }
        }
    }

    @Override
    public synchronized void close() throws SQLException {
        if (closed) {
            return;
        }
        try {
            connection.close();
        } finally {
            closed = true;
        }
    }
}
