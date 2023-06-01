package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.sql.SQLException;
import java.util.*;

import nl.tudelft.mavensecrets.resolver.DefaultResolver;
import org.apache.maven.model.Model;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;
//import org.jboss.shrinkwrap.resolver.api.maven;

import nl.tudelft.Database;
import nl.tudelft.Extractor;
import nl.tudelft.Field;
import nl.tudelft.Maven;
import nl.tudelft.Package;

public class DependencyExtractor implements Extractor {

    //private static final Logger LOGGER = LogManager.getLogger(DependencyExtractor.class);
    private static final RemoteRepository MAVEN_CENTRAL = new RemoteRepository.Builder("central", "default", "https://repo.maven.apache.org/maven2/").build();
    private static List<RemoteRepository> repositories = List.of(MAVEN_CENTRAL);
    private final Field[] fields = {
            new Field("directdependencies", "INTEGER"),
            new Field("transitivedependencies", "INTEGER")
    };

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) throws IOException, SQLException {
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);
        Object[] result = new Object[2];
        Model m = pkg.pom();
        List<org.apache.maven.model.Dependency> dependencies = m.getDependencies();
        int directDependencies = dependencies.size();
        String id = pkg.id().group() + ":" + pkg.id().artifact() + ":" + pkgType + ":" + pkg.id().version();
        List<MavenCoordinate> files = resolve(id);
//        int trans = resolves(pkg.id().group(), pkg.id().artifact(), pkg.id().version());
        int a = files.size() - 1;
        if(a < 0) a = 0;
        result[0] = directDependencies;
        result[1] = a;
        return result;
    }

    private static List<org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate> resolve(
            final String row) {
        List<MavenCoordinate> result =
                new ArrayList<>();
        try {
            result = org.jboss.shrinkwrap.resolver.api.maven.Maven.resolver().resolve(row).withTransitivity()
                    .asList(org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate.class);
        } catch (Exception e) {
            System.out.println("Exception occurred while resolving " + row + ", " + e);
            System.out.println(e);
            return result;
        }
        return result;
    }

//    private int resolves(String groupId, String artifactId, String version) {
//        Artifact artifact = resolver.createArtifact(groupId, artifactId, version);
//        RepositorySystem repository = resolver.getRepository();
//        RepositorySystemSession session = resolver.getRepositorySystemSession();
//        DependencyResult result;
//        List<Dependency> list = new ArrayList<>();
//        list.add(new Dependency(artifact, null));
//        try {
//            result = repository.resolveDependencies(session, new DependencyRequest(new CollectRequest((Dependency) null, list, repositories), null));
//        } catch (DependencyResolutionException exception) {
//            // Handle exception
//            return -1;
//        }
//        List<Artifact> transDeps = new ArrayList<>();
//        for (ArtifactResult ar : result.getArtifactResults()) {
//            Artifact a = ar.getArtifact();
//            transDeps.add(a);
//        }
//        return transDeps.size();
//    }
}
