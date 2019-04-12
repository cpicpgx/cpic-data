# CPIC Data

This repo contains all the DDL and code for defining the [CPIC](https://cpicpgx.org) data model and populating it with data.

## WARNING: NOT READY FOR USE :exclamation:  :exclamation:  :exclamation:

__:warning: This repo is under active development and, thus, not ready for any useful purpose. When this message disappears and a production release is on the Releases tab then it will be ready for review. :warning:__

__Seriously, you don't want to use this yet.__


## Setup

This project assumes you're running a Postgres 9+ database for loading/querying data.

Look in `src/main/resources`, copy `cpicData.properties.sample` to `cpicData.properties`, and fill in the appropriate values for your database.

## Running

Some steps below will require a compiled version (jar) of this project. Use gradle to build the jar file:

```sh
gradle jar
```

This will place a compiled jar in the `build/libs` directory.


### Bootstrapping the DB

This project uses Flyway configured through Gradle to set up the DB. Schema definition files are found in the `src/resources/db/migration` directory. Run the following to build the db:

You will need to set the Flyway "user" value (and possibly password). The best way is via an envvar called `FLYWAY_USER` but read the docs for other methods.

```sh
gradle flywayMigrate -i
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
