ARCHIVE_NAME = cpic_db_dump
DATED_NAME = $(ARCHIVE_NAME)-$(shell date +'%Y%m%d').sql

dump:
	pg_dump cpic -f ${DATED_NAME} --no-privileges --schema=public --no-owner

upload:
	gzip ${DATED_NAME}
	aws s3 cp ${DATED_NAME}.gz s3://files.cpicpgx.org/data/database/ --profile cpic
	rm -f ${DATED_NAME}.gz

archive: dump upload
