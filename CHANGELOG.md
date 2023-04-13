## [1.25.0](https://github.com/cpicpgx/cpic-data/compare/v1.24.0...v1.25.0) (2023-04-13)


### Features

* add a warning when recommendation drugs do not have RxNorm coverage ([1901a53](https://github.com/cpicpgx/cpic-data/commit/1901a5355965f6ebc18d8f35cc7c3d0cd50b5dfd))
* add information for SSRI/SNRI guideline ([3f3e735](https://github.com/cpicpgx/cpic-data/commit/3f3e735ac3c0bcb7693eaae5bfe9494a03ccc977))
* include RxNorm CUI as explicit field in PharmCAT drug data ([49d0e4a](https://github.com/cpicpgx/cpic-data/commit/49d0e4ac39c6fe05366b8d51765b46c780aa60cf))

## [1.24.0](https://github.com/cpicpgx/cpic-data/compare/v1.23.1...v1.24.0) (2023-03-23)


### Features

* flag for diplotype/phenotype frequencies in gene table ([87fb407](https://github.com/cpicpgx/cpic-data/commit/87fb4078171138f9ab0eaaa0c3edb978bb375756))


### Bug Fixes

* expand the list of genes to not display diplotype/phenotype frequencies for ([3361c17](https://github.com/cpicpgx/cpic-data/commit/3361c172ed157bd33fdbea4e61e967b87a837570))
* fix bad pg_dump arg in dump script ([3af3eee](https://github.com/cpicpgx/cpic-data/commit/3af3eee2a459c217f51697a316f3d61755aab8f3)), closes [#22](https://github.com/cpicpgx/cpic-data/issues/22)
* remove usage of non-existent script ([503d4ef](https://github.com/cpicpgx/cpic-data/commit/503d4ef9715c939a92fd20335ad5d160c185b098))

## [1.23.1](https://github.com/cpicpgx/cpic-data/compare/v1.23.0...v1.23.1) (2023-03-10)


### Bug Fixes

* add parser for notes sheet in test alert file ([d0749e7](https://github.com/cpicpgx/cpic-data/commit/d0749e74ac8912dcf29e94efe1e258f2ad329981))

## [1.23.0](https://github.com/cpicpgx/cpic-data/compare/v1.22.2...v1.23.0) (2023-02-28)


### Features

* add dosing info, alt drug, and other prescribing tags to recommendations ([88c10de](https://github.com/cpicpgx/cpic-data/commit/88c10deb113969fd21a7585931b90e27e908b697))


### Bug Fixes

* add a check for expected genes in recommendation files ([f307e2d](https://github.com/cpicpgx/cpic-data/commit/f307e2d8aa62e5ee11c33d50bab3e40decdefa2f))

## [1.22.2](https://github.com/cpicpgx/cpic-data/compare/v1.22.1...v1.22.2) (2023-02-03)


### Bug Fixes

* fix frequency exporter to not show null allele data ([79745ba](https://github.com/cpicpgx/cpic-data/commit/79745babc90d6f6083b17bc4efafbacc03ed2efe))

## [1.22.1](https://github.com/cpicpgx/cpic-data/compare/v1.22.0...v1.22.1) (2023-01-31)


### Bug Fixes

* properly encode spaces in filenames uploaded to S3 ([1394d80](https://github.com/cpicpgx/cpic-data/commit/1394d8037cd47f251c4e4cda175b9a5f3ab0d630))

## [1.22.0](https://github.com/cpicpgx/cpic-data/compare/v1.21.6...v1.22.0) (2023-01-27)


### Features

* add new make script for copying the DB ([e9ebb6a](https://github.com/cpicpgx/cpic-data/commit/e9ebb6ae372b7a79cfd8d47ff0a231d3fe4daacc))


### Bug Fixes

* data update ([277ceef](https://github.com/cpicpgx/cpic-data/commit/277ceef4778f98f569c43e2aac52d72d25a9a70f))

## [1.21.6](https://github.com/cpicpgx/cpic-data/compare/v1.21.5...v1.21.6) (2022-12-21)


### Bug Fixes

* data update ([c4dcd9d](https://github.com/cpicpgx/cpic-data/commit/c4dcd9d517ebdf66bdbd8e95295269439d4c49f1))

## [1.21.5](https://github.com/cpicpgx/cpic-data/compare/v1.21.4...v1.21.5) (2022-12-20)


### Bug Fixes

* update data ([2476a6c](https://github.com/cpicpgx/cpic-data/commit/2476a6ca40ef8482c1e5ac69d0af5259a66fea5e))

## [1.21.4](https://github.com/cpicpgx/cpic-data/compare/v1.21.3...v1.21.4) (2022-12-20)


### Bug Fixes

* update data ([3d23c05](https://github.com/cpicpgx/cpic-data/commit/3d23c055f997f6b02371090997b3901d839c5548))

## [1.21.3](https://github.com/cpicpgx/cpic-data/compare/v1.21.2...v1.21.3) (2022-10-31)


### Bug Fixes

* remove unneeded data caching scripts ([3cf620d](https://github.com/cpicpgx/cpic-data/commit/3cf620dafd92bc20ed5a4514826504ab870ef3ed))

## [1.21.2](https://github.com/cpicpgx/cpic-data/compare/v1.21.1...v1.21.2) (2022-10-24)


### Bug Fixes

* fix population summary stats on frequency export sheets ([d4c13f8](https://github.com/cpicpgx/cpic-data/commit/d4c13f880579ce05096e8f145d8a04d22b833166))

## [1.21.1](https://github.com/cpicpgx/cpic-data/compare/v1.21.0...v1.21.1) (2022-10-20)


### Bug Fixes

* do not generate diplotype/phenotype frequencies for x-linked genes ([8f4a7d9](https://github.com/cpicpgx/cpic-data/commit/8f4a7d91f218f53e0898cc84c3f88baad3d0f39e))
* normalize user-generated function descriptions ([ca8846d](https://github.com/cpicpgx/cpic-data/commit/ca8846d13ad31cfc5050c8d5230abe4a5bc996aa))
* put CYP4F2 *3 mention in warfarin diagram ([d0f57d4](https://github.com/cpicpgx/cpic-data/commit/d0f57d455f0f8684bdf26f30ef36b47cade1a0db))

## [1.21.0](https://github.com/cpicpgx/cpic-data/compare/v1.20.0...v1.21.0) (2022-10-04)


### Features

* add drug flowcharts for G6PD drugs ([42c750f](https://github.com/cpicpgx/cpic-data/commit/42c750fbb124249994f0ec199ac4d85f56b3d4ee))


### Bug Fixes

* add pointer to fosphenytoin flow chart ([f395963](https://github.com/cpicpgx/cpic-data/commit/f395963a486f5dfadd8276bc7dbe11a48a29afc3))
* fix ordering bug for variants in PharmCAT export ([795cafa](https://github.com/cpicpgx/cpic-data/commit/795cafa2846d692d034cb5d09133d69efe3853ea))
* standardize activity values and scores to always use decimal format ([4e458f4](https://github.com/cpicpgx/cpic-data/commit/4e458f49c42d8eb602e39e0cf7d3536c136d8746))

## [1.20.0](https://github.com/cpicpgx/cpic-data/compare/v1.19.1...v1.20.0) (2022-09-15)


### Features

* add import and export for a guideline data ([c9f2be2](https://github.com/cpicpgx/cpic-data/commit/c9f2be2205fd4eb8fde356786b96ba18edacd108))
* change reference prop for allele_definition and add inferredFrequency prop for allele ([b98ccba](https://github.com/cpicpgx/cpic-data/commit/b98ccbad0b412324d7db35cb921c598135950926))


### Bug Fixes

* base the imported drug name off of PharmGKB ([e417eea](https://github.com/cpicpgx/cpic-data/commit/e417eea426dae72d89b0d47887d5c72bf029ea2d))
* create directory if it doesn't already exist ([5059eac](https://github.com/cpicpgx/cpic-data/commit/5059eacea73fd635571142555caff6637ae92823))
* fix custom directory support ([036c8ae](https://github.com/cpicpgx/cpic-data/commit/036c8ae586eb1d8191461a28423c288cfe11004a))
* fix directory name ([3f9bdd0](https://github.com/cpicpgx/cpic-data/commit/3f9bdd022f93cd25dc483ee9bb1eb7ff33900406))
* fix NPE from change to BigDecimal ([faf86ec](https://github.com/cpicpgx/cpic-data/commit/faf86ecd396348f3c92876b333df6f2c34575171))
* fix ordering of data in guideline export class ([c0daf38](https://github.com/cpicpgx/cpic-data/commit/c0daf38f957cc6dada81e7fb0c6a94887ef70c5f))
* fix pharmcat files to use new "matchesreferencesequence" field ([17ad5e9](https://github.com/cpicpgx/cpic-data/commit/17ad5e96a85f63293953881f60380ff63fcc9da3))
* include PharmGKB ID update in gene resource import ([f10a6f3](https://github.com/cpicpgx/cpic-data/commit/f10a6f30ffdd94aa4a663815f6b668ef12e1a614))
* make recommendation drug lookup more forgiving ([8a99f92](https://github.com/cpicpgx/cpic-data/commit/8a99f92bbaeaae1011b5f234fb2c8f536435488e))
* more validation when importing guidelines and pairs ([8467442](https://github.com/cpicpgx/cpic-data/commit/8467442549d57f70dd61b472c44d913aead7f0b7))
* remove bloated, unused description field in pharmcat gene_phenotypes.json file ([b335a7b](https://github.com/cpicpgx/cpic-data/commit/b335a7b1dd6355de7d0d3cbf6bc92c67fafc117b))
* remove unused and redundant view ([19abe9a](https://github.com/cpicpgx/cpic-data/commit/19abe9aa40c488ca00165bce45e15408fb50c94a))
* stop including inserts file with CPIC release ([9ab357c](https://github.com/cpicpgx/cpic-data/commit/9ab357c00ad9033613063b7ddb198aadeb15ea0f))
* use BigDecimal to avoid precision loss ([5dfc6af](https://github.com/cpicpgx/cpic-data/commit/5dfc6affda427fa2845f003d2b1517620000a3e9))

## [1.19.1](https://github.com/cpicpgx/cpic-data/compare/v1.19.0...v1.19.1) (2022-08-25)


### Bug Fixes

* clear out old dump directory before creating new dump ([4d7d85a](https://github.com/cpicpgx/cpic-data/commit/4d7d85ac1a29e5f9d5c20a6540d03f1385b03213))
* create directory if it doesn't already exist ([3bf8e15](https://github.com/cpicpgx/cpic-data/commit/3bf8e158fd342ee86be3f36dcafa0a1d4f0fd29f))

## [1.19.0](https://github.com/cpicpgx/cpic-data/compare/v1.18.0...v1.19.0) (2022-08-16)


### Features

* add activity values to phenotype PharmCAT output ([83fdafe](https://github.com/cpicpgx/cpic-data/commit/83fdafedfd633c8fa55d674b6d292424fff17327))
* add statistics gathering code and database table ([0ff01d2](https://github.com/cpicpgx/cpic-data/commit/0ff01d2403662348df2368cfbb4dbff31f9abbdf))


### Bug Fixes

* add db bootstrap script and db-init make task ([6db458d](https://github.com/cpicpgx/cpic-data/commit/6db458d3f74dba39289c8220ad08c7c96fcbaed2))
* change node to load environment data from env file ([4ce7bdf](https://github.com/cpicpgx/cpic-data/commit/4ce7bdf5700d66958bdff1794d1f263d4a1b8343))
* fix gradle permissions on unix ([a643fe8](https://github.com/cpicpgx/cpic-data/commit/a643fe86fc008c93b117a79c0d94cfbbc7916c81))
* fix startup problems ([e2b0f8a](https://github.com/cpicpgx/cpic-data/commit/e2b0f8af9c35040ca90f3bb3f34f0d296c99eead))
* move node env config so it loads properly ([71d205c](https://github.com/cpicpgx/cpic-data/commit/71d205c89c27b7102a2417e2d3abb378564c78ca))

## [1.18.0](https://github.com/cpicpgx/cpic-data/compare/v1.17.0...v1.18.0) (2022-06-10)


### Features

* add guidelineurl to the pharmgkb_recommentation function ([65902c0](https://github.com/cpicpgx/cpic-data/commit/65902c0d3277d5c39c6f4c85effd5de4c3c965fa))


### Bug Fixes

* add Pair data loading back to DataImport ([0afcaf9](https://github.com/cpicpgx/cpic-data/commit/0afcaf9b1556d5f3d52256b1d866e6163db807b0))
* add sanity check for column header formatting ([25228b4](https://github.com/cpicpgx/cpic-data/commit/25228b4feff91f50b20a668ef4b1dbfb8ca74ec4))
* configuration update ([d30e97d](https://github.com/cpicpgx/cpic-data/commit/d30e97d7b161a54dde22a3528aba959f198bcd4d))
* improve cli handling ([7a2bc0f](https://github.com/cpicpgx/cpic-data/commit/7a2bc0f005b5c8ced455c6ee00c7061382ad6dfa))
* use unicode instead of windows-1252 encoded characters ([d80bd2b](https://github.com/cpicpgx/cpic-data/commit/d80bd2b7246082376ec7848668cb4c46cf3f1d46))
