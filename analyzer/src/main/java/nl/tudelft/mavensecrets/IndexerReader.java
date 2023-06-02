package nl.tudelft.mavensecrets;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
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
        db.createIndexesTableWithAllPackaging(false);

        List<String[]> indexInfo = new ArrayList<>();

        try (FileInputStream fileInputStream = new FileInputStream(file);
            ChunkReader reader = new ChunkReader("index", fileInputStream)) {
            for (Map<String, String> chunk : reader) {
                if (chunk.get("u") != null) {
                    String[] tokens = (chunk.get("u").split("\\|"));
                    String[] arti = (chunk.get("i").split("\\|"));
                    String[] newList = new String[5];
                    System.arraycopy(tokens, 0, newList, 0, 3);
                    String epochDate = chunk.get("m");
                    newList[3] = epochDate;
                    newList[4] = arti[arti.length - 1];
                    if(newList[1].equals("presto-benchto-benchmarks")) {
                        System.out.println(Arrays.toString(newList));
                    }
                    if (!newList[4].contains(".") && tokens[3].equals("NA")) {
                        indexInfo.add(newList);
                    }
                    // Insert batch into database
                    if (indexInfo.size() == BATCH_SIZE) {
                        db.batchUpdateIndexTable(indexInfo);
                        db.batchUpdateIndexTableWithPackaging(indexInfo);
                        indexInfo.clear();
                    }
                }
            }
        }

        // Remainder that is not part of a batch
        db.batchUpdateIndexTable(indexInfo);
        db.batchUpdateIndexTableWithPackaging(indexInfo);
    }

}


