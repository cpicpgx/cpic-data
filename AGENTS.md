# AGENTS.md

This file provides guidance to coding agents when working with code in this repository.

## What This Repo Does

This is the data pipeline for [CPIC](https://www.clinpgx.org/cpic) (Clinical Pharmacogenomics Implementation Consortium). It manages a PostgreSQL database of pharmacogenomics data: gene-drug pairs, allele definitions, diplotype-phenotype mappings, dosing recommendations, and allele frequencies. The pipeline reads Excel/CSV files from a companion data repo (`cpic-support-files/`) and loads them into the database. Exporters then produce file artifacts published to S3 and powering the REST API (via PostgREST).

## Dev Environment Setup

```sh
make dev-init   # clone cpic-support-files + cpic-data.wiki submodules, install yarn deps
make compile    # build the fat JAR (build/libs/CpicData-all.jar)
```

The project uses Java 17 (Gradle + Shadow plugin for fat JAR) and Node.js (Yarn 3).

## Database Configuration

Environment variables (all have local defaults):

| Variable | Default | Purpose |
|---|---|---|
| `CPIC_HOST` | `localhost` | DB hostname |
| `CPIC_USER` | `cpic` | PostgreSQL role |
| `CPIC_PASS` | _(blank)_ | Password |
| `CPIC_DB` | `cpic` | Database name |
| `CPIC_SCHEMA` | `cpic` | Schema name |

Local `.env` file holds PharmGKB/PharmVar credentials (not DB config).

## Common Commands

```sh
# Build
./gradlew shadowJar          # build fat JAR
./gradlew test               # run Java unit tests (JUnit 5)
./gradlew test --tests "org.cpicpgx.importer.AlleleDefinitionImporterTest"  # single test

# Database lifecycle
make db-bootstrap            # create schema + roles via Node.js script
make db-migrate              # run Flyway migrations (requires compiled JAR)
make db-update               # download latest DB dump from S3 and restore locally
make db-refresh              # drop + recreate local DB from already-downloaded dump

# Data import (reads from cpic-support-files/)
make import                  # import all data types in order
make import-alleles          # allele definition tables
make import-function         # allele function reference
make import-phenotype        # gene phenotype tables
make import-recommendation   # dosing recommendation tables
make import-pair             # gene-drug pair data
make import-drug             # drug resource data
make import-frequency        # allele frequency tables

# Export / publish
make publish-files           # export all artifacts to out/ and upload to S3
make dump                    # pg_dump to out/db/
make archive                 # dump + upload to S3
make api                     # start PostgREST API server
```

## Architecture

### Data Flow

```
cpic-support-files/          (Excel/CSV source data, separate git repo)
        ↓  Importers (Java)
PostgreSQL (cpic schema)
        ↓  Exporters (Java)
out/                         (TSV, Excel, JSON artifacts)
        ↓  S3 upload
files.cpicpgx.org            (public downloads + PostgREST API)
```

### Java Package Structure (`src/main/java/org/cpicpgx/`)

- **`importer/`** — One class per data type, all extending `BaseDirectoryImporter`. Each walks a directory of Excel/CSV files and upserts into the DB.
- **`exporter/`** — One class per artifact type, extending `BaseExporter`. Reads from DB, writes files.
- **`workbook/`** — Apache POI wrappers for reading/writing Excel sheets. Each `*Workbook` class knows the structure of one file type.
- **`db/`** — `ConnectionFactory` (JDBC), `FlywayMigrate`/`FlywayClean` (schema management), `DbHarness` (query helpers).
- **`util/`** — `RowWrapper`/`WorkbookWrapper` (POI row/sheet abstraction), `FileStoreClient` (S3), `TextUtils`, `ActivityScoreComparator`.
- **`model/`** — Enums: `FileType`, `EntityType`, `GnomadPopulation`, `DrugGenePair`.
- **`DataImport.java`** — Main entry point; runs all importers in dependency order.
- **`DataArtifactArchive.java`** — Main entry point for exporting all artifacts.

### Node.js (`src/main/node/`)

- **`db/bootstrap.mjs`** — Creates PostgreSQL roles and database (runs before Flyway).
- **`db/bootstrap_api.mjs`** — Sets up PostgREST API roles/permissions.
- **`cpicapi.js`** / **`updateGenes.js`** / **`updatePublications.js`** — Utilities for syncing data with external APIs (PharmGKB, PharmVar).

### Database Schema

Managed by Flyway; 79 versioned migration SQL files in `src/main/resources/db/migration/`. All tables live in the `cpic` schema. The `cpic-support-files/` companion repo contains the raw data files that feed the importers.

### Adding a New Data Type

The typical pattern: create a `*Workbook` class in `workbook/`, a `*Importer` in `importer/` extending `BaseDirectoryImporter`, and a corresponding `*Exporter` in `exporter/`. Add a Flyway migration for any schema changes, then wire the new importer into `DataImport.java` and add a `make import-*` target.
