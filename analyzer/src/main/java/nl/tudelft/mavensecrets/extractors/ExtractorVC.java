package nl.tudelft.mavensecrets.extractors;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import nl.tudelft.*;
import nl.tudelft.Package;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;

public class ExtractorVC implements Extractor {

    private static final Logger LOGGER = LogManager.getLogger(ExtractorVC.class);
    Field[] fields;

    public ExtractorVC() {
        this.fields = new Field[] {
                new Field("scm_url", "VARCHAR"),
                new Field("homepage_url", "VARCHAR"),
                new Field("dist_mgmt_repo_url", "VARCHAR")
                };
    }

    @Override
    public Field[] fields() {
        return fields;
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) {
        List<Object> values = new LinkedList<>();

        values.addAll(extractUrls(mvn, pkg));

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

}
