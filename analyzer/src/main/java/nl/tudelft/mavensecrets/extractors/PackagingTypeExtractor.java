package nl.tudelft.mavensecrets.extractors;

import java.util.*;
import java.util.jar.JarFile;
import nl.tudelft.*;
import nl.tudelft.Package;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;

public class PackagingTypeExtractor implements Extractor {
    private static final Logger LOGGER = LogManager.getLogger(PackagingTypeExtractor.class);
    private final Field[] fields;

    public PackagingTypeExtractor() {
        this.fields = new Field[]{
            new Field ("packagingtypefrompom", "VARCHAR(128)"),
            new Field("packagingtypefromrepo", "VARCHAR(128)"),
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
        JarFile file = pkg.jar();
        String packagingType = model.getPackaging();

        Artifact artifactSources;
        Artifact artifactJavadoc;
        Artifact artifactWithMd5;
        Artifact artifactWithSha1;

        String fileExtension = file.getName().substring(file.getName().lastIndexOf('.') + 1);


        artifactSources = getQualifierArtifact(mvn, pkg, fileExtension, "sources");

        artifactJavadoc = getQualifierArtifact(mvn, pkg, fileExtension, "javadoc");

        artifactWithMd5 = getCheckSumArtifact(mvn, pkg, fileExtension, ".md5");

        artifactWithSha1 = getCheckSumArtifact(mvn, pkg, fileExtension, ".sha1");


        extractedFields.add(packagingType);

        extractedFields.add(fileExtension);

        addQualifier(extractedFields, artifactSources);

        addQualifier(extractedFields, artifactJavadoc);

        addCheckSumType(extractedFields, artifactWithMd5, ".md5");

        addCheckSumType(extractedFields, artifactWithSha1, ".sha1");

        return extractedFields.toArray();
    }

    private Artifact getCheckSumArtifact(Maven mvn, Package pkg, String fileExtension, String checksumType) {
        Artifact artifact = null;
        try {
            artifact = mvn.getArtifactChecksum(pkg.id(), fileExtension + checksumType);
        } catch (PackageException | ArtifactResolutionException e) {
            LOGGER.error(checksumType + " artifact not found", e);
        }
        return artifact;
    }

    private Artifact getQualifierArtifact(Maven mvn, Package pkg, String fileExtension, String qualifierName) {
        Artifact artifact = null;
        try {
            artifact = mvn.getArtifactQualifier(pkg.id(), qualifierName, fileExtension);
        } catch (PackageException | ArtifactResolutionException e) {
            LOGGER.error(qualifierName + " artifact not found", e);
        }
        return artifact;
    }

    private void addQualifier(List<Object> extractedFields, Artifact artifact) {
        if (artifact != null) {
            extractedFields.add(artifact.getClassifier());
        } else {
            extractedFields.add("null");
        }
    }

    private void addCheckSumType(List<Object> extractedFields, Artifact artifact, String checksumType) {
        if(artifact != null) {
            extractedFields.add(checksumType.toUpperCase());
        } else {
            extractedFields.add("null");
        }
    }
}