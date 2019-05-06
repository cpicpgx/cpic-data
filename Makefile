ARCHIVE_NAME = cpic_files_archive
DATED_NAME = $(ARCHIVE_NAME)-$(shell date +'%Y_%m_%d')
TARBALL_NAME = $(DATED_NAME).tar.gz

all: reports push archive

reports:
	rm -rf out/files/cpic_information
	java -cp build/libs/CpicData.jar org.cpicpgx.DataArtifactArchive -d out/files

push:
	aws s3 sync out/files/cpic_information/ s3://files.cpicpgx.org/reports/ --exclude "*" --include "*.xlsx" --include "*.txt" --profile cpic

archive:
	mkdir $(ARCHIVE_NAME)
	cp -r out/files/cpic_information/* $(ARCHIVE_NAME)
	tar -czf $(TARBALL_NAME) $(ARCHIVE_NAME)
	rm -rf $(ARCHIVE_NAME)
	aws s3 cp $(TARBALL_NAME) s3://files.cpicpgx.org/reports/archive/ --profile cpic
	mv $(TARBALL_NAME) out
