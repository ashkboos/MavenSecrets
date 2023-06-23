# Version Control

These scripts use the intermediate data to answer the following RQs:
1. How reliable are the repository links?
1. Where are the repositories hosted and how does this change over time in the ecosystem?
1. Can the commit pertaining to a specific release be pinpointed?
1. How reproducible are the packages? Can one rebuild the packages with the same checksum?

## Database
The scripts create 4 new tables in the Postgres DB, namely: hosts, tags, builds, and errors.
- hosts - contains every package with a parseable host and eventually the valid urls
- tags - contains the tags/releases found
- builds - contains build params and results of each build
- errors - contains errors encountered when verifying or checking tags

## How to run
Make sure the Database is running and you have imported the data from the .sql file.
I recommend running the python scripts in a Conda environment. Install all dependencies using pip and/or conda.
Create a config.yaml file by copying default_config.yaml. You can choose which scripts to run there. You will also need to provide a Github API key for fetching the tag/release information. Then to run it, type the following commands:
```
cd pyscripts/vc
python main.py
```

## Dependencies
See requirements.txt for Python dependencies. These can be installed using pip install -r requirements.txt . If psycopg2 fails, try installing psycopg2-binary or install it with conda. You also need to install the dos2unix utility with your package manager or conda for the build scripts to convert newlines.