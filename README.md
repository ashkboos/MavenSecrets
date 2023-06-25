# MavenSecrets
MavenSecrets is a project designed to look at projects hosted on the Maven Central repository, and perform analysis on them.
It consists of the main application, the analyzer, which downloads and extracts data from Maven Cenral, and separate components processing this raw data.
This research was performed as part of the [Research Project](https://github.com/TU-Delft-CSE/Research-Project) of the [TU Delft](https://github.com/TU-Delft-CSE) CSE bachelor programme of 2022/2023.

## Analyzer
The application picking packages to analyze and extracting data, see [README](analyzer/README.md).

## PyScripts
Scripts to process the raw data from running the analyzer, and produce meaningful results, see [README](pyscripts/README.md).

## Java Build Aspects
A standalone application to process build aspect related data, see [README](visualization-build-aspects/README.md).