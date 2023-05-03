package nl.tudelft;
import org.apache.maven.index.reader.ChunkReader;

import java.io.*;
import java.sql.SQLException;
import java.util.*;
import java.sql.Date;

public class IndexerReader {
    private final Database db;

    IndexerReader(Database db) {
        this.db = db;
    }

    public static List<String[]> indexerReader() throws IOException {
        File file = new File("nexus-maven-repository-index.gz");
        FileInputStream fileInputStream = new FileInputStream(file);
        ChunkReader reader = new ChunkReader("index", fileInputStream);
        Iterator<Map<String, String>> itr = reader.iterator();
        List<String[]> indexInfo = new ArrayList<>();
        while (itr.hasNext()) {
            Map<String, String> chunk = itr.next();
            if(chunk.get("u") != null) {
                String[] tokens = (chunk.get("u").split("\\|"));
                String [] newList = new String[4];
                System.arraycopy(tokens, 0, newList, 0, 3);
                String epochDate = chunk.get("m");
                newList[3] = epochDate;
                indexInfo.add(newList);
            }
        }
        reader.close();
        return indexInfo;
    }

    public void putInDatabase() throws IOException, SQLException {
        db.createIndexesTable();
        List<String[]> indexedInfo = indexerReader();
        for(String[] info : indexedInfo) {
            db.updateIndexTable(info[0], info[1], info[2], convertToDate(info[3]));
        }

    }

    public Date convertToDate(String epochDate) {
        return new Date(Long.parseLong(epochDate));
    }
}


