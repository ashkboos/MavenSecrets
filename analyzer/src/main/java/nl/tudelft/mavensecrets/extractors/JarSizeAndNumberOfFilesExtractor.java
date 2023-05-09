package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.*;
import nl.tudelft.Package;

import nl.tudelft.Database;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * An extractor fetching Jar Size and number of files
 */
public class JarSizeAndNumberOfFilesExtractor implements Extractor {
    private final Database db;
    private Boolean checked;

    private final Field[] fields = {
            new Field("size", "BIGINT"),
            new Field("numberoffiles", "INTEGER")
    };

    public JarSizeAndNumberOfFilesExtractor(Database db) {
        this.db = db;
        checked = false;
    }

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg) throws IOException, SQLException {
        Map<String, Integer> extensionMap;
        Map<String, Long> extensionSizeMap = new HashMap<>();
        List<Field> extensionFields = new ArrayList<>();
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);
        List<Object> result = new ArrayList<>();
        Object[] sizeAndNumber = new Object[2];
        JarFile jar = pkg.jar();
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
                    if(extensionSizeMap.containsKey(ext)) {
                        extensionSizeMap.put(ext, extensionSizeMap.get(ext) + entry.getSize());
                    } else extensionSizeMap.put(ext, entry.getSize());
                    extensions.add(ext);
                } else if(name[name.length - 1].contains("-")) {
                    String ext = "containshyphen";
                    if(extensionSizeMap.containsKey(ext)) {
                        extensionSizeMap.put(ext, extensionSizeMap.get(ext) + entry.getSize());
                    } else extensionSizeMap.put(ext, entry.getSize());
                    extensions.add(ext);
                } else {
                    String ext = name[name.length - 1].toLowerCase();
                    if(extensionSizeMap.containsKey(ext)) {
                        extensionSizeMap.put(ext, extensionSizeMap.get(ext) + entry.getSize());
                    } else extensionSizeMap.put(ext, entry.getSize());
                    extensions.add(ext);
                }
            }
            if(!entry.isDirectory()) {
                numberOfFiles++;
            }
        }
        sizeAndNumber[0] = size;
        sizeAndNumber[1] = numberOfFiles;
        extensionMap = extensionsToMap(extensions);
        for(Map.Entry<String, Integer> t : extensionMap.entrySet()) {
            String extension = t.getKey();
            int count = t.getValue();
            extensionFields.add(new Field(extension, "INTEGER"));
            result.add(count);
        }
        for(Map.Entry<String, Long> t : extensionSizeMap.entrySet()) {
            String extension = t.getKey() + "size";
            long totalExtSize = t.getValue();
            extensionFields.add(new Field(extension, "BIGINT"));
            result.add(totalExtSize);
        }
        extensionDatabase(db, checked, extensionFields.toArray(new Field[0]), result.toArray(), pkg.id());
        checked = true;
        return sizeAndNumber;
    }

    void extensionDatabase(Database db, boolean checked, Field[] fields, Object[] values, PackageId id) throws SQLException {
        db.createExtensionTable(checked);
        db.updateExtensionSchema(fields);
        db.update(id, fields, values, false);
    }

    /**
     * Turns list into map, keeping track of number of occurrences
     *
     * @param extensions List of file extensions
     * @return map containing every unique extension in the list with the number of occurrences
     */
    private Map<String, Integer> extensionsToMap (List<String> extensions) {
        Map<String, Integer> result = new HashMap<>();
        for(String extension : extensions) {
            if(result.containsKey(extension)) {
                result.put(extension, result.get(extension) + 1);
            } else {
                result.put(extension, 1);
            }
        }
        return result;
    }
}
