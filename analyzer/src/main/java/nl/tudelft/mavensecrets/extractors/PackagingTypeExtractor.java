package nl.tudelft.mavensecrets.extractors;

import java.util.*;
import nl.tudelft.*;
import nl.tudelft.Package;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;

public class PackagingTypeExtractor implements Extractor {
    private final Field[] fields;

    public PackagingTypeExtractor() {
        this.fields = new Field[]{
            new Field ("packagingtype", "VARCHAR(128)"),
            new Field("qualifiersources", "VARCHAR(128)"),
            new Field("qualifierjavadoc", "VARCHAR(128)")
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
        int check = 0;

        List<Artifact> artifactWithSources = null;
        List<Artifact> artifactWithJavadoc = null;
        try {
            artifactWithSources = mvn.getArtifactSources(pkg.id());
            artifactWithJavadoc = mvn.getArtifactJavaDoc(pkg.id());
        } catch (PackageException | ArtifactResolutionException e) {
            e.printStackTrace();
        }

        extractedFields.add(packagingType);

        addQualifier(extractedFields, 0, artifactWithSources);

        addQualifier(extractedFields, 0, artifactWithJavadoc);

        return extractedFields.toArray();
    }

    private void addQualifier(List<Object> extractedFields, int check, List<Artifact> artifacts) {
        if (artifacts != null) {
            for (Artifact artifact : artifacts) {
                if (artifact != null) {
                    extractedFields.add(artifact.getClassifier());
                    check++;
                    break;
                }
            }
        }

        if(check == 0) {
            extractedFields.add("null");
        }
    }
}
