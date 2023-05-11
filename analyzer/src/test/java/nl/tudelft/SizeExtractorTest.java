package nl.tudelft;
import java.io.File;
import java.sql.Connection;
import java.sql.ResultSet;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import nl.tudelft.Database;
import nl.tudelft.mavensecrets.extractors.SizeExtractor;
import nl.tudelft.mavensecrets.resolver.Resolver;
import org.apache.maven.model.Model;
import org.junit.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class SizeExtractorTest {
    @InjectMocks private Database db;
    @Mock private Connection mockConnection = mock(Connection.class);

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
    }
    @Test
    public void testMockSizeExtractor() throws Exception {
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
        when(sizeExtractor.extract(mvn, pkg)).thenCallRealMethod();
        Object[] obj = sizeExtractor.extract(mvn, pkg);
        assertEquals(jarFile.size() - countDirectories(jarFile), obj[1]);
        assertEquals((long) 881724, obj[0]);
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
