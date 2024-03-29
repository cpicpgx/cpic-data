# CPIC Data

[![Build project](https://github.com/cpicpgx/cpic-data/actions/workflows/build.yml/badge.svg?branch=development)](https://github.com/cpicpgx/cpic-data/actions/workflows/build.yml)

This repo contains all the code used to create and maintain the [CPIC](https://cpicpgx.org) data model. This powers the API and the database.

If you're looking to use the REST API or get a copy of the database, go [read the documentation](https://github.com/cpicpgx/cpic-data/wiki).

## Important Links for Everyone

### Read the Docs

If you want more information about using the API or database, [read the documenatation](https://github.com/cpicpgx/cpic-data/wiki).

### Bugs/Discussion

If you found a bug or need to discuss something, [submit an issue](https://github.com/cpicpgx/cpic-data/issues). (Requires [GitHub account](https://github.com/join))

### Get Data/Code

If you want to get a copy of the raw data or code, [check the releases](https://github.com/cpicpgx/cpic-data/releases).


## Database Setup

You probably don't need to read the rest of this.

This section (and the next) are only applicable if you want to build the database from scratch. If you're importing a 
pre-built database export or using the API you don't need to do any of this. However, if you're interested in seeing an 
example of how to work with the database in Java code, follow along.

This project assumes you're running a Postgres 11+ database for loading/querying data.

Configuration happens with environment variables. Here's what needs to be set:

- _CPIC_HOST_ = the hostname for the db server (default `localhost`)
- _CPIC_USER_ = the postgresql role name to connect as (default `cpic`)
- _CPIC_PASS_ = the password for the postgresql role (default blank)
- _CPIC_DB_ = the postgresql database to connect to (default `cpic`, but could also be `cpic_staging`)

For local development you won't need to specify these. Set them if you're running in a different environment like the
production or staging servers.

## Running

Some steps below will require a compiled version (jar) of this project. Use gradle to build the jar file:

```shell script
./gradlew jar
```

or if you're on windows

```shell script
gradlew.bat jar
```

This will place a compiled "fat" jar (includes all dependencies) in the `build/libs` directory.


### Bootstrapping the DB

If you have an export of the database you do not need to do this. The export has all structure and data already. This 
section is for creating a bootstrap, mostly-empty version of the database.

This project uses Flyway to set up the DB. Schema definition files are in the `src/resources/db/migration` directory.
Run the following to build the db:

```sh
java -cp build/libs/CpicData.jar org.cpicpgx.db.FlywayMigrate
```

### Bootstrapping Information

There are multiple entity-specific data files, each with their own importer class. The entry points to load gene-specific data are in the `org.cpicpgx.importer` package. Check the javadocs on the individual importer classes for command-line parameters.

To load all data at once, use the `DataImport` class. This takes a `-d` parameter that is a directory with the following sub-folders containing excel files:

- allele_definition_tables
- allele_functionality_reference
- diplotype_phenotype_tables
- frequency_table
- gene_CDS
- gene_resource_mappings
- recommendation_tables
- test_alerts

Then put that jar on the classpath and run `org.cpicpgx.DataImport` class:

```sh
java -cp build/libs/CpicData.jar org.cpicpgx.DataImport -d <PATH_TO_DATA_DIRECTORY>
```


### Exporting Data Artifacts

To export file artifacts of compiled data in the database use the `DataArtifactArchive` class. It expects a command line argument of a directory to write to. By default, it will write to a subdirectory with a datestamped name. Inside that folder will be subfolders for the different types of exported data.

```sh
java -cp build/libs/CpicData.jar org.cpicpgx.DataArtifactArchive -d <PATH_TO_EXISTING_DIRECTORY>
```


### Running the API

This system relies on [postgrest](https://postgrest.org/) to run the API. The executable can be downloaded from the 
postgrest website or installed through a package manager. To run the API you can use the `make` target:

```shell
make api
```

This assumes two things:
1. `postgrest` is in your `$PATH`
2. you have all the required configurations [set up as environment variables as outlined in the docs](https://postgrest.org/en/stable/configuration.html).


## Maintenance

### Java dependencies

To check for dependencies that require updates due to registered vulnerabilities:

```sh
./gradlew dependencyCheckAnalyze
```

You'll see terminal output after a couple of minutes and an HTML report will be generated in `build/reports`.

To check for all dependency updates:

```sh
./gradlew dependencyUpdates -DoutputFormatter=html
```

You'll see terminal output and an HTML report will be in `build/dependencyUpdates`.

### Gradle wrapper update

To update the gradle wrapper for the project

```shell
./gradlew wrapper --gradle-version <new version>
```
