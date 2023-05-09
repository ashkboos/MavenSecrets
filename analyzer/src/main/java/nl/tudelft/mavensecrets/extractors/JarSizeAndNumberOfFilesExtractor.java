package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.Extractor;
import nl.tudelft.Field;
import nl.tudelft.Maven;
import nl.tudelft.Package;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Objects;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * An extractor fetching Jar Size and number of files
 */
public class JarSizeAndNumberOfFilesExtractor implements Extractor {
    private final Field[] fields = {
            new Field("size", "BIGINT"),
            new Field("numberoffiles", "INTEGER"),
            new Field("extension", "VARCHAR(128)")
    };

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg) throws IOException {
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);
        Object[] result = new Object[3];
        JarFile jar = pkg.jar();
        jar.size();
        long size = 0;
        Enumeration<JarEntry> enumerator = jar.entries();
        int numberOfFiles = 0;
        while(enumerator.hasMoreElements()) {
            JarEntry entry = enumerator.nextElement();
            size += entry.getSize();
            String[] name = entry.getName().split("\\.");

            if(!entry.isDirectory()) {
                String extension = name[name.length - 1];
                result[2] = extension;
            }

            if(!entry.isDirectory()) {
                numberOfFiles++;
            }
        }
        result[0] = size;
        result[1] = numberOfFiles;

        return result;
    }
}
