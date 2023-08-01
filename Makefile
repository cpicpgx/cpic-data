ARCHIVE_NAME = cpic_db_dump
TAG_NAME = $(ARCHIVE_NAME)-$(shell git describe --tags).sql
INSERTS_NAME = $(ARCHIVE_NAME)-$(shell git describe --tags)_inserts.sql

ifeq ($(OS),Windows_NT)
	YARN_CMD := cmd /c yarn --silent
	GRADLE_CMD := cmd /c gradlew.bat
	GIT_CLONE_OPTS := --config core.filemode=false
else
	YARN_CMD := yarn --silent
	GRADLE_CMD := ./gradlew
	GIT_CLONE_OPTS :=
endif


.PHONY: dev-init      # initializes dev environment
dev-init:
	@if [ ! -d "cpic-data.wiki" ];     then echo "Cloning wiki...";            git clone                   https://github.com/cpicpgx/cpic-data.wiki.git     cpic-data.wiki;     fi
	@if [ ! -d "cpic-support-files" ]; then echo "Cloning cpic-support-files"; git clone ${GIT_CLONE_OPTS} https://github.com/cpicpgx/cpic-support-files.git cpic-support-files; fi
	@${YARN_CMD}


.PHONY: dump
dump:
	rm -rf out/db
	mkdir -p out/db
	rm -f out/db/${TAG_NAME}
	rm -f out/db/${INSERTS_NAME}
	pg_dump cpic -f out/db/${TAG_NAME} --no-privileges --schema=cpic --no-owner
	pg_dump cpic -f out/db/${INSERTS_NAME} --data-only --column-inserts --schema=cpic --no-owner

.PHONY: upload
upload:
	gzip out/db/${TAG_NAME}
	gzip out/db/${INSERTS_NAME}
	aws s3 cp out/db/${TAG_NAME}.gz s3://files.cpicpgx.org/data/database/ --profile cpic
	aws s3 cp out/db/${INSERTS_NAME}.gz s3://files.cpicpgx.org/data/database/ --profile cpic
	@ echo "DB dump published to https://files.cpicpgx.org/data/database/${TAG_NAME}.gz"

.PHONY: upload-flow-charts
upload-flow-charts:
	aws s3 sync cpic-support-files/images/flow_chart s3://files.cpicpgx.org/images/flow_chart --profile cpic

.PHONY: archive
archive: dump upload

.PHONY: update-wiki-toc
update-wiki-toc:
	markdown-toc -i cpic-data.wiki/Home.md


.PHONY: compile
compile:
	${GRADLE_CMD} jar


.PHONY: data-changelog
data-changelog:
	@node src/main/node/writeChangelog.mjs

.PHONY: db-bootstrap
db-bootstrap:
	@node src/main/node/db/bootstrap.mjs

.PHONY: db-teardown
db-teardown:
	dropdb cpic -h localhost -U postgres
	psql -X -q  -h localhost -U postgres -c "drop user cpic"

.PHONY: db-init
db-init: db-bootstrap db-migrate
	java -jar build/libs/CpicData.jar -d cpic-support-files

.PHONY: db-download
db-download:
	aws s3 cp s3://cpic.backup/db/cpic_prod_db.sql.gz out/cpic_prod_db.sql.gz --profile cpic

.PHONY: db-refresh
db-refresh:
	dropdb cpic -h localhost -U postgres && createdb cpic -h localhost -U postgres
	gzip -cd out/cpic_prod_db.sql.gz | psql -d cpic -h localhost -U postgres

.PHONY: db-copy
db-copy: db-download db-refresh
	@echo "Database image copied and refreshed"

.PHONY: db-migrate
db-migrate: compile
	java -cp build/libs/CpicData.jar org.cpicpgx.db.FlywayMigrate


.PHONY: api-bootstrap
api-bootstrap:
	@node src/main/node/db/bootstrap_api.mjs

.PHONY: api
api:
	postgrest


.PHONY: clean
clean:
	${GRADLE_CMD} --quiet clean
	rm -rf out build logs

.PHONY: db-clean
db-clean:
	psql -h localhost -U postgres -c "drop database cpic; drop role web_anon; drop role cpic_api; drop role auth; drop role cpic;"
