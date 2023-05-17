package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.*;
import nl.tudelft.Package;
import nl.tudelft.mavensecrets.NopResolver;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.junit.jupiter.api.*;

import java.io.IOException;
import java.sql.SQLException;

import static org.mockito.Mockito.mock;

class ExtractorVCTest {
    private static Extractor extractor;
    private static Maven maven;
    private static Model testModel;
    private static String pkgName = "";
    private static Database db = mock(Database.class);

    @BeforeAll
    static void setUp() {
        extractor = new ExtractorVC();
        maven = new Maven(NopResolver.getInstance());

        Scm scm = new Scm();
        scm.setUrl("https://github.com/ashkboos/MavenSecrets");
        testModel = new Model();
        testModel.setScm(scm);
        testModel.setUrl("https://se.ewi.tudelft.nl/");

        DistributionManagement dist = new DistributionManagement();
        DeploymentRepository repo = new DeploymentRepository();
        repo.setUrl("https://github.com/example/example");
        dist.setRepository(repo);
        testModel.setDistributionManagement(dist);
    }

    @AfterAll
    static void tearDown() {
        extractor = null;
        maven = null;
    }

    @Test
    public void testNumFields () throws IOException, SQLException {
        try (Package pkg = createPackage(testModel)) {
            Object[] results = extractor.extract(maven, pkg, pkgName, db);
            Assertions.assertNotNull(results);
            Assertions.assertEquals(extractor.fields().length, results.length);
        }
    }

    @Test
    void testExtractAll() throws SQLException, IOException {
        Package pkg = createPackage(testModel);
        Object[] results = extractor.extract(maven, pkg, pkgName, db);
        Assertions.assertNotNull(results);
        Assertions.assertArrayEquals(new Object[] {
                "https://github.com/ashkboos/MavenSecrets",
                "https://se.ewi.tudelft.nl/",
                "https://github.com/example/example"
        }, results);
    }

    @Test
    void testAllNull() throws SQLException, IOException {
        Model emptyModel = new Model();
        Package pkg = createPackage(emptyModel);
        Object[] results = extractor.extract(maven, pkg, pkgName, db);
        Assertions.assertArrayEquals(new Object[] {null, null, null}, results);
    }

    private static Package createPackage(Model model) {
        return new Package(new PackageId("org.test","test", "1.42"), null, model);
    }
}