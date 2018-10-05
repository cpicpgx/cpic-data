# CPIC Data

This repo contains all the DDL and code for defining the [CPIC](https://cpicpgx.org) data model and populating it with data.

## Setup

This project assumes you're running a Postgres 9+ database for loading/querying data.

Look in `src/main/resources`, copy `cpicData.properties.sample` to `cpicData.properties`, and fill in the appropriate values for your database.

## Running

The main entry points are in the `org.cpicpgx.importer` package. Check the javadocs on the individual files for command-line parameters. 