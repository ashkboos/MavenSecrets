package nl.tudelft.mavensecrets.extractors;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import nl.tudelft.*;
import nl.tudelft.Package;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.index.reader.ChunkReader;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;

public class PackagingTypeExtractor2 implements Extractor {
    private static final Logger LOGGER = LogManager.getLogger(PackagingTypeExtractor2.class);
    private final Field[] fields;

    public PackagingTypeExtractor2() {
        this.fields = new Field[]{
            new Field ("packagingtypefrompom", "VARCHAR(128)"),
            new Field("packagingtypefromrepo", "VARCHAR(128)"),
            new Field("qualifiersources", "VARCHAR(128)"),
            new Field("qualifierjavadoc", "VARCHAR(128)"),
            new Field("md5", "VARCHAR(128)"),
            new Field("sha1", "VARCHAR(128)"),
            new Field("typesoffile", "VARCHAR(4096)")
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

        String[] index = App.getArgs();
        String indexFile = index[1];


        try {
            Set<String[]> values = indexerReader(indexFile);
        } catch(IOException s) {
            LOGGER.error(s.getMessage());
        }


        artifactSources = getQualifierArtifact(mvn, pkg, "sources");

        artifactJavadoc = getQualifierArtifact(mvn, pkg, "javadoc");

        artifactWithMd5 = getCheckSumArtifact(mvn, pkg, fileExtension, ".md5");

        artifactWithSha1 = getCheckSumArtifact(mvn, pkg, fileExtension, ".sha1");

        Set<String> allFiles = getFilesFromExecutable(file);


        extractedFields.add(packagingType);

        extractedFields.add(fileExtension);

        addQualifier(extractedFields, artifactSources);

        addQualifier(extractedFields, artifactJavadoc);

        addCheckSumType(extractedFields, artifactWithMd5, ".md5");

        addCheckSumType(extractedFields, artifactWithSha1, ".sha1");

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

    public Set<String[]> indexerReader(String indexFile) throws IOException {
        File file = new File(indexFile);
        FileInputStream fileInputStream = new FileInputStream(file);
        ChunkReader reader = new ChunkReader("index", fileInputStream);
        Iterator<Map<String, String>> itr = reader.iterator();
        Set<String[]> artifactInfo = new HashSet<>();
        while (itr.hasNext()) {
            Map<String, String> chunk = itr.next();
            if(chunk.get("u") != null) {
                String[] tokens = (chunk.get("u").split("\\|"));
                String[] arti = (chunk.get("i").split("\\|"));
                String [] newArti = new String[4];
                System.arraycopy(tokens, 0, newArti, 0, 3);
                newArti[3] = arti[arti.length - 1];
                artifactInfo.add(newArti);
            }
        }
        reader.close();
        return artifactInfo;
    }

}
