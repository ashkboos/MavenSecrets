package nl.tudelft.mavensecrets.extractors;

import java.util.*;
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

        Artifact executableArtifact = null;
        Artifact artifactSources = null;
        Artifact artifactJavadoc = null;
        Artifact artifactWithMd5 = null;
        Artifact artifactWithSha1 = null;

        try {
            executableArtifact = mvn.getArtifact(pkg.id(), "jar");
        } catch (ArtifactResolutionException e) {
            LOGGER.error("Jar artifact not found", e);
            try {
                executableArtifact = mvn.getArtifact(pkg.id(), "war");
            } catch (ArtifactResolutionException e1) {
                LOGGER.error("War artifact not found", e1);
                try {
                    executableArtifact = mvn.getArtifact(pkg.id(), "ear");
                } catch (ArtifactResolutionException e2) {
                    LOGGER.error("Ear artifact not found", e2);
                    try {
                        executableArtifact = mvn.getArtifact(pkg.id(), "zip");
                    } catch (ArtifactResolutionException e3) {
                        LOGGER.error("Zip artifact not found", e3);
                    }
                }
            }
        }

        assert executableArtifact != null;
        LOGGER.info(executableArtifact.getExtension() + " EXTENSIONNN");

        try {
            artifactSources = mvn.getArtifactQualifier(pkg.id(), "sources");
        } catch (PackageException | ArtifactResolutionException e) {
            LOGGER.error("Source artifact not found", e);
        }

        try {
            artifactJavadoc = mvn.getArtifactQualifier(pkg.id(), "javadoc");
        } catch (PackageException | ArtifactResolutionException e) {
            LOGGER.error("Javadoc artifact not found", e);
        }

        try {
            artifactWithMd5 = mvn.getArtifactChecksum(pkg.id(), "jar.md5");
        } catch (PackageException | ArtifactResolutionException e) {
            LOGGER.error("MD5 artifact not found", e);
        }

        try {
            artifactWithSha1 = mvn.getArtifactChecksum(pkg.id(), "jar.sha1");
        } catch (PackageException | ArtifactResolutionException e) {
            LOGGER.error("SHA1 artifact not found", e);
        }

        extractedFields.add(packagingType);

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
            extractedFields.add("null 12");
        }
    }
}
