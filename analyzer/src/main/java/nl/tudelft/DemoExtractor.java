package nl.tudelft;

import org.apache.maven.model.Model;

import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DemoExtractor implements Extractor {
    private Field[] fields;
    private int lengthOfFields;

    public DemoExtractor() {
        this.lengthOfFields = 3;
        this.fields = new Field[lengthOfFields];
    }

    @Override
    public Field[] fields() {
        fields[0] = new Field ("filename", "VARCHAR(128)");
        fields[1] = new Field("numberoffiles", "INTEGER");
        fields[2] = new Field("size", "BIGINT");
        return fields;
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg) {
        Object[] extractedFields = new Object[lengthOfFields];
        extractFromPom(pkg, extractedFields);
        extractFromJar(pkg, extractedFields);
        return extractedFields;
    }

    private void extractFromJar(Package pkg, Object[] extractedFields) {
        JarFile jar = pkg.jar();
        long size = 0;
        Enumeration<JarEntry> enumerator = jar.entries();
        int numberOfFiles = 0;
        while(enumerator.hasMoreElements()) {
            JarEntry entry = enumerator.nextElement();
            size += entry.getSize();
            if(!entry.isDirectory()) {
                numberOfFiles++;
            }
        }
        extractedFields[1] = numberOfFiles;
        extractedFields[2] = size;
    }

    private void extractFromPom(Package pkg, Object[] extractedFields) {
        Model model = pkg.pom();
        String nameOfFile = model.getName();
        extractedFields[0] = nameOfFile;
    }

}
