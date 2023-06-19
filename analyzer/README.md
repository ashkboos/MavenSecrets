# Analyzer
A data analyzer for the Maven Central repository.

## Usage
To build the JAR: `mvn package`

To run the JAR: `java -jar <jar_name>` (requires Java 17 or above)

## Configuration
When running the analyzer for the first time, a default configuration is created (`config.yml`).

### Extractors
To run a specific extractor, add the class to the `extractors` list.
The application expects extractor classses to implement `nl.tudelft.mavensecrets.extractors.Extractor` and have a public no-args constructor.

### Threads
The thread pool size for the runner.

### Database
The application expects a `PostgreSQL` database.
Username and password can be omitted if the database does not require authentication.

### Index File
The list of index files to read.
Index files are located in `./index-files/`, and if absent will be downloaded from [here](https://repo.maven.apache.org/maven2/.index/).

### `.m2` Directory
The location of the local repository.
If omitted, `$HOME/.m2/repository/` is used (the default local Maven repository location).

### Sample
The `seed` variable is the number used as seed for the data selection to ensure reproducibility.
The `sample-percentage` variable is the sample size used.

## Extending Functionality
To add a custom extractor, create an implementation for the Extractor class and add it to the configuration.
To add a custom data selection strategy, create an implementation for the `PackageSelector` class and change the selector used in `App.java` (currently unconfigurable).
Different repositories can be configured in the `DefaultResolver`, and additional configuration entries can be added in the `config` package.