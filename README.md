# CPIC Data Processor

Make sure you look in `src/main/resources`, copy `cpicData.properties.sample` to `cpicData.properties`, and fill in the appropriate values for your database.

The main entry point is `org.cpicpgx.importer.AlleleDirectoryProcessor`. It has an arg `-d` for specifying the directory of excel files you want to import. 