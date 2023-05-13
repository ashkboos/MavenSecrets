package nl.tudelft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.sql.*;
import java.sql.Date;
import java.util.*;

// PackageId PKEY, Field 1, Value 1, Field 2, Value 2, etc
public class Database implements Closeable {
    private static final Logger LOGGER = LogManager.getLogger(Database.class);
    private static final String PACKAGES_TABLE = "packages";
    private static final String PACKAGE_INDEX_TABLE = "package_list";
    private static final int BACKOFF_TIME_MS = 1000;
    private static final int BACKOFF_BASE = 2;
    private static final int BACKOFF_RETRIES = 3;

    static {
        // Legacy driver registring because Maven shade does funny things
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private final Connection conn;

    private Database(Connection conn) {
        this.conn = conn;
    }

    public static Database connect(String url, String user, String pass) throws SQLException {
        LOGGER.trace("connecting to " + url);
        var sleep = BACKOFF_TIME_MS;
        for (var i = 0;; i++) {
            try {
                return new Database(DriverManager.getConnection(url, user, pass));
            } catch (SQLException ex) {
                LOGGER.error("failed to connect to the database (attempt " + (i + 1) + ")", ex);
                if (i > BACKOFF_RETRIES)
                    throw ex;
            }

            try {
                // exponential backoff; not a busy wait
                // noinspection BusyWait
                Thread.sleep(sleep);
            } catch (InterruptedException e) {
                // ignored.
            }

            sleep *= BACKOFF_BASE;
        }
    }

    void addTimestamp() throws SQLException {
        updateSchema(new Field[] {new Field("lastmodified", "DATE")});
        execute("INSERT INTO packages (id, lastmodified) SELECT CONCAT(groupid, ':', artifactid, ':', version) AS id, lastmodified FROM package_list ON CONFLICT DO NOTHING");
    }

    void createIndexesTable(boolean checked) throws SQLException {
        if(!checked && !tableExists(PACKAGE_INDEX_TABLE)) {
            createIndexTable();
        }
    }

    void updateSchema(Field[] fields) throws SQLException{
        if (!tableExists(PACKAGES_TABLE))
            createTable();

        Set<String> cols = listColumns();
        for (var field : fields)
            if (!cols.contains(field.name()))
                createColumn(field);
    }

    private boolean tableExists(String name) throws SQLException {
        var result = queryScalar("SELECT EXISTS(SELECT * FROM information_schema.tables WHERE table_name = '" + name + "')");
        if (result instanceof Boolean)
            return (Boolean) result;

        throw new RuntimeException("query didn't result in boolean");
    }

    private void createTable() throws SQLException {
        execute("CREATE TABLE " + PACKAGES_TABLE + "(id VARCHAR PRIMARY KEY)");
    }

    private void createIndexTable() throws SQLException {
        conn.prepareStatement("CREATE TABLE " + PACKAGE_INDEX_TABLE + "(groupid varchar(128)," +
                "artifactid varchar(128)," +
                "version    varchar(128)," +
                "lastmodified date," +
                "constraint table_name_pk " +
                "primary key (groupid, artifactid, version))").execute();
    }

    private Set<String> listColumns() throws SQLException {
        try (var results = query("SELECT column_name FROM information_schema.columns WHERE table_name = '" + PACKAGES_TABLE + "'")) {
            var columns = new HashSet<String>();
            while (results.next())
                columns.add(results.getString(1));

            return columns;
        }
    }

    private void createColumn(Field field) throws SQLException {
        execute("ALTER TABLE " + PACKAGES_TABLE + " ADD COLUMN " + field.name() + " " + field.type() + " NULL");
    }

    // Don't call it without being sure of schema
    void update(PackageId id, Field[] fields, Object[] values) throws SQLException {
        if (fields.length != values.length)
            throw new IllegalArgumentException("number of fields and values is different");

        StringBuilder names = new StringBuilder("id");
        StringBuilder qe = new StringBuilder("?");
        StringBuilder upd = new StringBuilder();
        for (var field : fields) {
            names.append(",").append(field.name());
            qe.append(",?");
            if (!upd.isEmpty())
                upd.append(",");

            upd.append(field.name()).append("=?");
        }

        Object[] arguments = new Object[fields.length * 2 + 1];
        arguments[0] = id.toString();
        for (var i = 0; i < fields.length; i++)
            arguments[i + fields.length + 1] = arguments[i + 1] = values[i];
        execute("INSERT INTO " + PACKAGES_TABLE + "(" + names + ") VALUES (" + qe + ") ON CONFLICT(id) DO UPDATE SET " + upd, arguments);
    }

    void updateIndexTable(String groupId, String artifactId, String version, Date lastModified) throws SQLException {
        PreparedStatement query = conn.prepareStatement("INSERT INTO " + PACKAGE_INDEX_TABLE +
                "(groupid, artifactid, version, lastmodified) VALUES(?,?,?,?) ON CONFLICT DO NOTHING");
        query.setString(1, groupId);
        query.setString(2, artifactId);
        query.setString(3, version);
        query.setDate(4, lastModified);
        query.execute();

    }

    /**
     * @return list of package ids of packages to be fed to the runner
     */
    public List<PackageId> getPackageIds() throws SQLException {
        List<PackageId> packageIds = new LinkedList<>();
        if (!tableExists(PACKAGE_INDEX_TABLE))
            return packageIds;

        try (var results = query("SELECT groupid, artifactid, version FROM " + PACKAGE_INDEX_TABLE)) {
            while (results.next()) {
                packageIds.add(new PackageId(results.getString("groupid"),
                        results.getString("artifactid"),
                        results.getString("version")));
            }
        }

        return packageIds;
    }

    @Override
    public void close() throws IOException {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new IOException(e);
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
            LOGGER.error("query " + stringify(sql, arguments) + " failed", ex);
            throw ex;
        }

        LOGGER.trace("queried " + stringify(sql, arguments));
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
            LOGGER.error("query " + stringify(sql, arguments) + " failed", ex);
            throw ex;
        }

        LOGGER.trace("query " + stringify(sql, arguments) + " returned `" + value + "`: " + value.getClass().getName());
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
            LOGGER.error("query " + stringify(sql, arguments) + " failed", ex);
            throw ex;
        }

        LOGGER.trace("executed " + stringify(sql, arguments));
    }

    private static String stringify(String sql, Object[] arguments) {
        if (arguments.length == 0)
            return "`" + sql + "`";

        return "`" + sql + "` with [" + Arrays.stream(arguments).map(Objects::toString).reduce((i, j) -> i + "," + j).orElse("") + "]";
    }
}