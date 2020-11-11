ARCHIVE_NAME = cpic_db_dump
TAG_NAME = $(ARCHIVE_NAME)-$(shell git describe --tags).sql

.PHONY: dump
dump:
	mkdir -p out
	pg_dump cpic -f out/${TAG_NAME} --no-privileges --schema=cpic --no-owner

.PHONY: upload
upload:
	gzip out/${TAG_NAME}
	aws s3 cp out/${TAG_NAME}.gz s3://files.cpicpgx.org/data/database/ --profile cpic

.PHONY: archive
archive: dump upload
