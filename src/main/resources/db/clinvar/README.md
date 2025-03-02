# ClinVar data

These files manage data from ClinVar. This data is used when compiling submissions of CPIC data.

## How to use these

### 1. Load the data

    ./clinvar_download.sh

This will download a few files from the ClinVar FTP server and keep them in this directory. You should also download
[OrgTrack data for CPIC](https://www.ncbi.nlm.nih.gov/clinvar/?LinkName=orgtrack_clinvar&from_uid=505961) and store it 
as `clinvar_result.txt`. Hopefully this step can be automated at some point but it's currently unclear how. 

### 2. Setup the schema

    psql cpic -U cpic < clinvar_schema.sql

This will set up a `clinvar` schema and create a few tables that will hold data from the previous step.

This script will delete and recreate the clinvar data if the `clinvar` schema already exists.

### 3. Load the schema

    psql cpic -U cpic < clinvar_dataload.sql

This will actually load the data into the database.
