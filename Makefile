all: reports push

reports:
	rm -rf out/files/cpic_information
	java -cp build/libs/CpicData.jar org.cpicpgx.DataArtifactArchive -d out/files

push:
	aws s3 sync out/files/cpic_information/ s3://files.cpicpgx.org/reports/ --exclude "*" --include "*.xlsx" --include "*.txt" --profile cpic
