package nl.tudelft;

import org.apache.maven.model.Model;

import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DemoExtractor implements Extractor {
    private Field[] fields;
    private int lengthOfFields;

    public DemoExtractor() {
        this.fields = new Field[lengthOfFields];
        this.lengthOfFields = 3;
    }

    @Override
    public Field[] fields() {
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
        JarFile jar = pkg.getJar();
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
        fields[1] = new Field("numberOfFiles", "INT");
        fields[2] = new Field("size", "BIGINT");
        extractedFields[1] = numberOfFiles;
        extractedFields[2] = size;
    }

    private void extractFromPom(Package pkg, Object[] extractedFields) {
        Model model = pkg.getPom();
        String nameOfFile = model.getName();
        Field field = new Field ("fileName", "VARCHAR");
        fields[0] = field;
        extractedFields[0] = nameOfFile;
    }

}
