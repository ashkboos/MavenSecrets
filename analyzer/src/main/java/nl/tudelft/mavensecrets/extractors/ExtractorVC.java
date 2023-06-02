package nl.tudelft.mavensecrets.extractors;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import nl.tudelft.mavensecrets.Database;
import nl.tudelft.mavensecrets.Field;
import nl.tudelft.mavensecrets.Maven;
import nl.tudelft.mavensecrets.Package;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;

public class ExtractorVC implements Extractor {

    //private static final Logger LOGGER = LogManager.getLogger(ExtractorVC.class);
    Field[] fields;

    public ExtractorVC() {
        this.fields = new Field[] {
                new Field("scm_url", "VARCHAR"),
                new Field("homepage_url", "VARCHAR"),
                new Field("dist_mgmt_repo_url", "VARCHAR"),
                new Field("scm_conn_url", "VARCHAR"),
                new Field("dev_conn_url", "VARCHAR"),
                new Field("output_timestamp_prop", "VARCHAR") // used to ensure build reproducibility
                };
    }

    @Override
    public Field[] fields() {
        return fields;
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) {
        List<Object> values = new ArrayList<>();

        values.addAll(extractUrls(mvn, pkg));
        values.addAll(extractOther(mvn, pkg));

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

        String scmConnUrl = Optional.ofNullable(pkg.pom())
                .map(Model::getScm)
                .map(Scm::getConnection)
                .orElse(null);

        String scmDevUrl = Optional.ofNullable(pkg.pom())
                .map(Model::getScm)
                .map(Scm::getDeveloperConnection)
                .orElse(null);

        List<Object> urls = new ArrayList<>();

        urls.add(scmUrl);
        urls.add(homeUrl);
        urls.add(repositoryUrl);
        urls.add(scmConnUrl);
        urls.add(scmDevUrl);
        return urls;
    }

    private List<Object> extractOther(Maven mvn, Package pkg) {
        List<Object> values = new ArrayList<>();

        String outputTimestampProp = Optional.ofNullable(pkg.pom())
                .map(Model::getProperties)
                .map(x -> x.getProperty("project.build.outputTimestamp"))
                .orElse(null);

        values.add(outputTimestampProp);

        return values;


    }



}
