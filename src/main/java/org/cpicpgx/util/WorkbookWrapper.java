package org.cpicpgx.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Wrapper class for an Excel workbook. Helps read and setup supporting objects.
 * 
 * During construction, this will set the <code>currentSheet</code> to the first sheet in the workbook.
 *
 * @author Ryan Whaley
 */
public class WorkbookWrapper {
  public static final String NOTES_SHEET_NAME = "Notes";

  private Workbook workbook;
  private FormulaEvaluator formulaEvaluator;
  private String fileName = null;
  public Sheet currentSheet;

  /**
   * Constructor
   * @param in an {@link InputStream} for the excel file
   * @throws IOException can occur when reading the file
   */
  public WorkbookWrapper(InputStream in) throws IOException {
    if (in == null) {
      throw new InvalidParameterException("No valid input stream specified");
    }
    this.workbook = WorkbookFactory.create(in);
    this.formulaEvaluator = workbook.getCreationHelper().createFormulaEvaluator();
    this.currentSheet = workbook.getSheetAt(0);
  }

  /**
   * Set the current sheet by sheet name. The current sheet can be accessed from {@link WorkbookWrapper#currentSheet}.
   * @param name the name of the sheet
   * @throws InvalidParameterException when there is no sheet with the given name or when no sheet name is specified
   */
  public void currentSheetIs(String name) {
    if (StringUtils.isBlank(name)) {
      throw new InvalidParameterException("No sheet name specified");
    }
    
    Sheet nextSheet = this.workbook.getSheet(name);
    if (nextSheet == null) {
      throw new InvalidParameterException("No sheet with given name found");
    }
    
    this.currentSheet = nextSheet;
  }

  /**
   * Get a {@link RowWrapper} for the row at the given index, 0-based
   * @param i the index of the row to get, 0-based
   * @return a row of data, null if past last row in sheet
   */
  public RowWrapper getRow(int i) {
    if (i < 0) {
      throw new InvalidParameterException("Invalid row index " + i);
    }
    if (i > this.currentSheet.getLastRowNum()) {
      return null;
    }
    return new RowWrapper(this.currentSheet.getRow(i), this.formulaEvaluator);
  }
  
  public String toString() {
    return this.workbook.toString();
  }

  /**
   * Switch to a sheet at the given index
   * @param i index of a sheet in this workbook, 0-based
   */
  public void switchToSheet(int i) {
    int n = this.workbook.getNumberOfSheets();
    
    if (i+1 <= n) {
      this.currentSheet = this.workbook.getSheetAt(i);
    } else {
      this.currentSheet = null;
    }
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public Iterator<Sheet> getSheetIterator() {
    return this.workbook.sheetIterator();
  }

  /**
   * Reads all the notes in the workbook. This will look for a sheet named {@link WorkbookWrapper#NOTES_SHEET_NAME}. It
   * will assume the first row is a title row and read the first non-empty cell of every subsequent row.
   * @return a List of note strings
   */
  public List<String> getNotes() {
    try {
      currentSheetIs(NOTES_SHEET_NAME);
    } catch (InvalidParameterException ex) {
      return Collections.emptyList();
    }

    List<String> notes = new ArrayList<>();
    // intentionally skip first header row
    for (int i = 1; i <= currentSheet.getLastRowNum(); i++) {
      Row row = currentSheet.getRow(i);
      if (row == null) {
        continue;
      }
      Cell cell = row.getCell(0);
      if (cell == null || cell.getStringCellValue().length() == 0) {
        continue;
      }
      notes.add(StringUtils.strip(row.getCell(0).toString()));
    }
    return notes;
  }
}
