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

    public List<String[]> indexerReader(String indexFile) throws IOException, SQLException {
        boolean checked = false;
        File file = new File(indexFile);
        FileInputStream fileInputStream = new FileInputStream(file);
        ChunkReader reader = new ChunkReader("index", fileInputStream);
        Iterator<Map<String, String>> itr = reader.iterator();
        List<String[]> indexInfo = new ArrayList<>();
        int i = 0;
        while (itr.hasNext() && i < 2048) {
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
                i++;
                if(i == 2048) {
                    putInDatabase(indexInfo, checked);
                    checked = true;
                    i = 0;
                    indexInfo = new ArrayList<>();
                }
            }
        }
        reader.close();
        return indexInfo;
    }

    public void putInDatabase(List<String[]> indexedInfo, boolean checked) throws IOException, SQLException {
        db.createIndexesTable(checked);
        for(String[] info : indexedInfo) {
            db.updateIndexTable(info[0], info[1], info[2], convertToDate(info[3]), info[4]);
        }

    }

    public Date convertToDate(String epochDate) {
        return new Date(Long.parseLong(epochDate));
    }
}


