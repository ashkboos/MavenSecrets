package nl.tudelft.mavensecrets.extractors;

import java.io.File;
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

        Artifact artifactSources = null;
        Artifact artifactJavadoc = null;
        Artifact artifactWithMd5 = null;
        Artifact artifactWithSha1 = null;

        String fileExtension = file.getName().substring(file.getName().lastIndexOf('.') + 1);

        try {
            artifactSources = mvn.getArtifactQualifier(pkg.id(), "sources", fileExtension);
        } catch (PackageException | ArtifactResolutionException e) {
            LOGGER.error("Source artifact not found", e);
        }

        try {
            artifactJavadoc = mvn.getArtifactQualifier(pkg.id(), "javadoc", fileExtension);
        } catch (PackageException | ArtifactResolutionException e) {
            LOGGER.error("Javadoc artifact not found", e);
        }

        try {
            artifactWithMd5 = mvn.getArtifactChecksum(pkg.id(), fileExtension + ".md5");
        } catch (PackageException | ArtifactResolutionException e) {
            LOGGER.error("MD5 artifact not found", e);
        }

        try {
            artifactWithSha1 = mvn.getArtifactChecksum(pkg.id(), fileExtension + ".sha1");
        } catch (PackageException | ArtifactResolutionException e) {
            LOGGER.error("SHA1 artifact not found", e);
        }

        extractedFields.add(packagingType);

        extractedFields.add(fileExtension);

        addQualifier(extractedFields, artifactSources);

        addQualifier(extractedFields, artifactJavadoc);

        addCheckSumType(extractedFields, artifactWithMd5, ".md5");

        addCheckSumType(extractedFields, artifactWithSha1, ".sha1");

        return extractedFields.toArray();
    }

    private void addQualifier(List<Object> extractedFields, Artifact artifact) {
        if (artifact != null) {
            extractedFields.add(artifact.getClassifier());
        } else {
            extractedFields.add("null");
        }
    }

    private void addCheckSumType(List<Object> extractedFields, Artifact artifact, String type) {
        if(artifact != null) {
            extractedFields.add(type.toUpperCase());
        } else {
            extractedFields.add("null");
        }
    }
}
