## Why does it not work with the rest of the application?
It does not work in this branch because the [Dependency Extractor](java/nl/tudelft/mavensecrets/extractors/DependencyExtractor.java) 
uses a library called ShrinkWrap to resolve the number of transitive dependencies, which conflicts with some 
dependencies (org.apache.maven:maven-resolver-provider:3.9.1 and org.apache.maven.resolver:maven-resolver-connector-basic:1.9.8) 
used in the rest of the project. 

## How to run the dependency extractor
The size and dependency extractors can be run in this branch which extracts all the desired metadata except for the
number of transitive dependencies where it will insert -1 for every package. To get the number of transitive dependencies,
checkout to the branch 'dev-niels', and run just the dependency extractor there. Keep in mind other extractors may not work
as desired in that branch! 

Our recommendation is to first run all the extractors and run the dependency extractor last,
because the [Default Resolver](java/nl/tudelft/mavensecrets/resolver/DefaultResolver.java) no longer works in that branch.
This means that the POM and JAR (or other packaging type) need to be in the local .m2 folder before running the dependency extractor in 
this branch. One way to achieve this is to run the other extractors first on the same packages as you plan to run the dependency extractor on.
This will download all the necessary files to the local .m2 folder. 