package nl.tudelft.mavensecrets.visualization.buildaspects;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Objects;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

public class ConnectionWrapperTest {

    private Connection connection = null;
    private PreparedStatement ps = null;

    @Test
    public void test_connection_null() {
        Assertions.assertThrows(NullPointerException.class, () -> new ConnectionWrapper(null));
    }

    @Test
    public void test_query_singular_no_rows_1() {
        ResultSet rs = getResultSet(new Object[0]);
        setResultSet(rs);

        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(RuntimeException.class, () -> wrapper.querySingular("", rs0 -> rs0.getObject(1)));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_singular_no_rows_2() {
        ResultSet rs = getResultSet(new Object[0]);
        setResultSet(rs);

        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(RuntimeException.class, () -> wrapper.querySingular("", ps0 -> Assertions.assertSame(ps, ps0), rs0 -> rs0.getObject(1)));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_singular_one_row_1() {
        Object[] objects = new Object[] {new Object()};
        ResultSet rs = getResultSet(objects);
        setResultSet(rs);

        Object result;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            result = wrapper.querySingular("", rs0 -> {
                Assertions.assertSame(rs, rs0);
                return rs0.getObject(1);
            });
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertSame(objects[0], result);
    }

    @Test
    public void test_query_singular_one_row_2() {
        Object[] objects = new Object[] {new Object()};
        ResultSet rs = getResultSet(objects);
        setResultSet(rs);

        Object result;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            result = wrapper.querySingular("", ps0 -> Assertions.assertSame(ps, ps0), rs0 -> {
                Assertions.assertSame(rs, rs0);
                return rs0.getObject(1);
            });
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertSame(objects[0], result);
    }

    @Test
    public void test_query_singular_multi_row_1() {
        Object[] objects = new Object[] {new Object(), new Object()};
        ResultSet rs = getResultSet(objects);
        setResultSet(rs);

        Object result;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            result = wrapper.querySingular("", rs0 -> {
                Assertions.assertSame(rs, rs0);
                return rs0.getObject(1);
            });
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertSame(objects[0], result);
    }

    @Test
    public void test_query_singular_multi_row_2() {
        Object[] objects = new Object[] {new Object(), new Object()};
        ResultSet rs = getResultSet(objects);
        setResultSet(rs);

        Object result;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            result = wrapper.querySingular("", ps0 -> Assertions.assertSame(ps, ps0), rs0 -> {
                Assertions.assertSame(rs, rs0);
                return rs0.getObject(1);
            });
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertSame(objects[0], result);
    }

    @Test
    public void test_query_singular_query_null_1() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.querySingular(null, rs -> rs.getObject(1)));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_singular_query_null_2() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.querySingular(null, ps -> {}, rs -> rs.getObject(1)));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_singular_statement_mapper_null() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.querySingular("", null, rs -> rs.getObject(1)));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_singular_result_mapper_null_1() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.querySingular("", null));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_singular_result_mapper_null_2() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.querySingular("", ps -> {}, null));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_plural_no_rows_1() {
        ResultSet rs = getResultSet(new Object[0]);
        setResultSet(rs);

        List<? extends Object> result;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            result = wrapper.queryPlural("", rs0 -> rs0.getObject(1));
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_query_plural_no_rows_2() {
        ResultSet rs = getResultSet(new Object[0]);
        setResultSet(rs);

        List<? extends Object> result;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            result = wrapper.queryPlural("", ps0 -> Assertions.assertSame(ps, ps0), rs0 -> rs0.getObject(1));
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.isEmpty());
    }

    @Test
    public void test_query_plural_one_row_1() {
        Object[] objects = new Object[] {new Object()};
        ResultSet rs = getResultSet(objects);
        setResultSet(rs);

        List<? extends Object> result;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            result = wrapper.queryPlural("", rs0 -> {
                Assertions.assertSame(rs, rs0);
                return rs0.getObject(1);
            });
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(objects[0], result.get(0));
    }

    @Test
    public void test_query_plural_one_row_2() {
        Object[] objects = new Object[] {new Object()};
        ResultSet rs = getResultSet(objects);
        setResultSet(rs);

        List<? extends Object> result;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            result = wrapper.queryPlural("", ps0 -> Assertions.assertSame(ps, ps0), rs0 -> {
                Assertions.assertSame(rs, rs0);
                return rs0.getObject(1);
            });
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertNotNull(result);
        Assertions.assertEquals(1, result.size());
        Assertions.assertSame(objects[0], result.get(0));
    }

    @Test
    public void test_query_plural_multi_row_1() {
        Object[] objects = new Object[] {new Object(), new Object()};
        ResultSet rs = getResultSet(objects);
        setResultSet(rs);

        List<? extends Object> result;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            result = wrapper.queryPlural("", rs0 -> {
                Assertions.assertSame(rs, rs0);
                return rs0.getObject(1);
            });
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertSame(objects[0], result.get(0));
        Assertions.assertSame(objects[1], result.get(1));
    }

    @Test
    public void test_query_plural_multi_row_2() {
        Object[] objects = new Object[] {new Object(), new Object()};
        ResultSet rs = getResultSet(objects);
        setResultSet(rs);

        List<? extends Object> result;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            result = wrapper.queryPlural("", ps0 -> Assertions.assertSame(ps, ps0), rs0 -> {
                Assertions.assertSame(rs, rs0);
                return rs0.getObject(1);
            });
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertNotNull(result);
        Assertions.assertEquals(2, result.size());
        Assertions.assertSame(objects[0], result.get(0));
        Assertions.assertSame(objects[1], result.get(1));
    }

    @Test
    public void test_query_plural_query_null_1() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.queryPlural(null, rs -> rs.getObject(1)));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_plural_query_null_2() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.queryPlural(null, ps -> {}, rs -> rs.getObject(1)));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_plural_statement_mapper_null() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.queryPlural("", null, rs -> rs.getObject(1)));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_plural_result_mapper_null_1() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.queryPlural("", null));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_plural_result_mapper_null_2() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.queryPlural("", ps -> {}, null));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_1() {
        ResultSet rs = Mockito.mock(ResultSet.class);
        Object object = new Object();
        try {
            Mockito.when(rs.getObject(1))
                    .thenReturn(object);
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }
        setResultSet(rs);

        Object result;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            result = wrapper.query("", rs0 -> {
                Assertions.assertSame(rs, rs0);
                return rs0.getObject(1);
            });
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        try {
            Mockito.verify(ps, Mockito.times(1))
                    .executeQuery();
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertSame(object, result);
    }

    @Test
    public void test_query_2() {
        ResultSet rs = Mockito.mock(ResultSet.class);
        Object object = new Object();
        try {
            Mockito.when(rs.getObject(1))
                    .thenReturn(object);
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }
        setResultSet(rs);

        Object result;
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            result = wrapper.query("", ps0 -> Assertions.assertSame(ps, ps0), rs0 -> {
                Assertions.assertSame(rs, rs0);
                return rs0.getObject(1);
            });
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        try {
            Mockito.verify(ps, Mockito.times(1))
                    .executeQuery();
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        Assertions.assertSame(object, result);
    }

    @Test
    public void test_query_query_null_1() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.query(null, rs -> rs.getObject(1)));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_query_null_2() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.query(null, ps -> {}, rs -> rs.getObject(1)));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_statement_mapper_null() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.query("", null, rs -> rs.getObject(1)));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_result_mapper_null_1() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.query("", null));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_query_result_mapper_null_2() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.query("", ps -> {}, null));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_update_1() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            wrapper.update("");
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        try {
            Mockito.verify(ps, Mockito.times(1))
                    .executeUpdate();
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_update_2() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            wrapper.update("", ps0 -> Assertions.assertSame(ps, ps0));
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        try {
            Mockito.verify(ps, Mockito.times(1))
                    .executeUpdate();
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_update_query_null_1() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.update(null));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_update_query_null_2() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.update(null, ps -> {}));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_update_statement_mapper_null() {
        try (ConnectionWrapper wrapper = new ConnectionWrapper(connection)) {
            Assertions.assertThrows(NullPointerException.class, () -> wrapper.update("", null));
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_close() {
        ConnectionWrapper wrapper = new ConnectionWrapper(connection);
        try {
            wrapper.close();
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        try {
            Mockito.verify(connection, Mockito.times(1)).close();
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @Test
    public void test_close_multiple() {
        ConnectionWrapper wrapper = new ConnectionWrapper(connection);
        try {
            wrapper.close();
            wrapper.close();
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return;
        }

        try {
            Mockito.verify(connection, Mockito.times(1)).close();
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @BeforeEach
    public void setup() {
        connection = Mockito.mock(Connection.class);
        ps = Mockito.mock(PreparedStatement.class);
        try {
            Mockito.when(connection.prepareStatement(Mockito.anyString()))
                    .thenReturn(ps);
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }

    @AfterEach
    public void teardown() {
        connection = null;
        ps = null;
    }

    @NotNull
    private ResultSet getResultSet(@NotNull Object... objects) {
        // Preconditions
        Objects.requireNonNull(objects);
        for (Object object : objects) {
            Objects.requireNonNull(object);
        }

        ResultSet rs = Mockito.mock(ResultSet.class);
        int[] ctx = {-1, 0}; // Jank

        try {
            Mockito.when(rs.next())
                    .thenAnswer(new Answer<>() {

                        @NotNull
                        @Override
                        public Boolean answer(@NotNull InvocationOnMock invocation) throws Throwable {
                            // Preconditions
                            Objects.requireNonNull(invocation);

                            if (ctx[0] + 1 < objects.length) {
                                ctx[0]++;
                                return true;
                            }

                            return false;
                        }
                    });
            Mockito.when(rs.getObject(1))
                    .thenAnswer(new Answer<>() {

                        @NotNull
                        @Override
                        public Object answer(@NotNull InvocationOnMock invocation) throws Throwable {
                            // Preconditions
                            Objects.requireNonNull(invocation);

                            if (ctx[1] != 0) {
                                throw new SQLException("Result set is closed");
                            }
                            if (ctx[0] < 0 || ctx[0] >= objects.length) {
                                throw new SQLException("Cursor is not positioned properly");
                            }
                            return objects[ctx[0]];
                        }
                    });
            Mockito.doAnswer(new Answer<>() {

                @NotNull
                @Override
                public Object answer(@NotNull InvocationOnMock invocation) throws Throwable {
                    // Preconditions
                    Objects.requireNonNull(invocation);

                    ctx[1] = 1;
                    return null;
                }
            })
                    .when(rs)
                    .close();
        } catch (SQLException exception) {
            Assertions.fail(exception);
            return null;
        }

        return rs;
    }

    private void setResultSet(@NotNull ResultSet rs) {
        // Preconditions
        Objects.requireNonNull(rs);

        try {
            Mockito.when(ps.executeQuery())
                    .thenReturn(rs);
        } catch (SQLException exception) {
            Assertions.fail(exception);
        }
    }
}
