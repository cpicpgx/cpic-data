ARCHIVE_NAME = cpic_db_dump
TAG_NAME = $(ARCHIVE_NAME)-$(shell git describe --tags).sql
INSERTS_NAME = $(ARCHIVE_NAME)-$(shell git describe --tags)_inserts.sql

.PHONY: dump
dump:
	mkdir -p out
	pg_dump cpic -f out/${TAG_NAME} --no-privileges --schema=cpic --no-owner
	pg_dump cpic -f out/${INSERTS_NAME} --data-only --column-inserts --schema=cpic --no-owner

.PHONY: upload
upload:
	gzip out/${TAG_NAME}
	gzip out/${INSERTS_NAME}
	aws s3 cp out/${TAG_NAME}.gz s3://files.cpicpgx.org/data/database/ --profile cpic
	aws s3 cp out/${INSERTS_NAME}.gz s3://files.cpicpgx.org/data/database/ --profile cpic

.PHONY: archive
archive: dump upload

.PHONY: update-wiki-toc
update-wiki-toc:
	markdown-toc -i cpic-data.wiki/Home.md

.PHONY: db-refresh
db-refresh:
	dropdb cpic && createdb cpic
	gzip -cd out/cpic_prod_db.sql.gz | psql cpic

.PHONY: compile
compile:
	./gradlew jar

.PHONY: db-migrate
db-migrate: compile
	java -cp build/libs/CpicData.jar org.cpicpgx.db.FlywayMigrate
