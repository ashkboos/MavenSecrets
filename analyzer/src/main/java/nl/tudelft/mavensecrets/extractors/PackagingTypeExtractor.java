package nl.tudelft.mavensecrets.extractors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import nl.tudelft.*;
import nl.tudelft.Package;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Model;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class PackagingTypeExtractor implements Extractor {
    private static final Logger LOGGER = LogManager.getLogger(PackagingTypeExtractor.class);
    private final Field[] fields;

    public PackagingTypeExtractor() {
        this.fields = new Field[]{
            new Field ("packagingtypefrompom", "VARCHAR(128)"),
            new Field("packagingtypefromrepo", "VARCHAR(128)"),
            new Field("qualifiersources", "VARCHAR(128)"),
            new Field("qualifierjavadoc", "VARCHAR(128)"),
            new Field("md5", "VARCHAR"),
            new Field("sha1", "VARCHAR"),
            new Field("sha256", "VARCHAR"),
            new Field("sha512", "VARCHAR"),
            new Field("typesoffile", "VARCHAR(4096)"),
            new Field("allqualifiers", "VARCHAR"),
            new Field("allpackagingtype", "VARCHAR"),
            new Field("allchecksum", "VARCHAR")
        };
    }

    @Override
    public Field[] fields() {
        return fields;
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) {
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

        List<String> allArtifacts = request(pkg.id());

        Set<String> allQualifiers = new HashSet<>();
        Set<String> allTypesOfExecutable = new HashSet<>();
        Set<String> allTypesOfCheckSum = new HashSet<>();

        extractQualifier(pkg.id(), allArtifacts, allQualifiers);

        extractExecutableTypeAndCheckSum(pkg.id(), allArtifacts, allTypesOfExecutable, allTypesOfCheckSum);


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

        addCheckSumType(extractedFields, artifactWithMd5);

        addCheckSumType(extractedFields, artifactWithSha1);

        addCheckSumType(extractedFields, artifactWithSha256);

        addCheckSumType(extractedFields, artifactWithSha512);

        extractedFields.add(allFiles.toString());

        extractedFields.add(allQualifiers.toString());

        extractedFields.add(allTypesOfExecutable.toString());

        extractedFields.add(allTypesOfCheckSum.toString());

        return extractedFields.toArray();
    }

    public void extractExecutableTypeAndCheckSum(PackageId id, List<String> allArtifacts, Set<String> allTypesOfExecutable, Set<String> allTypesOfCheckSum) {
        String baseName = id.artifact() + "-" + id.version() + ".";

        for (String filename : allArtifacts) {
            String restOfFileName = "";
            int startIndex = filename.indexOf(baseName) + baseName.length();

            if (startIndex != -1 && startIndex < filename.length()) {
                restOfFileName = filename.substring(startIndex);
            }

            if(restOfFileName.contains(".asc") || restOfFileName.contains("-")) {
                continue;
            }

            int endIndex = restOfFileName.indexOf(".");
            int lastIndex = restOfFileName.lastIndexOf(".");

            if(endIndex == -1) {
                allTypesOfExecutable.add(restOfFileName);
            }
            if(endIndex != lastIndex) {
                allTypesOfExecutable.add(restOfFileName.substring(0, lastIndex));
            }

            if(restOfFileName.endsWith("md5") || restOfFileName.endsWith("sha1")
            || restOfFileName.endsWith("sha256") || restOfFileName.endsWith("sha512")) {
                allTypesOfCheckSum.add(restOfFileName.substring(lastIndex));
            }

        }
        if(allTypesOfExecutable.size() > 1) {
            allTypesOfExecutable.remove("pom");
        }
    }

    public void extractQualifier(PackageId id, List<String> allArtifacts, Set<String> allQualifiers) {
        String baseName = id.artifact() + "-" + id.version();
        String restOfFileName = "";
        for(String filename : allArtifacts) {
            int startIndex = filename.indexOf(baseName) + baseName.length();

            if (startIndex != -1 && startIndex < filename.length()) {
                restOfFileName = filename.substring(startIndex);
            }

            if(restOfFileName.contains(".asc")) {
                continue;
            }

            int start = restOfFileName.indexOf("-");
            int endIndex = restOfFileName.indexOf(".");

            if (start != -1 && endIndex != -1) {
                allQualifiers.add(restOfFileName.substring(start + 1, endIndex));
            }
        }
    }

    private Artifact getCheckSumArtifact(Maven mvn, Package pkg, String fileExtension, String checksumType) {
        Artifact artifact;
        try {
            artifact = mvn.getArtifactChecksum(pkg.id(), fileExtension + checksumType);
        } catch (PackageException | ArtifactResolutionException e) {
            LOGGER.trace("{} artifact not found ({})", checksumType, pkg.id(), e);
            artifact = null;
        }
        return artifact;
    }

    private Artifact getQualifierArtifact(Maven mvn, Package pkg, String qualifierName) {
        Artifact artifact;
        try {
            // The source files and javadoc files are always packaged as "jar"
            artifact = mvn.getArtifactQualifier(pkg.id(), qualifierName, "jar");
        } catch (PackageException | ArtifactResolutionException e) {
            LOGGER.trace("{} artifact not found ({})", qualifierName, pkg.id(), e);
            artifact = null;
        }
        return artifact;
    }

    private void addQualifier(List<Object> extractedFields, Artifact artifact) {
        Objects.requireNonNull(extractedFields);

        extractedFields.add(artifact == null ? null : artifact.getClassifier());
    }

    private void addCheckSumType(List<Object> extractedFields, Artifact artifact) {
        Objects.requireNonNull(extractedFields);

        String checksum;
        try {
            checksum = artifact == null ? null : readChecksum(artifact);
        } catch (IOException exception) {
            LOGGER.warn("Could not read checksum file", exception);
            checksum = null;
        }
        extractedFields.add(checksum);
    }

    public Set<String> getFilesFromExecutable(JarFile file) {
        // Sanity check
        if (file == null) {
            return new HashSet<>();
        }

        Set<String> fileTypes = new HashSet<>();

        Enumeration<JarEntry> entries = file.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (!entry.isDirectory()) {
                String entryName = entry.getName();
                int lastDashIndex = entryName.lastIndexOf('/');
                if (lastDashIndex != entryName.length() - 1) {
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

    private String readChecksum(Artifact artifact) throws IOException {
        String file = artifact.getFile().getPath();
        LOGGER.debug("Jar name = {}", file);
        String checksum;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            checksum = reader.readLine().split("\\s+")[0];
        }
        return checksum;
    }

    public List<String> request(PackageId packageId) {
        String repositoryUrl = "https://repo.maven.apache.org/maven2/";
        String url = repositoryUrl + packageId.group().replace('.', '/')
            + "/" + packageId.artifact() + "/" + packageId.version();

        List<String> allFiles = new ArrayList<>();
        try {
            // Send HTTP request and retrieve the response

            Document document = Jsoup.connect(url).get();

            // Find the <pre> element with the id "contents"
            Elements contentsElement = document.select("pre#contents");

            // Extract the <a> elements within the <pre> element
            Elements linkElements = contentsElement.select("a[href]");

            // Iterate over the link elements and print the titles
            for (Element linkElement : linkElements) {
                String title = linkElement.attr("href");
                if (title.endsWith("/")) {  // Skip directories
                    continue;
                }
                allFiles.add(title);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return allFiles;
    }
}
