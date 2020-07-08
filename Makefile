ARCHIVE_NAME = cpic_db_dump
DATED_NAME = $(ARCHIVE_NAME)-$(shell date +'%Y%m%d').sql

dump:
	mkdir -p out
	pg_dump cpic -f out/${DATED_NAME} --no-privileges --schema=public --no-owner

upload:
	gzip out/${DATED_NAME}
	aws s3 cp out/${DATED_NAME}.gz s3://files.cpicpgx.org/data/database/ --profile cpic

archive: dump upload
