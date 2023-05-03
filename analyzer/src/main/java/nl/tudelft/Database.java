package nl.tudelft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.sql.*;
import java.util.*;

// PackageId PKEY, Field 1, Value 1, Field 2, Value 2, etc
public class Database implements Closeable {
    private static final String TABLE_NAME = "packages";
    private final Connection conn;
    private final Logger log;

    private Database(Connection conn, Logger log) {
        this.conn = conn;
        this.log = log;
    }

    public static Database connect(String url, String user, String pass) throws SQLException {
        var log = LogManager.getLogger(Database.class);
        try {
            log.trace("connecting to " + url);
            return new Database(DriverManager.getConnection(url, user, pass), log);
        } catch (SQLException ex) {
            log.error("failed to connect to the database", ex);
            throw ex;
        }
    }

    void updateSchema(Field[] fields) throws SQLException {
        if (!tableExists())
            createTable();

        Set<String> cols = listColumns();
        for (var field : fields)
            if (!cols.contains(field.getName()))
                createColumn(field);
    }

    private boolean tableExists() throws SQLException {
        var result = queryScalar("SELECT EXISTS(SELECT * FROM information_schema.tables WHERE table_name = '" + TABLE_NAME + "')");
        if (result instanceof Boolean)
            return (Boolean) result;

        throw new RuntimeException("query didn't result in boolean");
    }

    private void createTable() throws SQLException {
        execute("CREATE TABLE " + TABLE_NAME + "(id VARCHAR(128) PRIMARY KEY)");
    }

    private Set<String> listColumns() throws SQLException {
        try (var results = query("SELECT column_name FROM information_schema.columns WHERE table_name = '" + TABLE_NAME + "'")) {
            var columns = new HashSet<String>();
            while (results.next())
                columns.add(results.getString(1));

            return columns;
        }
    }

    private void createColumn(Field field) throws SQLException {
        execute("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + field.getName() + " " + field.getType() + " NULL");
    }

    // Don't call it without being sure of schema
    void update(PackageId id, Field[] fields, Object[] values) throws SQLException {
        if (fields.length != values.length)
            throw new IllegalArgumentException("number of fields and values is different");

        StringBuilder names = new StringBuilder("id");
        StringBuilder qe = new StringBuilder("?");
        StringBuilder upd = new StringBuilder();
        for (var field : fields) {
            names.append(",").append(field.getName());
            qe.append(",?");
            if (!upd.isEmpty())
                upd.append(",");

            upd.append(field.getName()).append("=?");
        }

        Object[] arguments = new Object[fields.length * 2 + 1];
        arguments[0] = id.toString();
        for (var i = 0; i < fields.length; i++)
            arguments[i + fields.length + 1] = arguments[i + 1] = values[i];
        execute("INSERT INTO " + TABLE_NAME + "(" + names + ") VALUES (" + qe + ") ON CONFLICT(id) DO UPDATE SET " + upd, arguments);
    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private PreparedStatement prepare(String sql, Object[] arguments) throws SQLException {
        var statement = conn.prepareStatement(sql);
        for (var i = 0; i < arguments.length; i++)
            statement.setObject(i + 1, arguments[i]);

        return statement;
    }

    private ResultSet query(String sql) throws SQLException {
        return query(sql, new Object[0]);
    }

    private ResultSet query(String sql, Object[] arguments) throws SQLException {
        ResultSet results;
        try {
            results = prepare(sql, arguments).executeQuery();
        } catch (SQLException ex) {
            log.error("query " + stringify(sql, arguments) + " failed", ex);
            throw ex;
        }

        log.trace("queried " + stringify(sql, arguments));
        return results;
    }

    private Object queryScalar(String sql) throws SQLException {
        return queryScalar(sql, new Object[0]);
    }

    private Object queryScalar(String sql, Object[] arguments) throws SQLException {
        Object value;
        try {
            try (ResultSet results = prepare(sql, arguments).executeQuery()) {
                if (!results.next())
                    throw new RuntimeException("query didn't return any rows");

                value = results.getObject(1);
                if (results.next())
                    throw new RuntimeException("query returned too many rows");
            }
        } catch (SQLException ex) {
            log.error("query " + stringify(sql, arguments) + " failed", ex);
            throw ex;
        }

        log.trace("query " + stringify(sql, arguments) + " returned `" + value + "`: " + value.getClass().getName());
        return value;
    }

    private void execute(String sql) throws SQLException {
        execute(sql, new Object[0]);
    }

    private void execute(String sql, Object[] arguments) throws SQLException {
        try {
            try (var statement = prepare(sql, arguments)) {
                statement.execute();
            }
        } catch (SQLException ex) {
            log.error("query " + stringify(sql, arguments) + " failed", ex);
            throw ex;
        }

        log.trace("executed " + stringify(sql, arguments));
    }

    private static String stringify(String sql, Object[] arguments) {
        if (arguments.length == 0)
            return "`" + sql + "`";

        return "`" + sql + "` with [" + Arrays.stream(arguments).map(Object::toString).reduce((i, j) -> i + "," + j).orElse("") + "]";
    }
}