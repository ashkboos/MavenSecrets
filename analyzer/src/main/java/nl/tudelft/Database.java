package nl.tudelft;

import java.io.Closeable;
import java.sql.*;
import java.sql.Date;
import java.util.*;

// PackageId PKEY, Field 1, Value 1, Field 2, Value 2, etc
public class Database implements Closeable {
    private static final String TABLE_NAME = "packages";
    private static final String TABLE_NAME2 = "indexes";
    private final Connection conn;

    private Database(Connection conn) {
        this.conn = conn;
    }

    public static Database connect(String url, String user, String pass) throws SQLException {
        return new Database(DriverManager.getConnection(url, user, pass));
    }

    void createIndexesTable() throws SQLException {
        if(!tableExists(TABLE_NAME2)) {
            createTable2();
        }
    }

    void updateSchema(Field[] fields) throws SQLException{
        if (!tableExists(TABLE_NAME))
            createTable();

        Set<String> cols = listColumns();
        for (var field : fields)
            if (!cols.contains(field.getName()))
                createColumn(field);
    }

    private boolean tableExists(String tableName) throws SQLException {
        var query = conn.prepareStatement("SELECT EXISTS(SELECT * FROM information_schema.tables WHERE table_name = '" + tableName + "')");

        var results = query.executeQuery();
        return results.next() && results.getBoolean(1);
    }

    private void createTable() throws SQLException {
        conn.prepareStatement("CREATE TABLE " + TABLE_NAME + "(id VARCHAR(128) PRIMARY KEY)").execute();
    }

    private void createTable2() throws SQLException {
        conn.prepareStatement("CREATE TABLE " + TABLE_NAME2 + "(groupid varchar(128)," +
                "artifactid varchar(128)," +
                "version    varchar(128)," +
                "lastmodified date," +
                "constraint table_name_pk " +
                "primary key (groupid, artifactid, version))").execute();
    }

    private Set<String> listColumns() throws SQLException {
        var query = conn.prepareStatement("SELECT column_name FROM information_schema.columns WHERE table_name = '" + TABLE_NAME + "'");

        var results = query.executeQuery();

        var columns = new HashSet<String>();
        while (results.next())
            columns.add(results.getString(1));

        return columns;
    }

    private void createColumn(Field field) throws SQLException {
        conn.prepareStatement("ALTER TABLE " + TABLE_NAME + " ADD COLUMN " + field.getName() + " " + field.getType() + " NULL").execute();
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
        
        PreparedStatement query = conn.prepareStatement("INSERT INTO Packages(" + names + ") VALUES (" + qe + ") ON CONFLICT(id) DO UPDATE SET " + upd);
        query.setString(1, id.toString());
        for (var i = 0; i < fields.length; i++) {
            query.setObject(i + 2, values[i]);
            query.setObject(i + 2 + fields.length, values[i]);
        }

        query.execute();
    }

    void updateIndexTable(String groupId, String artifactId, String version, Date lastModified) throws SQLException {
        PreparedStatement query = conn.prepareStatement("INSERT INTO " + TABLE_NAME2 +
                "(groupid, artifactid, version, lastmodified) VALUES(?,?,?,?) ON CONFLICT DO NOTHING");
        query.setString(1, groupId);
        query.setString(2, artifactId);
        query.setString(3, version);
        query.setDate(4, lastModified);
        query.execute();

    }

    @Override
    public void close() {
        try {
            conn.close();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }
}