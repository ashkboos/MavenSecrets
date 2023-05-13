package nl.tudelft.mavensecrets.extractors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

import java.util.Collections;
import java.util.jar.JarFile;
import nl.tudelft.Package;
import nl.tudelft.*;
import org.apache.maven.model.Model;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;

public class PackagingTypeExtractorTest {

    private PackagingTypeExtractor packagingTypeExtractor;

    @Before
    public void setup()  {

        MockitoAnnotations.initMocks(this);
        packagingTypeExtractor = new PackagingTypeExtractor();

    }

    @Test
    public void testFields() {
        Field[] expectedFields = new Field[] {
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

        Assertions.assertArrayEquals(expectedFields, packagingTypeExtractor.fields());
    }

    @Test
    public void testExtract1() throws ArtifactResolutionException, PackageException {
        // Create mock objects
        Maven mockedMvn = mock(Maven.class);
        Package mockedPkg = mock(Package.class);
        Model mockedModel = mock(Model.class);
        JarFile mockedJarFile = mock(JarFile.class);

        // Set up mock object behaviors
        when(mockedPkg.pom()).thenReturn(mockedModel);
        when(mockedPkg.jar()).thenReturn(mockedJarFile);
        when(mockedModel.getPackaging()).thenReturn("pom");
        when(mockedJarFile.entries()).thenReturn(Collections.emptyEnumeration());
        when(mockedMvn.getArtifactChecksum(ArgumentMatchers.any(PackageId.class), ArgumentMatchers.anyString()))
            .thenReturn(null);
        when(mockedMvn.getArtifactQualifier(ArgumentMatchers.any(PackageId.class), ArgumentMatchers.anyString(),
            ArgumentMatchers.anyString())).thenReturn(null);

        // Call extract method
        Object[] extractedFields = packagingTypeExtractor.extract(mockedMvn, mockedPkg, "jar");

        // Verify the extracted fields
        assertEquals("pom", extractedFields[0]);
        assertEquals("jar", extractedFields[1]);
        assertEquals("null", extractedFields[2]);
        assertEquals("null", extractedFields[3]);
        assertEquals("null", extractedFields[4]);
        assertEquals("null", extractedFields[5]);
        assertEquals("null", extractedFields[6]);
        assertEquals("null", extractedFields[7]);
        assertEquals("[]", extractedFields[8]);
    }
}

