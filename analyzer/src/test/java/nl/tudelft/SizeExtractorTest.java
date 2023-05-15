package nl.tudelft;
import java.io.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

import nl.tudelft.mavensecrets.extractors.SizeExtractor;
import nl.tudelft.mavensecrets.resolver.Resolver;
import org.apache.maven.model.Model;

import org.junit.jupiter.api.Test;
import org.mockito.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class SizeExtractorTest {

    @Test
    public void test() throws Exception {
        Database db = mock(Database.class);
        List<File> files = new ArrayList<>();
        String name = System.getProperty("user.home") +"/.m2/test/demo.jar";
        File pomFile = new File(System.getProperty("user.home"), "/.m2/test/pom.xml");
        File classFile = new File(System.getProperty("user.home"), "/.m2/test/hello.class");
        files.add(pomFile);
        files.add(classFile);
        JarFile j = createJarFileFromFiles(name, files);
        Resolver resolver = mock(Resolver.class);
        Maven mvn = new Maven(resolver);
        Model model = new Model();
        String groupId = "antlr";
        String artifactId = "antlr";
        String version = "2.7.7";
        PackageId id = new PackageId(groupId, artifactId, version);
        Package pkg = new Package(id, j, model);
        SizeExtractor sizeExtractor = mock(SizeExtractor.class);
        java.lang.reflect.Field checked = SizeExtractor.class.getDeclaredField("checked");
        checked.set(sizeExtractor, true);
        checked.setAccessible(true);
        ArgumentCaptor<Field[]> fields = ArgumentCaptor.forClass(Field[].class);
        ArgumentCaptor<Object[]> values = ArgumentCaptor.forClass(Object[].class);
        Mockito.doNothing().when(sizeExtractor).extensionDatabase(
                Mockito.any(Database.class),
                Mockito.anyBoolean(),
                Mockito.any(Field[].class),
                Mockito.any(Object[].class),
                Mockito.any(PackageId.class));
        when(sizeExtractor.extract(mvn, pkg, db)).thenCallRealMethod();
        //verify(sizeExtractor).extensionDatabase(Mockito.any(Database.class), Mockito.anyBoolean(), fields.capture(), values.capture(), Mockito.any(PackageId.class));
        Object[] obj = sizeExtractor.extract(mvn, pkg, db);
        //Field[] o = fields.getValue();
        assertEquals(files.size(), obj[1]);


    }


    @Test
    public void testMockSizeExtractor() throws Exception {
        Database db = mock(Database.class);
        Resolver resolver = mock(Resolver.class);
        Maven mvn = new Maven(resolver);
        Model model = new Model();
        String groupId = "antlr";
        String artifactId = "antlr";
        String version = "2.7.7";
        PackageId id = new PackageId(groupId, artifactId, version);
        File file = new File(System.getProperty("user.home"),".m2/repository/" + groupId + "/" + artifactId + "/" + version + "/" + artifactId + "-" + version + ".jar");
        JarFile jarFile = new JarFile(file);
        Package pkg = new Package(id, jarFile, model);
        SizeExtractor sizeExtractor = mock(SizeExtractor.class);
        java.lang.reflect.Field checked = SizeExtractor.class.getDeclaredField("checked");
        checked.set(sizeExtractor, true);
        checked.setAccessible(true);
        Mockito.doNothing().when(sizeExtractor).extensionDatabase(Mockito.any(Database.class),
                Mockito.anyBoolean(),
                Mockito.any(Field[].class),
                Mockito.any(Object[].class),
                Mockito.any(PackageId.class));
        when(sizeExtractor.extract(mvn, pkg, db)).thenCallRealMethod();
        Object[] obj = sizeExtractor.extract(mvn, pkg, db);
        assertEquals(jarFile.size() - countDirectories(jarFile), obj[1]);
        assertEquals((long) 881724, obj[0]);
    }

    public JarFile createJarFileFromFiles(String jarFileName, List<File> files) throws IOException {
        File jarFile = new File(jarFileName);
//        Manifest manifest = new Manifest();
//        manifest.getMainAttributes().put(Manifest.Attribute.MANIFEST_VERSION, "1.0");
        JarOutputStream jarOutputStream = new JarOutputStream(new FileOutputStream(jarFile));
        for (File file : files) {
            ZipEntry zipEntry = new ZipEntry(file.getName());
            jarOutputStream.putNextEntry(zipEntry);
            // Write the contents of the file to the JarOutputStream
            // You can use a FileInputStream to read the contents of the file and write them to the JarOutputStream
            jarOutputStream.closeEntry();
        }
        jarOutputStream.close();
        return new JarFile(jarFile);
    }

    public static int countDirectories(JarFile jar) {
        int count = 0;
        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            JarEntry entry = entries.nextElement();
            if (entry.isDirectory()) {
                count++;
            }
        }
        return count;
    }
}
