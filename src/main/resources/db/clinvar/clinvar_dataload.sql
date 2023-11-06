\copy clinvar.submission from program 'gzip -cd submission_summary.txt.gz | grep -vE "^#"';
\copy clinvar.allele_gene from program 'gzip -cd allele_gene.txt.gz | grep -vE "^#"';
\copy clinvar.variation_allele from program 'gzip -cd variation_allele.txt.gz | grep -vE "^#"';
\copy clinvar.variant_summary from program 'gzip -cd variant_summary.txt.gz | grep -vE "^#"';
\copy clinvar.orgtrack from program 'cat clinvar_result.txt | grep -vE "^Name"';
