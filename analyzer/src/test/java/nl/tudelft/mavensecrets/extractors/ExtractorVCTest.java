package nl.tudelft.mavensecrets.extractors;

import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Properties;

import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import nl.tudelft.mavensecrets.Database;
import nl.tudelft.mavensecrets.Maven;
import nl.tudelft.mavensecrets.Package;
import nl.tudelft.mavensecrets.PackageId;
import nl.tudelft.mavensecrets.testutils.NopResolver;

class ExtractorVCTest {
    private static Extractor extractor;
    private static Maven maven;
    private static Model testModel;
    private static String pkgName = "";
    private static Database db = mock(Database.class);
    private static final String defaultUrl = "https://github.com/ashkboos/MavenSecrets";
    private static final String defaultHomepageUrl = "https://se.ewi.tudelft.nl/";
    private static final String defaultDistMgmtUrl = "https://github.com/example/example";
    private static final String defaultConnUrl = "scm:git:git@github.com:ericmoshare/uid-generator.git";
    private static final String defaultDevConnUrl = "scm:git:github.com/guardian/identity";

    @BeforeAll
    static void setUp() {
        extractor = new ExtractorVC();
        maven = new Maven(NopResolver.getInstance());

        Scm scm = new Scm();
        scm.setUrl(defaultUrl);
        scm.setDeveloperConnection(defaultDevConnUrl);
        scm.setConnection(defaultConnUrl);
        testModel = new Model();
        testModel.setScm(scm);
        testModel.setUrl(defaultHomepageUrl);

        DistributionManagement dist = new DistributionManagement();
        DeploymentRepository repo = new DeploymentRepository();
        repo.setUrl(defaultDistMgmtUrl);
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
    void testExtractAllUrls() throws SQLException, IOException {
        Package pkg = createPackage(testModel);
        Object[] results = extractor.extract(maven, pkg, pkgName, db);
        Assertions.assertNotNull(results);
        Assertions.assertArrayEquals(new Object[] {
                defaultUrl,
                defaultHomepageUrl,
                defaultDistMgmtUrl,
                defaultConnUrl,
                defaultDevConnUrl,
                null
        }, results);
    }

    @Test
    void testAllNull() throws SQLException, IOException {
        Model emptyModel = new Model();
        Package pkg = createPackage(emptyModel);
        Object[] results = extractor.extract(maven, pkg, pkgName, db);
        Assertions.assertArrayEquals(new Object[] {null, null, null, null, null, null}, results);
    }

    @Test
    void testReproducibilityProperty() throws SQLException, IOException {
        Model emptyModel = new Model();
        Properties props = new Properties();
        props.put("project.build.outputTimestamp","2023-04-22T15:25:31Z");
        emptyModel.setProperties(props);
        Package pkg = createPackage(emptyModel);
        Object[] results = extractor.extract(maven, pkg, pkgName, db);
        Assertions.assertArrayEquals(new Object[] {null, null, null, null, null, "2023-04-22T15:25:31Z"}, results);
    }


    private static Package createPackage(Model model) {
        return new Package(new PackageId("org.test","test", "1.42"), null, model);
    }
}