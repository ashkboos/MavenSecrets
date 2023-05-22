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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.index.reader.ChunkReader;

public class IndexerReader {
    Logger LOGGER = LogManager.getLogger(IndexerReader.class);
    private final Database db;

    IndexerReader(Database db) {
        this.db = db;
    }

    public void indexerReader(String indexFile) throws IOException, SQLException {
        boolean checked = false;
        File file = new File(indexFile);
        List<String[]> indexInfo = new ArrayList<>();
        try (FileInputStream fileInputStream = new FileInputStream(file);
             ChunkReader reader = new ChunkReader("index", fileInputStream)) {
            Iterator<Map<String, String>> itr = reader.iterator();
            int i = 0;
            while (itr.hasNext() && i < 2048) {
                Map<String, String> chunk = itr.next();
                if (chunk.get("u") != null) {
                    String[] tokens = (chunk.get("u").split("\\|"));
                    String[] arti = (chunk.get("i").split("\\|"));
                    String[] newList = new String[5];
                    System.arraycopy(tokens, 0, newList, 0, 3);
                    String epochDate = chunk.get("m");
                    newList[3] = epochDate;
                    newList[4] = arti[arti.length - 1];
                    //TODO - DELETE BEFORE MERGING IN DEV
                    if(newList[1].equals("yamcs-api")) {
                        System.out.println("kkkk");
                    }
                    if (!newList[4].contains(".") && tokens[3].equals("NA")) {
                        indexInfo.add(newList);
                        i++;
                    }
                    if (i == 2048) {
                        putInDatabase(indexInfo, checked);
                        checked = true;
                        i = 0;
                        indexInfo = new ArrayList<>();
                    }
                }
            }
            putInDatabase(indexInfo, checked);
        }
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


