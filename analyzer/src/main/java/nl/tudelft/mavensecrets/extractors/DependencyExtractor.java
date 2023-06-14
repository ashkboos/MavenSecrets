package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.mavensecrets.Database;
import nl.tudelft.mavensecrets.Field;
import nl.tudelft.mavensecrets.Maven;
import nl.tudelft.mavensecrets.Package;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.eclipse.aether.repository.RemoteRepository;
import org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;

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
        int directDependencies = 0;
        List<org.apache.maven.model.Dependency> dependencies = m.getDependencies();
        for(Dependency d : dependencies){
            if(d.getScope() == null || d.getScope().equals("compile")) {
                directDependencies++;
            }
        }
        String id = pkg.id().group() + ":" + pkg.id().artifact() + ":" + pkgType + ":" + pkg.id().version();
        int trans = resolve(id);
//        int trans = resolves(pkg.id().group(), pkg.id().artifact(), pkg.id().version());
        int a = trans - 1;
        if(a == -1) a = 0;
        if(a == -2) a = -1;
        result[0] = directDependencies;
        result[1] = a;
        return result;
    }


//    private static int timeoutResolve(String row) {
//        ExecutorService executor = Executors.newSingleThreadExecutor();
//
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(() -> {
//            // Perform your method call here
//            return resolve(row);
//        }, executor);
//
//        try {
//            int result = future.get(600, TimeUnit.SECONDS); // Timeout set to 600 seconds
//            return result;
//        } catch (TimeoutException | InterruptedException | ExecutionException e) {
//            return -1;
//        } finally {
//            executor.shutdown(); // Shutdown the executor
//        }
//    }


    private static int resolve(
            final String row) {
        List<MavenCoordinate> result =
                new ArrayList<>();
        try {
            result = org.jboss.shrinkwrap.resolver.api.maven.Maven.resolver().resolve(row).withTransitivity()
                    .asList(org.jboss.shrinkwrap.resolver.api.maven.coordinate.MavenCoordinate.class);
        } catch (Exception e) {
            System.out.println("Exception occurred while resolving " + row + ", " + e);
            System.out.println(e);
            return result.size();
        }
        return result.size();
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
