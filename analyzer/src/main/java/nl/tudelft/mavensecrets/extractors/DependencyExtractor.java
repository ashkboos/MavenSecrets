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
        Queue<Tuple<Model, Integer>> toVisit = new ArrayDeque<>();
        toVisit.add(new Tuple(m, 0));
        Set<PackageId> packagesVisited = new HashSet<>();
        int levels = 3;
        int nullValue = 0;
        int notResolved = 0;
        getTransitiveDeps(mvn, toVisit, packagesVisited, nullValue, notResolved, levels);
        System.out.println("HELOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO");
        result[0] = directDependencies;
        result[1] = packagesVisited.size();
        return result;
        //Set<Revision> directDeps = getDependencies(pomFile, true);
    }

    public void getTransitiveDeps(Maven mvn, Queue<Tuple<Model, Integer>> toVisit, Set<PackageId> packagesVisited, int nullValue, int notResolved, int maxDepth) {
        if(toVisit.isEmpty()) {
            return;
        }
        Tuple<Model, Integer> current = toVisit.poll();
        if(current.depth == maxDepth) {
            return;
        }
        Model currentModel = current.x;

        for (Dependency d : currentModel.getDependencies()) {
            if (d.getVersion() == null || d.getGroupId() == null || d.getArtifactId() == null) {
                nullValue++;
                continue;
            }
            PackageId id = new PackageId(d.getGroupId(), d.getArtifactId(), d.getVersion());
            try {
                Model p = mvn.getPom(id);
                if (!packagesVisited.contains(id)) {
                    packagesVisited.add(id);
                    toVisit.add(new Tuple<>(p, current.depth + 1));
                }
            } catch (PackageException e) {
                packagesVisited.add(id);
                notResolved++;
                LOGGER.error(e);
            }
        }
    getTransitiveDeps(mvn, toVisit, packagesVisited, nullValue, notResolved, maxDepth);
    }

    class Tuple<Model, Integer> {
        Model x;
        int depth;
        public Tuple(Model x, int y) {
            this.x = x;
            this.depth = y;
        }

    }
}
