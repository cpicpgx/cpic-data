# CPIC Data

This repo contains all the DDL and code for defining the [CPIC](https://cpicpgx.org) data model and populating it with data.

## WARNING: NOT READY FOR USE :exclamation:  :exclamation:  :exclamation:

__:warning: This repo is under active development and, thus, not ready for any useful purpose. When this message disappears and a production release is on the Releases tab then it will be ready for review. :warning:__

__Seriously, you don't want to use this yet.__


## Setup

This project assumes you're running a Postgres 9+ database for loading/querying data.

Configuration is handled through environment variables. Here's what needs to be set:

- _CPIC_DB_ = the hostname for the db server (default `localhost`)
- _CPIC_USER_ = the postgresql role name to connect as (default `cpic`)
- _CPIC_PASS_ = the password for the postgresql role (default blank)

For local development you won't need to specify these. Set them if you want to connect to a different DB (e.g. push to prod). 

## Running

Some steps below will require a compiled version (jar) of this project. Use gradle to build the jar file:

```sh
gradle jar
```

This will place a compiled jar in the `build/libs` directory.


### Bootstrapping the DB

This project uses Flyway to set up the DB. Schema definition files are found in the `src/resources/db/migration` directory. Run the following to build the db:

```sh
java -cp build/libs/**CURRENT_JAR**.jar org.cpicpgx.db.FlywayMigrate
```

### Bootstrapping Information

There are multiple gene-specific data files, each with their own importer class. The entry points to load gene-specific data are in the `org.cpicpgx.importer` package. Check the javadocs on the individual importer classes for command-line parameters.

To load all data at once, use the `DataImport` class. This takes a `-d` parameter that is a directory with the following sub-folders containing excel files:

- allele_definition_tables
- allele_functionality_reference
- diplotype_phenotype_tables
- frequency_table
- gene_resource_mappings
- recommendation_tables
- test_alerts

Then put that jar on the classpath and run `org.cpicpgx.DataImport` class:

```sh
java -cp build/libs/**CURRENT_JAR**.jar org.cpicpgx.DataImport -d **PATH_TO_DATA_DIRECTORY**
```


### Exporting Data Artifacts

To export file artifacts of compiled data in the database use the `DataArtifactArchive` class. It expects a command line argument of a directory to write to. By default, it will write to a subdirectory with a datestamped name. Inside that folder will be subfolders for the different types of exported data.

```sh
java -cp build/libs/**CURRENT_JAR**.jar org.cpicpgx.DataArtifactArchive -d **PATH_TO_EXISTING_DIRECTORY**
```


### Exporting Web Data

To export JSON files that cache data for the website, just the following node script:

```sh
node src/main/node/writeData.js **PATH_TO_EXISTING_DIRECTORY**
```

By default it will use the production API. If you want to use a local development API set the `API` envvar to `dev`.
