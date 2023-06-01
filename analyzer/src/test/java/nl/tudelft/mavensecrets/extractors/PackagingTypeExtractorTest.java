package nl.tudelft.mavensecrets.extractors;

import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;

import org.apache.maven.model.Model;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import nl.tudelft.Database;
import nl.tudelft.Maven;
import nl.tudelft.Package;
import nl.tudelft.PackageId;
import nl.tudelft.mavensecrets.JarUtil;
import nl.tudelft.mavensecrets.resolver.DefaultResolver;

public class PackagingTypeExtractorTest {

    private static PackagingTypeExtractor extractor = null;
    private static Maven maven = null;
    private static File fileExecutable = null;
    private static File sourceFile = null;
    private static File javadocFile = null;
    private static String pkgName = "";
    private static Model model = null;
    private static PackageId packageId = null;
    private static PackageId id = null;
    static List<String> allArtifacts = null;
    private static Database db = mock(Database.class);

    @TempDir
    private static File dir;

    @BeforeAll
    public static void setup() throws IOException {
        File f = new File(dir, ".m2/test/mybat/yourbat/4.5");
        f.mkdirs();
        extractor = new PackagingTypeExtractor();
        maven = new Maven(new DefaultResolver(new File(dir, ".m2/test")));
        fileExecutable = new File(f, "yourbat-4.5.war");
        sourceFile = new File(f, "yourbat-4.5-sources.jar");
        javadocFile = new File(f, "yourbat-4.5-javadoc.jar");
        model = new Model();
        model.setPackaging("jar");
        packageId = new PackageId("mybat", "yourbat", "4.5");
        pkgName = "war";

        JarUtil.createJar(fileExecutable, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);
        JarUtil.createJar(sourceFile, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);
        JarUtil.createJar(javadocFile, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);

        id = new PackageId("de.mediathekview","MServer","3.1.60");
        allArtifacts = new ArrayList<>();
        allArtifacts.add("MServer-3.1.60.tar.gz");
        allArtifacts.add("MServer-3.1.60.jar");
        allArtifacts.add("MServer-3.1.60.jar.sha512");
        allArtifacts.add("MServer-3.1.60.tar.bz2");
        allArtifacts.add("MServer-3.1.60.tar.gz.sha1");
        allArtifacts.add("MServer-3.1.60.tar.bz2.sha256");
        allArtifacts.add("MServer-3.1.60.tar.bz2.sha1");
        allArtifacts.add("MServer-3.1.60-sources.jar.sha1");
        allArtifacts.add("MServer-3.1.60-change-log.txt.sha256");
        allArtifacts.add("MServer-3.1.60-changelog.tar.bz2.sha256");
    }


