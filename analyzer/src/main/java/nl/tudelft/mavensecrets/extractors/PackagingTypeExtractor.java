package nl.tudelft.mavensecrets.extractors;

import java.io.File;
import java.util.*;
import nl.tudelft.*;
import nl.tudelft.Package;
import org.apache.commons.io.FilenameUtils;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;

public class PackagingTypeExtractor implements Extractor {
    private final Field[] fields;

    public PackagingTypeExtractor() {
        this.fields = new Field[]{
            new Field ("packagingtype", "VARCHAR(128)"),
            new Field("qualifiersources", "VARCHAR(128)"),
            new Field("qualifierjavadoc", "VARCHAR(128)"),
            new Field("md5", "VARCHAR(128)"),
            new Field("sha1", "VARCHAR(128)")
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

        int fa = 0;
        int ga = 0;
        if(artifactWithSources != null) {
            for (Artifact a : artifactWithSources) {
                File f = a.getFile();
                String checkSumFilePath =
                    f.getParent() + File.separator + FilenameUtils.getBaseName(f.getName());

                if(new File(checkSumFilePath + ".md5").exists() && fa != 1) {
                    extractedFields.add("MD5");
                    fa = 1;
                }

                if(new File(checkSumFilePath + ".sha1").exists() && ga != 1) {
                    extractedFields.add("SHA1");
                    ga = 1;
                }
            }
        }

        if(fa == 0) {
            extractedFields.add("null");
        }

        if(ga == 0) {
            extractedFields.add("null");
        }

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
