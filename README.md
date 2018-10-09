# CPIC Data

This repo contains all the DDL and code for defining the [CPIC](https://cpicpgx.org) data model and populating it with data.

## WARNING: NOT READY FOR USE :exclamation:  :exclamation:  :exclamation:

__:warning: This repo is under active development and, thus, not ready for any useful purpose. When this message disappears and a production release is on the Releases tab then it will be ready for review. :warning:__

__Seriously, you don't want to use this yet.__


## Setup

This project assumes you're running a Postgres 9+ database for loading/querying data.

Look in `src/main/resources`, copy `cpicData.properties.sample` to `cpicData.properties`, and fill in the appropriate values for your database.

## Running

### Bootstrapping the DB

This project uses Flyway configured through Gradle to set up the DB. Schema definition files are found in the `src/resources/db/migration` directory. Run the following to build the db:

You will need to set the Flyway "user" value (and possibly password). The best way is via an envvar called `FLYWAY_USER` but read the docs for other methods.

```sh
gradle flywayMigrate -i
```

### Bootstrapping Gene-specific Information

The entry points to load gene-specific data are in the `org.cpicpgx.importer` package. Check the javadocs on the individual importer classes for command-line parameters.

To load all data at once, use the `DataImport` class. This takes a `-d` parameter that is a directory that expects the following subfolders containing excel files:

- allele_definition_tables
- allele_functionality_reference
- diplotype_phenotype_tables
- frequency_table

Use gradle to build the jar file:

```sh
gradle jar
```

Then put that jar on the classpath and run `org.cpicpgx.DataImport` class:

```sh
java -cp build/libs/**CURRENT_JAR**.jar org.cpicpgx.DataImport -d **PATH_TO_DATA_DIRECTORY**
```
