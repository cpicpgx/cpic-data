# CPIC Data

This repo contains all the DDL and code for defining the [CPIC](https://cpicpgx.org) data model and populating it with data.

## Setup

This project assumes you're running a Postgres 9+ database for loading/querying data.

Look in `src/main/resources`, copy `cpicData.properties.sample` to `cpicData.properties`, and fill in the appropriate values for your database.

## Running

To set up the DB, look in the `db` folder. Run the following to build the db:

```sh
make build
```

Next, apply the right permissions to ensure your user account can import data

The entry points to load data are in the `org.cpicpgx.importer` package. Check the javadocs on the individual files for command-line parameters.

```sh
java -cp build/libs/**CURRENT_JAR**.jar org.cpicpgx.importer.AlleleDirectoryProcessor -d **PATH_TO**/allele_definition_tables
java -cp build/libs/**CURRENT_JAR**.jar org.cpicpgx.importer.AlleleFrequencyImporter -d **PATH_TO**/frequency_table
java -cp build/libs/**CURRENT_JAR**.jar org.cpicpgx.importer.FunctionReferenceImporter -d **PATH_TO**/allele_functionality_reference
java -cp build/libs/**CURRENT_JAR**.jar org.cpicpgx.importer.DiplotypePhenotypeImporter -d **PATH_TO**/diplotype_phenotype_tables
```
