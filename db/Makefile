clear:
	-dropdb cpic
	-psql -c 'drop role cpic;'

build:
	psql -c 'create role cpic;'
	createdb cpic -O cpic
	psql cpic -q < cpic.schema.sql

rebuild: clear build
