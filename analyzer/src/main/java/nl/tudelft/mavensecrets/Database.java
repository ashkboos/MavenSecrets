package nl.tudelft.mavensecrets;

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
    private static final String PACKAGE_INDEX_TABLE_WITH_ALL_PACKAGING = "package_list_with_all_packaging";
    private static final String SELECTED_INDEX_TABLE = "selected_packages";
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

    public void createIndexesTableWithAllPackaging(boolean checked) throws SQLException {
        if(!checked && !tableExists(PACKAGE_INDEX_TABLE_WITH_ALL_PACKAGING)) {
            createIndexTableWithPackaging();
        }
    }

    public void createExtensionTable() throws SQLException {
        if(!tableExists(EXTENSION_TABLE)) {
            createTable(EXTENSION_TABLE);
        }
    }

    public void createUnresolvedTable() throws SQLException {
        if(!tableExists(UNRESOLVED_PACKAGES)) {
            createUnresolvedTable0();
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

    private boolean tableExists(String name) throws SQLException {
        var result = queryScalar("SELECT EXISTS(SELECT * FROM information_schema.tables WHERE table_name = '" + name + "')");
        if (result instanceof Boolean)
            return (Boolean) result;

        throw new RuntimeException("query didn't result in boolean");
    }

    private void createUnresolvedTable0() throws SQLException {
        execute("CREATE TABLE " + UNRESOLVED_PACKAGES + "(groupid VARCHAR, artifactid VARCHAR, version VARCHAR, error VARCHAR, PRIMARY KEY (groupid, artifactid, version))");
    }

    private void createTable(String tableName) throws SQLException {
        execute("CREATE TABLE " + tableName + "(groupid VARCHAR, artifactid VARCHAR, version VARCHAR, updated TIMESTAMP NOT NULL DEFAULT NOW(), PRIMARY KEY (groupid, artifactid, version))");
    }

    private void createIndexTable() throws SQLException {
        execute("CREATE TABLE " + PACKAGE_INDEX_TABLE + "(groupid varchar," +
                "artifactid varchar," +
                "version    varchar," +
                "lastmodified date," +
                "packagingtype varchar," +
                "primary key (groupid, artifactid, version))");
    }

    private void createIndexTableWithPackaging() throws SQLException {
        execute("CREATE TABLE " + PACKAGE_INDEX_TABLE_WITH_ALL_PACKAGING + "(groupid varchar," +
            "artifactid varchar," +
            "version    varchar," +
            "lastmodified date," +
            "packagingtype varchar," +
            "primary key (groupid, artifactid, version, packagingtype))");
    }

    public void createSelectedTable() throws SQLException {
        conn.prepareStatement("DROP TABLE IF EXISTS " + SELECTED_INDEX_TABLE).execute();
        conn.prepareStatement(
                "create table " + SELECTED_INDEX_TABLE + """
                (
                groupid       varchar not null,
                artifactid    varchar not null,
                version       varchar not null,
                lastmodified  date,
                packagingtype varchar,
                primary key (groupid, artifactid, version)
                );
                """
        ).execute();
    }

    private void createExtensionTable() throws SQLException {
        conn.prepareStatement("CREATE TABLE " + EXTENSION_TABLE + "(id varchar(128)," +
                "extension varchar(128)," +
                "count BIGINT," +
                "size BIGINT," +
                "min BIGINT," +
                "max BIGINT," +
                "median BIGINT," +
                "constraint extension_table_name_pk " +
                "primary key (id, extension))").execute();
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
    public void update(PackageId id, Field[] fields, Object[] values) throws SQLException {
        if (fields.length != values.length)
            throw new IllegalArgumentException("number of fields and values is different");

        StringBuilder names = new StringBuilder("groupid,artifactid,version");
        StringBuilder qe = new StringBuilder("?,?,?");
        StringBuilder upd = new StringBuilder();
        for (var field : fields) {
            names.append(",").append(field.name());
            qe.append(",?");
            if (!upd.isEmpty())
                upd.append(",");

            upd.append(field.name()).append("=?");
        }

        Object[] arguments = new Object[fields.length * 2 + 3];
        arguments[0] = id.group();
        arguments[1] = id.artifact();
        arguments[2] = id.version();
        for (var i = 0; i < fields.length; i++)
            arguments[i + fields.length + 3] = arguments[i + 3] = values[i];

        var table = updatePackageTable ? PACKAGES_TABLE : EXTENSION_TABLE;
        execute("INSERT INTO " + table + "(" + names + ") VALUES (" + qe + ") ON CONFLICT(groupid,artifactid,version) DO UPDATE SET updated = DEFAULT," + upd, arguments);
    }

    void updateIndexTable(String groupId, String artifactId, String version, Date lastModified, String packagingType) throws SQLException {
        execute("INSERT INTO " + PACKAGE_INDEX_TABLE + "(groupid, artifactid, version, lastmodified, packagingtype) VALUES(?,?,?,?,?) ON CONFLICT DO NOTHING", new Object[]{groupId, artifactId, version, lastModified, packagingType});
    }

    // TODO UPDATE THIS
    void batchUpdateIndexTable(List<String[]> indexedInfo) throws SQLException {
       PreparedStatement query =  conn.prepareStatement("INSERT INTO " + PACKAGE_INDEX_TABLE + " "
               + "(groupid, artifactid, version, lastmodified, packagingtype) VALUES (?,?,?,?,?)");
       for (String[] info : indexedInfo) {
           query.setString(1, info[0]);
           query.setString(2, info[1]);
           query.setString(3, info[2]);
           query.setDate(4, new Date(Long.parseLong(info[3])));
           query.setString(5, info[4]);
           query.addBatch();
       }
       query.executeBatch();
    }

    public void batchUpdateIndexTableWithPackaging(List<String[]> indexInfo) throws SQLException {
        PreparedStatement query =  conn.prepareStatement("INSERT INTO " + PACKAGE_INDEX_TABLE_WITH_ALL_PACKAGING + " "
                + "(groupid, artifactid, version, lastmodified, packagingtype) VALUES (?,?,?,?,?)");
        for (String[] info : indexInfo) {
            query.setString(1, info[0]);
            query.setString(2, info[1]);
            query.setString(3, info[2]);
            query.setDate(4, new Date(Long.parseLong(info[3])));
            query.setString(5, info[4]);
            query.addBatch();
        }
        query.executeBatch();
    }

    void updateUnresolvedTable(ArtifactId id, String error) throws SQLException {
        execute("INSERT INTO " + UNRESOLVED_PACKAGES + "(groupid,artifactid,version, error) VALUES(?,?,?,?) ON CONFLICT DO NOTHING",
                new Object[] { id.group(), id.artifact(), id.version(), error });
    }

    public void updateExtensionTable(String id, String extension, long count, long size, long min, long max, long median) throws SQLException {
        PreparedStatement query = conn.prepareStatement("INSERT INTO " + EXTENSION_TABLE +
                "(id, extension, count, size, min, max, median) VALUES(?,?,?,?,?,?,?) ON CONFLICT DO NOTHING");
        query.setString(1, id);
        query.setString(2, extension);
        query.setLong(3, count);
        query.setLong(4, size);
        query.setLong(5, min);
        query.setLong(6, max);
        query.setLong(7, median);
        query.execute();
    }

    /**
     * @return list of package ids of packages to be fed to the runner
     */
    public List<ArtifactId> getArtifactIds(int page, int pageSize) throws SQLException {
        List<ArtifactId> artifacts = new LinkedList<>();
        if (!tableExists(PACKAGE_INDEX_TABLE))
            return artifacts;

        try (var results = query("SELECT groupid, artifactid, version, packagingtype FROM " + PACKAGE_INDEX_TABLE + " ORDER BY groupid, artifactid, version LIMIT " + pageSize + " OFFSET " + pageSize * page)) {
            while (results.next()) {
                artifacts.add(new ArtifactId(results.getString("groupid"),
                        results.getString("artifactid"),
                        results.getString("version"),
                        results.getString("packagingtype")));
            }
        }

        return artifacts;
    }


    public List<ArtifactId> getSelectedPkgs(int page, int pageSize) throws SQLException {
        List<ArtifactId> artifacts = new LinkedList<>();
        if (!tableExists(SELECTED_INDEX_TABLE))
            return artifacts;

        try (var results = query("SELECT groupid, artifactid, version, packagingtype FROM " + SELECTED_INDEX_TABLE + " ORDER BY groupid, artifactid, version LIMIT " + pageSize + " OFFSET " + pageSize * page)) {

            while (results.next()) {
                artifacts.add(new ArtifactId(results.getString("groupid"),
                        results.getString("artifactid"),
                        results.getString("version"),
                        results.getString("packagingtype")));
            }
        }
        return artifacts;
    }

    public Map<Integer, Integer> getYearCounts() throws SQLException {
        String sql = "SELECT date_part('year', lastmodified) AS year, COUNT(*)"
                + "FROM " + PACKAGE_INDEX_TABLE
                + " group by year ";
        ResultSet rs = query(sql);
        Map<Integer, Integer> yearCounts = new HashMap<>();
        while (rs.next()) {
           yearCounts.put(rs.getInt(1), rs.getInt(2)) ;
        }
        return yearCounts;
    }

    public void extractStrataSample(long seed, double percent, int year) throws SQLException {
       String sql = "INSERT INTO selected_packages SELECT DISTINCT ON (groupid, artifactid) * FROM "
               + PACKAGE_INDEX_TABLE + " TABLESAMPLE bernoulli(" + percent +") REPEATABLE ("+ seed + ")" +
               "WHERE date_part('year', lastmodified) = " + year;
       execute(sql);
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