package nl.tudelft.mavensecrets.extractors;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import nl.tudelft.Package;
import nl.tudelft.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;

public class DependencyExtractor implements Extractor {
    private static final Logger LOGGER = LogManager.getLogger(Runner.class);;
    private final Field[] fields = {
            new Field("directdependencies", "INTEGER"),
            new Field("transitivedependencies", "INTEGER")
//            new Field("idisnull", "INTEGER"),
//            new Field("transitivenotresolved", "INTEGER")
    };

    @Override
    public Field[] fields() {
        return fields.clone();
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg, String pkgType, Database db) throws IOException {
        Objects.requireNonNull(mvn);
        Objects.requireNonNull(pkg);
        Object[] result = new Object[2];
        //File pomFile = pkg.pom().getPomFile();
        Model m = pkg.pom();
        List<Dependency> dependencies = m.getDependencies();
        int directDependencies = dependencies.size();
//        Queue<Model> toVisit = new ArrayDeque<>();
//        toVisit.add(m);
//        Set<PackageId> packagesVisited = new HashSet<>();
//        int nullValue = 0;
//        int notResolved = 0;
//        while (!toVisit.isEmpty()) {
//            Model currentModel = toVisit.poll();
//
//            for (Dependency d : currentModel.getDependencies()) {
//                if (d.getVersion() == null || d.getGroupId() == null || d.getArtifactId() == null) {
//                    nullValue++;
//                    continue;
//                }
//                PackageId id = new PackageId(d.getGroupId(), d.getArtifactId(), d.getVersion());
//                try {
//                    Model p = mvn.getPom(id);
//                    if (!packagesVisited.contains(id)) {
//                        packagesVisited.add(id);
//                        toVisit.add(p);
//                    }
//                } catch (PackageException e) {
//                    packagesVisited.add(id);
//                    notResolved++;
//                    LOGGER.error(e);
//                }
//            }
//        }
//        System.out.println("HELOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
        result[0] = directDependencies;
        result[1] = -1;
        return result;
        //Set<Revision> directDeps = getDependencies(pomFile, true);
    }
}
