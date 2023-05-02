package nl.tudelft;

import org.apache.maven.model.Model;

import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public class DemoExtractor implements Extractor {
    private Field[] fields;

    public DemoExtractor() {
        this.fields = new Field[2];
    }

    @Override
    public Field[] fields() {
        return fields;
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg) {
        Object[] extractedFields = new Object[2];
        extractFromPom(pkg, extractedFields);
        extractFromJar(pkg, extractedFields);
        return extractedFields;
    }

    private void extractFromJar(Package pkg, Object[] extractedFields) {
        JarFile jar = pkg.getJar();
        Enumeration<JarEntry> enumerator = jar.entries();
        int numberOfFiles = 0;
        while(enumerator.hasMoreElements()) {
            JarEntry entry = enumerator.nextElement();
            if(!entry.isDirectory()) {
                numberOfFiles++;
            }

        }
        fields[1] = new Field("numberOfFiles", "INT");
        extractedFields[1] = numberOfFiles;
    }

    public void extractFromPom(Package pkg, Object[] extractedFields) {
        Model model = pkg.getPom();
        String nameOfFile = model.getName();
        Field field = new Field ("fileName", "VARCHAR");
        fields[0] = field;
        extractedFields[0] = nameOfFile;
    }

}
