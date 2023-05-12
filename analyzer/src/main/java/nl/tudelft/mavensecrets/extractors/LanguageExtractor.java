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
            new Field("has_tasty", "BOOLEAN"),
            new Field("has_kt", "BOOLEAN"),
            new Field("has_kotlin_module", "BOOLEAN"),
            new Field("has_clj", "BOOLEAN")
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
                hasTasty(pkg),
                hasKt(pkg),
                hasKotlinModule(pkg),
                hasClj(pkg)
        };
    }

    private static boolean isScalaLinked(Package pkg) {
        return pkg.pom().getDependencies().stream().anyMatch(LanguageExtractor::isScalaLink);
    }

    private static boolean isKotlinLinked(Package pkg) {
        return pkg.pom().getDependencies().stream().anyMatch(LanguageExtractor::isKotlinLink);
    }

    private static boolean isClojureLinked(Package pkg) {
        return pkg.pom().getDependencies().stream().anyMatch(LanguageExtractor::isClojureLink);
    }

    private static boolean isGroovyLinked(Package pkg) {
        return pkg.pom().getDependencies().stream().anyMatch(LanguageExtractor::isGroovyLink);
    }

    private static boolean hasTasty(Package pkg) {
        return pkg.jar().stream().anyMatch(i -> i.getRealName().toLowerCase().endsWith(".tasty"));
    }

    private static boolean hasKt(Package pkg) {
        return pkg.jar().stream().anyMatch(i -> i.getRealName().endsWith("Kt.class"));
    }

    private static boolean hasKotlinModule(Package pkg) {
        return pkg.jar().stream().anyMatch(i -> i.getRealName().toLowerCase().endsWith(".kotlin_module"));
    }

    private static boolean hasClj(Package pkg) {
        return pkg.jar().stream().anyMatch(i -> i.getRealName().toLowerCase().endsWith(".clj"));
    }

    private static boolean isScalaLink(Dependency dep) {
        return Objects.equals(dep.getGroupId(), "org.scala-lang") &&
                (Objects.equals(dep.getArtifactId(), "scala3-library_3")
                        || Objects.equals(dep.getArtifactId(), "scala-library"));
    }

    private static boolean isKotlinLink(Dependency dep) {
        return Objects.equals(dep.getGroupId(), "org.jetbrains.kotlin")
                && Objects.equals(dep.getArtifactId(), "kotlin-stdlib");
    }

    private static boolean isClojureLink(Dependency dep) {
        return Objects.equals(dep.getGroupId(), "org.clojure")
                && Objects.equals(dep.getArtifactId(), "clojure");
    }

    private static boolean isGroovyLink(Dependency dep) {
        return Objects.equals(dep.getArtifactId(), "groovy") &&
                (Objects.equals(dep.getGroupId(), "org.apache.groovy")
                        || Objects.equals(dep.getGroupId(), "org.codehaus.groovy"));
    }
}
