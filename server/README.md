# CPIC Services

## Running DB/API with Docker


### Creating the cpic-db image

The default empty DB image is published as `cpicpgx/cpic-db` to [Docker Hub](https://hub.docker.com/r/cpicpgx/cpic-db/).
You should be able to just use that pre-built image. The rest of these 
commands are for rebuilding and publishing that.

You can update this by being logged into Docker Hub and then executing 
the following. Build the image:

    docker build -t cpicpgx/cpic-db .

Then push the resulting image up to Docker Hub (this won't work if 
you're not added as a contributor to the repo)

    docker push cpicpgx/cpic-db

### Running CPIC API services

To run all the services (DB/Postgrest/Swagger) on your 
__local dev machine__ use _docker-compose_:

    docker-compose up -d

This will run in daemon mode so you won't see logs, if you want to see 
logs remove the `-d`. This reads settings from the `docker-compose.yml` 
and `docker-compose.override.yml` files.

When you're ready to run this on a __production server__ use:

    docker-compose -f docker-compose.yml -f docker-compose.prod.yml up -d

This uses [multiple compose files](https://docs.docker.com/compose/extends/#multiple-compose-files)
to control the different settings between dev and prod. Specifically, 
this will not run a local PostgreSQL instance and will rely on the one 
configured in `docker-compose-prod.yml` instead.

To shut down the docker-compose services (in any configuration):

    docker-compose down

### Running just the database

To set up (and run) just a docker container of the database only

    docker run -p 5432:5432 --name cpic-db -d cpicpgx/cpic-db

To stop that container

    docker stop cpic-db

When that's installed, do the following to run it:

    docker start cpic-db

To connect to the db via the psql client

    psql -h 0.0.0.0 -p 5432 -U cpic


## File Services

CPIC files are stored and distributed from an AWS S3 bucket accessible via http. The files are available via the `files.cpicpgx.org` domain.

- `/data` - data files that are used to help render cpicpgx.org website. These are files generated from data in the database.
- `/images` - static image assets to be used on the website or in reports.
  - `/test_alerts` - graphics used in the Pre- and Post- test alert files.
- `/reports` - report files generated from data in the database, mostly stored in Excel or CSV formatted files.
  - `/archive` - timestamped tarball archives of the files that appear in this directory 
  - `/drugs` - drug-centric reports
  - `/genes` - gene-centric reports