    @Test
    public void testCorrectNumberOfFields() throws IOException {
        try (Package pkg = createPackage(packageId, new JarFile(fileExecutable), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(extractor.fields().length, results.length);
        }
    }

    @Test
    public void testPackagingTypeFromPOM() throws IOException {
        try (Package pkg = createPackage(packageId, new JarFile(fileExecutable), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("jar", results[0]);
        }
    }

    @Test
    public void testPackagingTypeFromExecutable() throws IOException {
        try (Package pkg = createPackage(packageId, new JarFile(fileExecutable), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("war", results[1]);
        }
    }

    @Test
    public void testSourceQualifier() throws IOException {
        try (Package pkg = createPackage(packageId, new JarFile(sourceFile), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("sources", results[2]);
        }
    }

    @Test
    public void testAllQualifier() {
        Set<String> allQualifiers = new HashSet<>();
        extractor.extractQualifier(id, allArtifacts, allQualifiers);

        Set<String> result = new HashSet<>();
        result.add("sources");
        result.add("changelog");
        result.add("change-log");

        Assertions.assertEquals(result, allQualifiers);
    }

    @Test
    public void testAllCheckSumAndExecutable() {
        Set<String> allExecutable = new HashSet<>();
        Set<String> allChecksum = new HashSet<>();
        extractor.extractExecutableTypeAndCheckSum(id, allArtifacts, allExecutable, allChecksum);

        Set<String> resultExecutable = new HashSet<>();
        resultExecutable.add("tar.bz2");
        resultExecutable.add("tar.gz");
        resultExecutable.add("jar");

        Set<String> resultChecksum = new HashSet<>();
        resultChecksum.add(".sha1");
        resultChecksum.add(".sha256");
        resultChecksum.add(".sha512");

        Assertions.assertEquals(resultExecutable, allExecutable);
        Assertions.assertEquals(resultChecksum, allChecksum);
    }

    @Test
    public void testJavadocQualifier() throws IOException {
        try (Package pkg = createPackage(packageId, new JarFile(javadocFile), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("javadoc", results[3]);
        }
    }

    @Test
    public void testMd5() throws IOException {
        File f = createChecksumFile("md5");
        try (Package pkg = createPackage(packageId, new JarFile(sourceFile), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("2e315dcaa77983999bf11106c65229dc", results[4]);
        }
        f.delete();

    }

    @Test
    public void testSha1() throws IOException {
        File f= createChecksumFile("sha1");
        try (Package pkg = createPackage(packageId, new JarFile(sourceFile), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("8a7b91cee1b3fd5aafd5838a2867dfedcd92f227", results[5]);
        }
        f.delete();
    }

    @Test
    public void testSha256() throws IOException {
        File f = createChecksumFile("sha256");
        try (Package pkg = createPackage(packageId, new JarFile(sourceFile), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("408f31d86c6bf4a8aff4ea682ad002278f8cb39dc5f37b53d343e63a61f3cc4f", results[6]);
        }
        f.delete();
    }

    @Test
    public void testSha512() throws IOException {
        File f = createChecksumFile("sha512");
        try (Package pkg = createPackage(packageId, new JarFile(sourceFile), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("322c832910d962188db403fd8e0c5b026aba8e2daec603d191b00ef907896bf990bc605f7caa84d164450bf8f9797b5f32d38e5af96f7e0e577b89c15a9da689", results[7]);
        }
        f.delete();
    }

    @Test
    void testNoHash() throws IOException {
        try (Package pkg = createPackage(packageId, new JarFile(sourceFile), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNull(results[4]);
            Assertions.assertNull(results[5]);
            Assertions.assertNull(results[6]);
            Assertions.assertNull(results[7]);
        }
    }

    @Test
    void testAllHashes() throws IOException {
        File f1 = createChecksumFile("md5");
        File f2 = createChecksumFile("sha1");
        File f3 = createChecksumFile("sha256");
        File f4 = createChecksumFile("sha512");
        try (Package pkg = createPackage(packageId, new JarFile(sourceFile), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("2e315dcaa77983999bf11106c65229dc", results[4]);
            Assertions.assertEquals("8a7b91cee1b3fd5aafd5838a2867dfedcd92f227", results[5]);
            Assertions.assertEquals("408f31d86c6bf4a8aff4ea682ad002278f8cb39dc5f37b53d343e63a61f3cc4f", results[6]);
            Assertions.assertEquals("322c832910d962188db403fd8e0c5b026aba8e2daec603d191b00ef907896bf990bc605f7caa84d164450bf8f9797b5f32d38e5af96f7e0e577b89c15a9da689", results[7]);
        }
        f1.delete();
        f2.delete();
        f3.delete();
        f4.delete();

    }

    @Test
    public void testFileTypes() throws IOException {
        JarUtil.createJar(fileExecutable, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES.andThen(jos -> {
            jos.putNextEntry(new ZipEntry("my-bat.class"));
            jos.closeEntry();
            jos.putNextEntry(new ZipEntry("mybat.txt"));
            jos.closeEntry();
            jos.putNextEntry(new ZipEntry("Readme"));
            jos.closeEntry();
        }));
        JarFile file = new JarFile(fileExecutable);
        try (Package pkg = createPackage(packageId, file, model)) {
            Set<String> allFiles = extractor.getFilesFromExecutable(file);
            Assertions.assertNotNull(allFiles);
            Set<String> res = new HashSet<>(Arrays.asList("class", "txt", "mf", "file"));
            Assertions.assertEquals(allFiles, res);
        }
    }


    @AfterAll
    public static void teardown() {
        extractor = null;
        maven = null;
        fileExecutable = null;
        sourceFile = null;
        javadocFile = null;
        packageId = null;
    }

    private static Package createPackage(PackageId id ,JarFile jar, Model pom) {
        Objects.requireNonNull(jar);

        return new Package(id, jar, pom);
    }

    private static File createChecksumFile(String checksumType) {
        File path = new File(dir, "/.m2/test/mybat/yourbat/4.5/yourbat-4.5.war." + checksumType);
        Map<String, String> hash = Map.of(
                "md5",
                "2e315dcaa77983999bf11106c65229dc",
                "sha1",
                "8a7b91cee1b3fd5aafd5838a2867dfedcd92f227",
                "sha256",
                "408f31d86c6bf4a8aff4ea682ad002278f8cb39dc5f37b53d343e63a61f3cc4f",
                "sha512",
                "322c832910d962188db403fd8e0c5b026aba8e2daec603d191b00ef907896bf990bc605f7caa84d164450bf8f9797b5f32d38e5af96f7e0e577b89c15a9da689"
        );

        try (FileWriter fileWriter = new FileWriter(path)) {
            fileWriter.write(hash.get(checksumType));
            System.out.println("Text file created successfully.");
        } catch (IOException e) {
            System.out.println("An error occurred while creating the text file: " + e.getMessage());
        }
        return path;
    }

}
