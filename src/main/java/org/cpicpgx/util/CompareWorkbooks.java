package org.cpicpgx.util;

import com.google.common.base.Joiner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

public class CompareWorkbooks {
  private static final Logger sf_logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  public static void main(String[] args) {
    Options options = new Options();
    options.addOption("a", true, "first to compare");
    options.addOption("b", true, "second to compare");
    CommandLineParser clp = new DefaultParser();
    try {
      CommandLine cl = clp.parse(options, args);
      CompareWorkbooks compareWorkbooks = new CompareWorkbooks(cl.getOptionValue("a"), cl.getOptionValue("b"));
      compareWorkbooks.compare();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private final Path f_fileAPath;
  private final Path f_fileBPath;

  private final WorkbookWrapper f_workbookA;
  private final WorkbookWrapper f_workbookB;

  private CompareWorkbooks(String fileA, String fileB) throws IOException {
    if (StringUtils.isBlank(fileA) || StringUtils.isBlank(fileB)) {
      throw new RuntimeException("Missing file to compare");
    }

    f_fileAPath = Paths.get(fileA);
    f_fileBPath = Paths.get(fileB);

    File fileAFile = f_fileAPath.toFile();
    File fileBFile = f_fileBPath.toFile();

    if (!fileAFile.exists() || !fileAFile.isFile()) {
      throw new FileNotFoundException("Not found " + f_fileAPath.toAbsolutePath());
    }
    if (!fileBFile.exists() || !fileBFile.isFile()) {
      throw new FileNotFoundException("Not found " + f_fileBPath.toAbsolutePath());
    }

    try (
        FileInputStream inA = new FileInputStream(fileAFile);
        FileInputStream inB = new FileInputStream(fileBFile)
    ) {
      f_workbookA = new WorkbookWrapper(inA);
      f_workbookB = new WorkbookWrapper(inB);
    }
  }

  private void compare() {
    sf_logger.info("Comparing\n\t{}\n\t{}", f_fileAPath.getFileName(), f_fileBPath.getFileName());

    List<String> sheetsA = f_workbookA.getSheetNameList().stream()
        .map(n -> n.toLowerCase(Locale.ROOT))
        .collect(Collectors.toList());
    List<String> sheetsB = f_workbookB.getSheetNameList().stream()
        .map(n -> n.toLowerCase(Locale.ROOT))
        .collect(Collectors.toList());

    List<String> commonSheets = new ArrayList<>(sheetsA);
    commonSheets.retainAll(sheetsB);

    List<String> onlyASheets = new ArrayList<>(sheetsA);
    onlyASheets.removeAll(sheetsB);
    if (onlyASheets.size() > 0) {
      sf_logger.warn("{} is missing sheets: [{}]", f_fileBPath.getFileName(), Joiner.on("; ").join(onlyASheets));
    }

    List<String> onlyBSheets = new ArrayList<>(sheetsB);
    onlyBSheets.removeAll(sheetsA);
    if (onlyBSheets.size() > 0) {
      sf_logger.warn("{} is missing sheets: [{}]", f_fileAPath.getFileName(), Joiner.on("; ").join(onlyBSheets));
    }

    for (String sheetName : commonSheets) {
      sf_logger.info("Checking sheet: {}", sheetName);

      f_workbookA.currentSheetIs(sheetName);
      f_workbookB.currentSheetIs(sheetName);
      compareSheets();
    }
  }

  private void compareSheets() {
    int rowCount = Math.max(f_workbookA.currentSheet.getLastRowNum(), f_workbookB.currentSheet.getLastRowNum());

    for (int i = 0; i < rowCount; i++) {
      RowWrapper rowA = f_workbookA.getRow(i);
      RowWrapper rowB = f_workbookB.getRow(i);

      if (rowA != null && rowB == null) {
        for (int j = 0; j < rowA.getLastCellNum(); j++) {
          if (!rowA.hasNoText(j)) {
            sf_logger.warn("Change in Cell {}:\n[{}]\n[]", rowA.getAddress(j), rowA.getText(j));
          }
        }
      }
      else if (rowA == null && rowB != null) {
        for (int j = 0; j < rowB.getLastCellNum(); j++) {
          if (!rowB.hasNoText(j)) {
            sf_logger.warn("Change in Cell {}:\n[]\n[{}]", rowB.getAddress(j), rowB.getText(j));
          }
        }
      }
      else if (rowA != null && rowB != null) {
        int maxCellNum = Math.max(rowA.getLastCellNum(), rowB.getLastCellNum());

        for (int j = 0; j < maxCellNum; j++) {
          String cellA = rowA.getNullableText(j);
          String cellB = rowB.getNullableText(j);

          if (!Objects.equals(cellA, cellB) && !Objects.equals("p."+cellA, cellB)) {
            sf_logger.warn("Change in Cell {}:\n\t[{}]\n\t[{}]", rowA.getAddress(j), cellA, cellB);
          }
        }
      }
    }
  }
}
