package nl.tudelft.mavensecrets.extractors;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import nl.tudelft.*;
import nl.tudelft.Package;
import nl.tudelft.mavensecrets.JarUtil;
import nl.tudelft.mavensecrets.resolver.DefaultResolver;
import org.apache.maven.model.Model;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class PackagingTypeExtractorTest {

    private static PackagingTypeExtractor extractor = null;
    private static Maven maven = null;
    private static File fileExecutable = null;
    private static File sourceFile = null;
    private static File javadocFile = null;
    private static File md5File = null;
    private static File sha1File = null;
    private static String pkgName = "";
    private static Model model = null;
    private static PackageId packageId = null;


    private static File dir;

    @BeforeAll
    public static void setup() {
        extractor = new PackagingTypeExtractor();
        maven = new Maven(new DefaultResolver(".m2/test"));
        String child = ".m2/test/mybat/yourbat/4.5";
        dir = new File(System.getProperty("user.home"), child);
        fileExecutable = new File(dir, "yourbat-4.5.war");
        sourceFile = new File(dir, "yourbat-4.5-sources.jar");
        javadocFile = new File(dir, "yourbat-4.5-javadoc.jar");
        md5File = new File(dir, "yourbat-4.5.war.md5");
        sha1File = new File(dir, "yourbat-4.5.war.sha1");
        model = new Model();
        model.setPackaging("jar");
        packageId = new PackageId("mybat", "yourbat", "4.5");
        pkgName = "war";
    }


    @Test
    public void testCorrectNumberOfFields() throws IOException {
        JarUtil.createJar(fileExecutable, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(packageId, new JarFile(fileExecutable), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(extractor.fields().length, results.length);
        }
    }

    @Test
    public void testPackagingTypeFromPOM() throws IOException {
        JarUtil.createJar(fileExecutable, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(packageId, new JarFile(fileExecutable), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("jar", results[0]);
        }
    }

    @Test
    public void testPackagingTypeFromExecutable() throws IOException {
        JarUtil.createJar(fileExecutable, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(packageId, new JarFile(fileExecutable), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("war", results[1]);
        }
    }

    @Test
    public void testSourceQualifier() throws IOException {
        JarUtil.createJar(sourceFile, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(packageId, new JarFile(sourceFile), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("sources", results[2]);
        }
    }

    @Test
    public void testJavadocQualifier() throws IOException {
        JarUtil.createJar(javadocFile, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(packageId, new JarFile(javadocFile), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("javadoc", results[3]);
        }
    }

    @Test
    public void testMd5() throws IOException {
        JarUtil.createJar(md5File, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(packageId, new JarFile(md5File), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(".MD5", results[4]);
        }
    }

    @Test
    public void testSha1() throws IOException {
        JarUtil.createJar(sha1File, JarUtil.DEFAULT_MANIFEST, JarUtil.DEFAULT_RESOURCES);
        try (Package pkg = createPackage(packageId, new JarFile(sha1File), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(".SHA1", results[5]);
        }
    }

    @Test
    public void testSha256() throws IOException {
        try (Package pkg = createPackage(packageId, new JarFile(sha1File), model)) {
            Object[] results = extractor.extract(maven, pkg, pkgName);
            Assertions.assertNotNull(results);
            Assertions.assertEquals("null", results[6]);
        }
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
        sha1File = null;
        md5File = null;
        packageId = null;
    }

    private static Package createPackage(PackageId id ,JarFile jar, Model pom) {
        Objects.requireNonNull(jar);

        return new Package(id, jar, pom);
    }

}
