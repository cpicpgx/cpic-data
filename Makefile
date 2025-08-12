ARCHIVE_NAME = cpic_db_dump
TAG_NAME = $(ARCHIVE_NAME)-$(shell git describe --tags).sql
UNTAG_NAME = ${ARCHIVE_NAME}.sql
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
	pg_dump cpic -U cpic -f out/db/${TAG_NAME} --no-privileges --schema=cpic --no-owner
	pg_dump cpic -U cpic -f out/db/${INSERTS_NAME} --data-only --column-inserts --schema=cpic --no-owner

.PHONY: upload
upload:
	gzip out/db/${TAG_NAME}
	gzip out/db/${INSERTS_NAME}
	aws s3 cp out/db/${TAG_NAME}.gz s3://files.cpicpgx.org/data/database/ --profile cpic
	aws s3 cp out/db/${TAG_NAME}.gz s3://files.cpicpgx.org/data/database/${UNTAG_NAME}.gz --profile cpic
	aws s3 cp out/db/${INSERTS_NAME}.gz s3://files.cpicpgx.org/data/database/ --profile cpic
	@ echo "Full DB export available: https://files.cpicpgx.org/data/database/${TAG_NAME}.gz"

.PHONY: upload-flow-charts
upload-flow-charts:
	aws s3 sync cpic-support-files/images/flow_chart s3://files.cpicpgx.org/images/flow_chart --profile cpic

.PHONY: archive
archive: dump upload
.PHONY: publish-db
publish-db: archive

.PHONY: update-wiki-toc
update-wiki-toc:
	markdown-toc -i cpic-data.wiki/Home.md


.PHONY: compile
compile:
	${GRADLE_CMD} shadowJar
.PHONY: jar
jar: compile


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
db-init: db-bootstrap db-update db-migrate
	java -jar build/libs/CpicData-all.jar -d cpic-support-files

.PHONY: db-download
db-download:
	mkdir -p out
	curl https://files.cpicpgx.org/data/database/cpic_db_dump.sql.gz --output out/cpic_db_dump.sql.gz

.PHONY: db-refresh
db-refresh:
	dropdb cpic -h localhost -U postgres && createdb cpic -h localhost -U postgres -O cpic
	gzip -cd out/cpic_db_dump.sql.gz | psql -d cpic -h localhost -U cpic

.PHONY: db-update
db-update: db-download db-refresh
	@echo "Database image copied and refreshed"

.PHONY: db-update-staging
db-update-staging:
	@echo "Database image copied and refreshed"
	dropdb cpic-staging -h localhost -U postgres
	createdb cpic-staging -h localhost -U postgres
	pg_dump cpic | psql cpic-staging

.PHONY: db-migrate
db-migrate: compile
	java -cp build/libs/CpicData-all.jar org.cpicpgx.db.FlywayMigrate


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


.PHONY: import
import:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.DataImport -d cpic-support-files

.PHONY: import-alleles
import-alleles:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.importer.AlleleDefinitionImporter -d cpic-support-files/allele_definition

.PHONY: import-function
import-function:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.importer.FunctionReferenceImporter -d cpic-support-files/allele_function_reference

.PHONY: import-drug
import-drug:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.importer.DrugImporter -d cpic-support-files/drug_resource

.PHONY: import-frequency
import-frequency:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.importer.AlleleFrequencyImporter -d cpic-support-files/frequency

.PHONY: import-cds
import-cds:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.importer.GeneCdsImporter -d cpic-support-files/gene_cds

.PHONY: import-phenotype
import-phenotype:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.importer.GenePhenotypeImporter -d cpic-support-files/gene_phenotype

.PHONY: import-gene
import-gene:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.importer.GeneReferenceImporter -d cpic-support-files/gene_resource

.PHONY: import-pair
import-pair:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.importer.PairImporter -d cpic-support-files/pair

.PHONY: import-recommendation
import-recommendation:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.importer.RecommendationImporter -d cpic-support-files/recommendation

.PHONY: import-test
import-test:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.importer.TestAlertImporter -d cpic-support-files/test_alert


.PHONY: publish-files
publish-files:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.DataArtifactArchive -d out -u

.PHONY: publish-diplotypes
publish-diplotypes:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.exporter.DiplotypePhenotypeExporter -d out -u

.PHONY: publish-frequency
publish-frequency:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.exporter.FrequencyExporter -d out -u

.PHONY: stats
stats:
	java -cp build/libs/CpicData-all.jar org.cpicpgx.stats.StatisticsCollector
