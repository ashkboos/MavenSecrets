package nl.tudelft.mavensecrets.extractors;

import java.util.*;
import java.util.jar.JarEntry;
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
            new Field("sha1", "VARCHAR(128)"),
            new Field("sha256", "VARCHAR(128)"),
            new Field("sha512", "VARCHAR(128)"),
            new Field("typesoffile", "VARCHAR(4096)")
        };
    }

    @Override
    public Field[] fields() {
        return fields;
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType) {
        List<Object> extractedFields = new ArrayList<>();
        Model model = pkg.pom();
        JarFile file = pkg.jar();
        String packagingTypeFromPom = model.getPackaging();

        Artifact artifactSources;
        Artifact artifactJavadoc;
        Artifact artifactWithMd5;
        Artifact artifactWithSha1;
        Artifact artifactWithSha256;
        Artifact artifactWithSha512;


        artifactSources = getQualifierArtifact(mvn, pkg, "sources");

        artifactJavadoc = getQualifierArtifact(mvn, pkg, "javadoc");

        artifactWithMd5 = getCheckSumArtifact(mvn, pkg, pkgType, ".md5");

        artifactWithSha1 = getCheckSumArtifact(mvn, pkg, pkgType, ".sha1");

        artifactWithSha256 = getCheckSumArtifact(mvn, pkg, pkgType, ".sha256");

        artifactWithSha512 = getCheckSumArtifact(mvn, pkg, pkgType, ".sha512");

        Set<String> allFiles = getFilesFromExecutable(file);


        extractedFields.add(packagingTypeFromPom);

        extractedFields.add(pkgType);


        addQualifier(extractedFields, artifactSources);

        addQualifier(extractedFields, artifactJavadoc);

        addCheckSumType(extractedFields, artifactWithMd5, ".md5");

        addCheckSumType(extractedFields, artifactWithSha1, ".sha1");

        addCheckSumType(extractedFields, artifactWithSha256, "sha256");

        addCheckSumType(extractedFields, artifactWithSha512, ".sha512");

        extractedFields.add(allFiles.toString());

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

    private Artifact getQualifierArtifact(Maven mvn, Package pkg, String qualifierName) {
        Artifact artifact = null;
        try {
            // The source files and javadoc files are always packaged as "jar"
            artifact = mvn.getArtifactQualifier(pkg.id(), qualifierName, "jar");
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

    private Set<String> getFilesFromExecutable(JarFile file) {
        Set<String> fileTypes = new HashSet<>();

        Enumeration<JarEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory()) {
                String entryName = entry.getName();
                int lastDashIndex = entryName.lastIndexOf('/');
                if (lastDashIndex != -1 && lastDashIndex != entryName.length() - 1) {
                    String fileName = entryName.substring(lastDashIndex + 1).toLowerCase();
                    String fileExtension;
                    if(fileName.lastIndexOf('.') != -1) {
                        fileExtension = fileName.substring(fileName.lastIndexOf('.') + 1);
                    } else {
                        fileExtension = "file";
                    }
                    fileTypes.add(fileExtension);
                }
            }
        }
        return fileTypes;
    }
}
