# CPIC Services

The following documentation is for people maintaining the CPIC database and API services. Most people will not find this 
useful but if you're curious how the services work or looking for examples of how to run your own services you may 
find something useful here.

If you're looking for how to use CPIC data, go read [the main README for this repo](../README.md).


## Running DB/API

The database should be run on a PostgreSQL 12 instance. It may work on other versions but they have not been tested. 
PostgreSQL is available on most OS's, [read the OS-specific documentation](https://www.postgresql.org/download/) for 
instructions.

The PostgREST API can be run on most major OS. Read the docs for 
[instructions on installation](http://postgrest.org/en/v7.0.0/tutorials/tut0.html). This directory contains a 
`postgrest.conf` file that you can use as an example for running PostgREST.

If you want to run the services via docker find the instructions below.


## Clearing & Populating the server with data

Make sure you're using the latest checkout of this repo and that at least the database container is up and running

    java -jar build/libs/<Current CpicData Build>.jar org.cpicpgx.db.FlywayClean

This will clear the database of all data and relations.

    java -jar build/libs/<Current CpicData Build>.jar org.cpicpgx.db.FlywayMigrate

This will set up all the DB objects and populate some support tables but it will still be mostly empty.

    java -jar build/libs/<Current CpicData Build>.jar org.cpicpgx.DataImport -d /home/cpic/cpic-support-files

This will import all the data from the CPIC data files and leave the server with an initial state of data

Once this is all completed, you should probably stop and restart all docker containers so documentation and API endpoints can be rebuilt.


## Exporting a DB archive

To export all data from the production CPIC server from a client machine:

    pg_dump cpic -h db.cpicpgx.org -p 5432 -U cpic > ~/path/to/cpic_db_dump.sql

This has been put into a Makefile that can be run from the server itself so making a db dump is:

    make dump


## File Services

CPIC files are stored and distributed from an AWS S3 bucket accessible via http. The files are available via the `files.cpicpgx.org` domain.

- `/data` - data files that are used to help render cpicpgx.org website. These are files generated from data in the database.
- `/images` - static image assets to be used on the website or in reports.
  - `/test_alerts` - graphics used in the Pre- and Post- test alert files.
- `/reports` - report files generated from data in the database, mostly stored in Excel or CSV formatted files.
  - `/archive` - timestamped tarball archives of the files that appear in this directory 
  - `/drugs` - drug-centric reports
  - `/genes` - gene-centric reports


## Running DB/API with Docker

If you can't run PostgreSQL or PostgREST on your OS for some reason you could try running the services via Docker
instead. This works well for a dev environment, but should probably not be used for a production environment.

### Creating the cpic-db image

The default empty DB image is published as `cpicpgx/cpic-db` to [Docker Hub](https://hub.docker.com/r/cpicpgx/cpic-db/).
You should be able to just use that pre-built image. The rest of these 
commands are for rebuilding and publishing that.

You can update this by being logged into Docker Hub (if you're not already logged in) 

    docker login

Then build the image

    docker build -t cpicpgx/cpic-db .

Then push the resulting image up to Docker Hub (this won't work if 
you're not added as a contributor to the repo)

    docker push cpicpgx/cpic-db

### Running CPIC API services

To run the API services (Postgrest/Swagger) on your __local dev machine__ use `docker-compose`. This relies on the host 
OS running the PostgreSQL database so make sure that's up and running before starting the API.

The API and swagger docs can be started using the command:

    docker-compose up -d

This will run in daemon mode so you won't see logs on stdout (but can [through docker](https://docs.docker.com/engine/reference/commandline/logs/)).
If you want to see logs on stdout remove the `-d`. This reads settings from the `docker-compose.yml` and 
`docker-compose.override.yml` files.

When you're ready to run this on a __production server__ use:

    docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

This uses [multiple compose files](https://docs.docker.com/compose/extends/#multiple-compose-files)
to control the different settings between dev and prod. Specifically, 
this will not run a local PostgreSQL instance and will rely on the one 
configured in `docker-compose-prod.yml` instead.

To shut down the docker-compose services (in any configuration):

    docker-compose down

To restart the services with the same configuration that started them:

    docker-compose restart

By default, the API is run on port 3000 and exposed to the host. So you should be able to make requests to
`http://localhost:3000` for API calls. The port number can be reconfigured in `docker-compose.yml`.

### Running just the database

To set up (and run) just a docker container of the database only

    docker run -p 5432:5432 --name cpic-db -d cpicpgx/cpic-db

To stop that container

    docker stop cpic-db

When that's installed, do the following to run it:

    docker start cpic-db

To connect to the db via the psql client

    psql -h 0.0.0.0 -p 5432 -U cpic
