#!/bin/sh
curl -O "ftp://ftp.ncbi.nlm.nih.gov//pub/clinvar/tab_delimited/allele_gene.txt.gz"
curl -O "ftp://ftp.ncbi.nlm.nih.gov//pub/clinvar/tab_delimited/submission_summary.txt.gz"
curl -O "ftp://ftp.ncbi.nlm.nih.gov//pub/clinvar/tab_delimited/variation_allele.txt.gz"
curl -O "ftp://ftp.ncbi.nlm.nih.gov//pub/clinvar/tab_delimited/variant_summary.txt.gz"

# download the table from https://www.ncbi.nlm.nih.gov/clinvar/?LinkName=orgtrack_clinvar&from_uid=505961 and put in clinvar_result.txt
