package nl.tudelft;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.apache.maven.index.reader.ChunkReader;

public class IndexerReader {

    private static final int BATCH_SIZE = 1 << 11;
    private final Database db;

    public IndexerReader(Database db) {
        this.db = Objects.requireNonNull(db);
    }

    public void indexerReader(File file) throws IOException, SQLException {
        Objects.requireNonNull(file);

        db.createIndexesTable(false);

        List<String[]> indexInfo = new ArrayList<>();

        try (FileInputStream fileInputStream = new FileInputStream(file); ChunkReader reader = new ChunkReader("index", fileInputStream)) { 
            Iterator<Map<String, String>> itr = reader.iterator();
            while (itr.hasNext()) {
                Map<String, String> chunk = itr.next();
                if(chunk.get("u") != null) {
                    String[] tokens = (chunk.get("u").split("\\|"));
                    String[] arti = (chunk.get("i").split("\\|"));
                    String [] newList = new String[5];
                    System.arraycopy(tokens, 0, newList, 0, 3);
                    String epochDate = chunk.get("m");
                    newList[3] = epochDate;
                    newList[4] = arti[arti.length - 1];
                    indexInfo.add(newList);

                    // Insert batch into database
                    if (indexInfo.size() == BATCH_SIZE) {
                        putInDatabase(indexInfo);
                        indexInfo.clear();
                    }
                }
            }
        }

        // Remainder that is not part of a batch
        putInDatabase(indexInfo);
    }

    public void putInDatabase(List<String[]> indexedInfo) throws SQLException {
        for (String[] info : indexedInfo) {
            db.updateIndexTable(info[0], info[1], info[2], convertToDate(info[3]), info[4]);
        }
    }

    public Date convertToDate(String epochDate) {
        return new Date(Long.parseLong(epochDate));
    }
}


