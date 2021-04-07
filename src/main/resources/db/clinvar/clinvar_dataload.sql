COPY clinvar.submission from program 'gzcat submission_summary.txt.gz | grep -vE "^#"';
COPY clinvar.allele_gene from program 'gzcat allele_gene.txt.gz | grep -vE "^#"';
COPY clinvar.variation_allele from program 'gzcat variation_allele.txt.gz | grep -vE "^#"';
COPY clinvar.variant_summary from program 'gzcat variant_summary.txt.gz | grep -vE "^#"';
COPY clinvar.orgtrack from program 'cat clinvar_result.txt | grep -vE "^#"';
