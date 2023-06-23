# Java Build Aspects
A data dumper for some Java build aspects.
Important numbers are logged, tables and various ids are dumped in `csv` files.

## Prerequisites
Run the analyzer with at least the following extractors:
- ArtifactExistsExctractor
- CompilerConfigExtractor
- JavaModuleExtractor
- JavaVersionExtractor

## Usage
To build the JAR: `mvn package`

To run the JAR: `java -jar <jar_name> <hostname> <port> <database> [<username> <password>]` (requires Java 17 or above)
