package nl.tudelft;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;

public class ExtractorVC implements Extractor{

    private static final Logger LOGGER = LogManager.getLogger(ExtractorVC.class);
    Field[] fields;

    public ExtractorVC() {
        this.fields = new Field[] {
                new Field("scm_url", "VARCHAR"),
                new Field("homepage_url", "VARCHAR"),
                new Field("dist_mgmt_repo_url", "VARCHAR"),
                new Field("checksum_md5", "VARCHAR"),
                new Field("checksum_sha1", "VARCHAR"),
                new Field("checksum_sha256", "VARCHAR"),
                new Field("checksum_sha512", "VARCHAR")
                };
    }

    @Override
    public Field[] fields() {
        return fields;
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg) {
        List<Object> values = new LinkedList<>();

        values.addAll(extractUrls(mvn, pkg));
        values.addAll(extractChecksums(mvn, pkg));

        return values.toArray();
    }

    private List<Object> extractUrls(Maven mvn, Package pkg) {
        String scmUrl = Optional.ofNullable(pkg.pom())
                .map(Model::getScm)
                .map(Scm::getUrl)
                .orElse(null);

        String homeUrl = Optional.ofNullable(pkg.pom())
                .map(Model::getUrl)
                .orElse(null);

        String repositoryUrl = Optional.ofNullable(pkg.pom())
                .map(Model::getDistributionManagement)
                .map(DistributionManagement::getRepository)
                .map(DeploymentRepository::getUrl)
                .orElse(null);
        List<Object> urls = new LinkedList<>();

        urls.add(scmUrl);
        urls.add(homeUrl);
        urls.add(repositoryUrl);
        return urls;
    }

    private List<Object> extractChecksums(Maven mvn, Package pkg) {
        // TODO for multiple packaging types (JAR, WAR, EAR)
        // TODO convert try-catches to more readable format
        // TODO better exception handling
        // MD5
        JarFile jar = pkg.jar();
        String jarName = jar.getName();
        LOGGER.debug("Jar name = " + jarName);
        String md5 = null, sha1 = null, sha256 = null, sha512 = null;
        try {
            md5 = readChecksum(jarName + ".md5");
        } catch (IOException e) {}
        try {
            sha1 = readChecksum(jarName + ".sha1");
        } catch (IOException e) {}
        try {
            sha256 = readChecksum(jarName + ".sha256");
        } catch (IOException e) {}
        try {
            sha512 = readChecksum(jarName + ".sha512");
        } catch (IOException e) {}
        List<Object> checksums = new LinkedList<>();
        checksums.add(md5);
        checksums.add(sha1);
        checksums.add(sha256);
        checksums.add(sha512);

        return checksums;

    }

    private String readChecksum(String filename) throws IOException {
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            return reader.readLine().split("\\s+")[0];
        }
    }

}
