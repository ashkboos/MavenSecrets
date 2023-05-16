package nl.tudelft;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import org.apache.maven.model.DeploymentRepository;
import org.apache.maven.model.DistributionManagement;
import org.apache.maven.model.Model;
import org.apache.maven.model.Scm;

public class ExtractorVC implements Extractor{

    Field[] fields;

    public ExtractorVC() {
        this.fields = new Field[] {
                new Field("scm_url", "VARCHAR(512)"),
                new Field("homepage_url", "VARCHAR(512)"),
                new Field("dist_mgmt_repo_url", "VARCHAR(512)")};
    }

    @Override
    public Field[] fields() {
        return fields;
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) {
        List<Object> values = new LinkedList<>();

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

        values.add(scmUrl);
        values.add(homeUrl);
        values.add(repositoryUrl);

        return values.toArray();
    }

}
