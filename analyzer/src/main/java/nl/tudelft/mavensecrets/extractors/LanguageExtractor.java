package nl.tudelft.mavensecrets.extractors;

import nl.tudelft.Extractor;
import nl.tudelft.Field;
import nl.tudelft.Maven;
import nl.tudelft.Package;
import org.apache.maven.model.Dependency;

import java.io.IOException;
import java.util.Objects;

public class LanguageExtractor implements Extractor {
    private final Field[] fields = new Field[]{
            new Field("scala_linked", "BOOLEAN"),
            new Field("kotlin_linked", "BOOLEAN"),
            new Field("clojure_linked", "BOOLEAN"),
            new Field("groovy_linked", "BOOLEAN"),
    };

    @Override
    public Field[] fields() {
        return fields;
    }

    @Override
    public Object[] extract(Maven mvn, Package pkg) throws IOException {
        return new Object[]{
                isScalaLinked(pkg),
                isKotlinLinked(pkg),
                isClojureLinked(pkg),
                isGroovyLinked(pkg),
        };
    }

    private boolean isScalaLinked(Package pkg) {
        return pkg.pom().getDependencies().stream().anyMatch(LanguageExtractor::isScalaLink);
    }

    private static boolean isScalaLink(Dependency dep) {
        return Objects.equals(dep.getGroupId(), "org.scala-lang") &&
                (Objects.equals(dep.getArtifactId(), "scala3-library_3")
                        || Objects.equals(dep.getArtifactId(), "scala-library"));
    }

    private boolean isKotlinLinked(Package pkg) {
        return pkg.pom().getDependencies().stream().anyMatch(LanguageExtractor::isKotlinLink);
    }

    private static boolean isKotlinLink(Dependency dep) {
        return Objects.equals(dep.getGroupId(), "org.jetbrains.kotlin")
                && Objects.equals(dep.getArtifactId(), "kotlin-stdlib");
    }

    private boolean isClojureLinked(Package pkg) {
        return pkg.pom().getDependencies().stream().anyMatch(LanguageExtractor::isClojureLink);
    }

    private static boolean isClojureLink(Dependency dep) {
        return Objects.equals(dep.getGroupId(), "org.clojure")
                && Objects.equals(dep.getArtifactId(), "clojure");
    }

    private boolean isGroovyLinked(Package pkg) {
        return pkg.pom().getDependencies().stream().anyMatch(LanguageExtractor::isGroovyLink);
    }

    private static boolean isGroovyLink(Dependency dep) {
        return Objects.equals(dep.getArtifactId(), "groovy") &&
                (Objects.equals(dep.getGroupId(), "org.apache.groovy")
                        || Objects.equals(dep.getGroupId(), "org.codehaus.groovy"));
    }
}
