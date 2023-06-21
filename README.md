# MavenSecrets
MavenSecrets is a project designed to look at projects hosted on the Maven Central repository, and perform analysis on them.
It consists of the main application, the analyzer, which downloads and extracts data from Maven Cenral, and separate components processing this raw data.

## Analyzer
The application picking packages to analyze and extracting data, see [README](analyzer/README.md).

## PyScripts
Scripts to process the raw data from running the analyzer, and produce meaningful results, see [README](pyscripts/README.md).

## Java Build Aspects
A standalone application to process build aspect related data, see [README](visualization-build-aspects/README.md).

## Space Extractor
Due to dependency conflicts the space extractor has to be run a different way than the rest of the application see [README](analyzer/src/main/README.md)