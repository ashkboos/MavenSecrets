package nl.tudelft.mavensecrets.extractors;

import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import nl.tudelft.Extractor;
import nl.tudelft.Field;
import nl.tudelft.Maven;
import nl.tudelft.Package;
import nl.tudelft.mavensecrets.resolver.DefaultResolver;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;

public class PackagingTypeExtractor implements Extractor {
    private final Field[] fields;

    public PackagingTypeExtractor() {
        this.fields = new Field[]{
            new Field ("packagingtype", "VARCHAR(128)"),
            new Field("qualifier", "VARCHAR(128)")
        };
    }

    @Override
    public Field[] fields() {
        return fields;
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg) {
        List<Object> extractedFields = new ArrayList<>();
        Model model = pkg.pom();
        String packagingType = model.getPackaging();

        StringBuilder allQualifiers = new StringBuilder();
        for(Dependency d : model.getDependencies()) {
            String qualifier = d.getClassifier();
            if(qualifier != null)
                allQualifiers.append(qualifier).append(" ");
        }

        if(allQualifiers.toString().equals("")) {
            allQualifiers.append("null");
        }

        Artifact resolvedArtifact = null;
        try {
            DefaultResolver resolver = new DefaultResolver();
            Artifact artifact = resolver.createArtifact(model.getGroupId(), model.getArtifactId(), model.getVersion());
            resolvedArtifact = resolver.resolve(artifact);
        } catch (ArtifactResolutionException e) {
            e.printStackTrace();
        }

        extractedFields.add(packagingType);
        extractedFields.add(printUniqueWords(allQualifiers.toString()));
        //extractedFields.add(resolvedArtifact.getClassifier());

        return extractedFields.toArray();
    }

    private static String printUniqueWords(String str)
    {
        // Extracting words from string
        Pattern p = Pattern.compile("[a-zA-Z0-9]+");
        Matcher m = p.matcher(str);

        // Map to store count of a word
        Map<String, Integer> hm = new HashMap<>();

        // if a word found
        while (m.find())
        {
            String word = m.group();
            // If this is first occurrence of word
            if(!hm.containsKey(word))
                hm.put(word, hm.getOrDefault(word, 0) + 1);
        }

        Set<String> s = hm.keySet();

        StringBuilder uniqueWords = new StringBuilder();
        for (String w : s) {
            uniqueWords.append(w).append(" ");
        }

        return uniqueWords.toString();
    }

    private void extractFromJar(Package pkg, List<Object> extractedFields) {
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
        extractedFields.add(numberOfFiles);
        extractedFields.add(size);
    }

    private void extractFromPom(Package pkg, List<Object> extractedFields) {
        Model model = pkg.pom();
        String nameOfFile = model.getName();
        extractedFields.add(nameOfFile);
    }
}
