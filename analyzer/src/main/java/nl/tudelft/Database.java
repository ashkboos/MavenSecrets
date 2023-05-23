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
    private static final String EXTENSION_TABLE = "extensions";
    private static final String UNRESOLVED_PACKAGES = "unresolved_packages";
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
        LOGGER.trace("Attempting to connect to {}", url);
        var sleep = BACKOFF_TIME_MS;
        for (var i = 1;; i++) {
            try {
                return new Database(DriverManager.getConnection(url, user, pass));
            } catch (SQLException ex) {
                LOGGER.warn("Failed to connect to the database (attempt {})", i, ex);
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
        execute("UPDATE packages p SET lastmodified = pl.lastmodified " +
                        "FROM package_list pl " +
                        "WHERE CONCAT(pl.groupid, ':', pl.artifactid, ':', pl.version) = p.id ");
    }

    void createIndexesTable(boolean checked) throws SQLException {
        if(!checked && !tableExists(PACKAGE_INDEX_TABLE)) {
            createIndexTable();
        }
    }

    public void createExtensionTable(boolean checked) throws SQLException {
        if(!checked && !tableExists(EXTENSION_TABLE)) {
            createTable(EXTENSION_TABLE);
        }
    }

    public void createUnresolvedTable(boolean checked) throws SQLException {
        if(!checked && !tableExists(UNRESOLVED_PACKAGES)) {
            createUnresolvedTable();
        }
    }

    void updateSchema(Field[] fields) throws SQLException{
        if (!tableExists(PACKAGES_TABLE))
            createTable(PACKAGES_TABLE);

        Set<String> cols = listColumns(PACKAGES_TABLE);
        for (var field : fields)
            if (!cols.contains(field.name()))
                createColumn(field, PACKAGES_TABLE);
    }

    public void updateExtensionSchema(Field[] fields) throws SQLException{
        Set<String> cols = listColumns(EXTENSION_TABLE);
        for (var field : fields)
            if (!cols.contains(field.name())) {
                createColumn(field, EXTENSION_TABLE);
            }
    }

    private boolean tableExists(String name) throws SQLException {
        var result = queryScalar("SELECT EXISTS(SELECT * FROM information_schema.tables WHERE table_name = '" + name + "')");
        if (result instanceof Boolean)
            return (Boolean) result;

        throw new RuntimeException("query didn't result in boolean");
    }

    private void createUnresolvedTable() throws SQLException {
        execute("CREATE TABLE " + UNRESOLVED_PACKAGES + "(id VARCHAR(128) PRIMARY KEY, error VARCHAR(512))");
    }

    private void createTable(String tableName) throws SQLException {
        execute("CREATE TABLE " + tableName + "(id VARCHAR(128) PRIMARY KEY)");
    }

    private void createIndexTable() throws SQLException {
        conn.prepareStatement("CREATE TABLE " + PACKAGE_INDEX_TABLE + "(groupid varchar(128)," +
                "artifactid varchar(128)," +
                "version    varchar(128)," +
                "lastmodified date," +
                "packagingtype varchar(128)," +
                "constraint table_name_pk " +
                "primary key (groupid, artifactid, version))").execute();
    }

    private Set<String> listColumns(String tableName) throws SQLException {
        try (var results = query("SELECT column_name FROM information_schema.columns WHERE table_name = '" + tableName + "'")) {
            var columns = new HashSet<String>();
            while (results.next())
                columns.add(results.getString(1));

            return columns;
        }
    }

    private void createColumn(Field field, String tableName) throws SQLException {
        execute("ALTER TABLE " + tableName + " ADD COLUMN " + field.name() + " " + field.type() + " NULL");
    }

    // Don't call it without being sure of schema
    public void update(PackageId id, Field[] fields, Object[] values, boolean updatePackageTable) throws SQLException {
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
        if(updatePackageTable) {
            execute("INSERT INTO " + PACKAGES_TABLE + "(" + names + ") VALUES (" + qe + ") ON CONFLICT(id) DO UPDATE SET " + upd, arguments);
        } else execute("INSERT INTO " + EXTENSION_TABLE + "(" + names + ") VALUES (" + qe + ") ON CONFLICT(id) DO UPDATE SET " + upd, arguments);
    }

    void updateIndexTable(String groupId, String artifactId, String version, Date lastModified, String packagingType) throws SQLException {
        PreparedStatement query = conn.prepareStatement("INSERT INTO " + PACKAGE_INDEX_TABLE +
                "(groupid, artifactid, version, lastmodified, packagingtype) VALUES(?,?,?,?,?) ON CONFLICT DO NOTHING");
        query.setString(1, groupId);
        query.setString(2, artifactId);
        query.setString(3, version);
        query.setDate(4, lastModified);
        query.setString(5, packagingType);
        query.execute();
    }

    void updateUnresolvedTable(String id, String error) throws SQLException {
        PreparedStatement query = conn.prepareStatement("INSERT INTO " + UNRESOLVED_PACKAGES +
                "(id, error) VALUES(?,?) ON CONFLICT DO NOTHING");
        query.setString(1, id);
        query.setString(2, error);
        query.execute();

    }

    /**
     * @return list of package ids of packages to be fed to the runner
     */
    public List<PackageId> getPackageIds() throws SQLException {
        List<PackageId> packageIds = new LinkedList<>();
        if (!tableExists(PACKAGE_INDEX_TABLE))
            return packageIds;

        try (var results = query("SELECT groupid, artifactid, version FROM " + PACKAGE_INDEX_TABLE + " ORDER BY CONCAT(groupid, artifactid, version)")) {
            while (results.next()) {
                packageIds.add(new PackageId(results.getString("groupid"),
                        results.getString("artifactid"),
                        results.getString("version")));
            }
        }

        return packageIds;
    }

    public Map<PackageId, String> getPackagingType() throws SQLException {
        Map<PackageId, String> packagingTypes = new HashMap<>();

        if (!tableExists(PACKAGE_INDEX_TABLE)) {
            return packagingTypes;
        }

        try (var results = query("SELECT groupid, artifactid, version, packagingtype FROM " + PACKAGE_INDEX_TABLE)) {
            while (results.next()) {
                PackageId id = new PackageId(results.getString("groupid"), results.getString("artifactid"), results.getString("version"));
                String type = results.getString("packagingtype");
                packagingTypes.put(id, type);
            }
        }

        return packagingTypes;
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
            LOGGER.error("Query {} failed", stringify(sql, arguments), ex);
            throw ex;
        }

        LOGGER.trace("Queried {}", stringify(sql, arguments));
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
            LOGGER.error("Query {} failed", stringify(sql, arguments), ex);
            throw ex;
        }

        LOGGER.trace("Query {} returned '{}': {}", stringify(sql, arguments), value, value.getClass().getName());
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
            LOGGER.error("Query {} failed", stringify(sql, arguments), ex);
            throw ex;
        }

        LOGGER.trace("Executed {}", stringify(sql, arguments));
    }

    private static String stringify(String sql, Object[] arguments) {
        if (arguments.length == 0)
            return "`" + sql + "`";

        return "`" + sql + "` with [" + Arrays.stream(arguments).map(Objects::toString).reduce((i, j) -> i + "," + j).orElse("") + "]";
    }
}