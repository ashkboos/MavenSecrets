package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.mavensecrets.Package;
import nl.tudelft.mavensecrets.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * An extractor fetching Jar Size and number of files
 */
public class SizeExtractor implements Extractor {
    //for test purposes only
    private Map<String, ExtensionInfo> extensionsTesting;

    private final Field[] fields = {
            new Field("size", "BIGINT"),
            new Field("numberoffiles", "INTEGER")
    };

    public SizeExtractor() {
        extensionsTesting = new HashMap<>();
    }

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) throws IOException, SQLException {
        List<ExtensionTuple> extensionTuples = new ArrayList<>();
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);
        Object[] sizeAndNumber = new Object[2];
        JarFile jar = pkg.jar();

        // Sanity check
        if (jar == null) {
            return new Object[fields.length];
        }

        jar.size();
        long size = 0;
        Enumeration<JarEntry> enumerator = jar.entries();
        int numberOfFiles = 0;
        List<String> extensions = new ArrayList<String>();
        while(enumerator.hasMoreElements()) {
            JarEntry entry = enumerator.nextElement();
            size += entry.getSize();
            String[] name = entry.getName().split("\\.");

            if(!entry.isDirectory()) {
                if(name[name.length - 1].contains("/")) {
                    String ext = "noextension";
                    extensionTuples.add(new ExtensionTuple(ext, entry.getSize()));
                    extensions.add(ext);
                } else if(name[name.length - 1].contains("-")) {
                    String ext = "containshyphen";
                    extensionTuples.add(new ExtensionTuple(ext, entry.getSize()));
                    extensions.add(ext);
                } else {
                    String ext = name[name.length - 1].toLowerCase();
                    extensionTuples.add(new ExtensionTuple(ext, entry.getSize()));
                    extensions.add(ext);
                }
            }
            if(!entry.isDirectory()) {
                numberOfFiles++;
            }
        }
        sizeAndNumber[0] = size;
        sizeAndNumber[1] = numberOfFiles;
        Map<String, ExtensionInfo> extensionInfo = computeExtensionInfo(extensionTuples);
        extensionsTesting = extensionInfo;
        for(Map.Entry<String, ExtensionInfo> t : extensionInfo.entrySet()) {
            String extension = t.getKey();
            ExtensionInfo info = t.getValue();
            extensionDatabase(db, extension, info.count, info.total, info.min, info.max, info.median, pkg.id());
        }
        return sizeAndNumber;
    }

    public void extensionDatabase(Database db, String extension, long count, long total, long min, long max, long median, PackageId id) throws SQLException {
        db.updateExtensionTable(id.toString(),
                extension,
                count,
                total,
                min,
                max,
                median
        );
    }

    private Map<String, ExtensionInfo> computeExtensionInfo (List<ExtensionTuple> extensionTuples) {
        Map<String, ExtensionInfo> result = new HashMap<>();
        Map<String, ArrayList<Long>> sizes = new HashMap<>();
        for(ExtensionTuple e : extensionTuples) {
            if(result.containsKey(e.extension)) {
                sizes.get(e.extension).add(e.size);
                ExtensionInfo info = result.get(e.extension);
                info.updateTotal(e.size);
                info.updateMin(e.size);
                info.updateMax(e.size);
                info.updateCount();
                result.put(e.extension, info);
            } else {
                ArrayList<Long> list = new ArrayList<>();
                list.add(e.size);
                sizes.put(e.extension, list);
                result.put(e.extension, new ExtensionInfo(e.size, e.size, e.size, e.size, e.size));
            }
        }

        //Compute median for every extension
        for(Map.Entry<String, ArrayList<Long>> entry  : sizes.entrySet()) {
            //ExtensionInfo info = result.get(entry.getKey());
            //long mean = info.total / info.count;
            List<Long> entrySizes = entry.getValue();
            Collections.sort(entrySizes);
            long median;
            if(entrySizes.size() % 2 == 0) {
                median = (entrySizes.get(entrySizes.size() / 2) + (entrySizes.get(entrySizes.size() / 2 - 1))) / 2;
            } else  {
                median = entrySizes.get(entrySizes.size() / 2);
            }
            result.get(entry.getKey()).updateMedian(median);
        }
        return result;
    }

    //for test purposes only
    public Map<String, ExtensionInfo> getExtensionsTesting() {
        return this.extensionsTesting;
    }

    public class ExtensionTuple {
        String extension;
        long size;

        public ExtensionTuple(String extension, long size) {
            this.extension = extension;
            this.size = size;
        }
    }

    public class ExtensionInfo {
        long total;
        long min;
        long max;
        long std;
        long median;
        long count;

        public ExtensionInfo(long total, long min, long max, long std, long median) {
            this.total = total;
            this.min = min;
            this.max = max;
            this.std = std;
            this.median = median;
            this.count = 1;
        }
        void updateTotal(long other) {
            this.total += other;
        }

        void updateMin(long other) {
            if(other < this.min) {
                this.min = other;
            }
        }

        void updateMax(long other) {
            if(other > this.max) {
                this.max = other;
            }
        }

        void updateMedian(long other) {
            this.median = other;
        }

        void updateCount() {
            this.count++;
        }

        void updateStd(long other) {
            this.std = other;
        }
    }
}