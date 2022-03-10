package org.cpicpgx.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.cpicpgx.exception.NotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidParameterException;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Wrapper class for an Excel workbook. Helps read and setup supporting objects.
 * 
 * During construction, this will set the <code>currentSheet</code> to the first sheet in the workbook.
 *
 * @author Ryan Whaley
 */
public class WorkbookWrapper {
  public static final String NOTES_SHEET_NAME = "Notes";

  private final Workbook workbook;
  private final FormulaEvaluator formulaEvaluator;
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
      throw new InvalidParameterException("No sheet with given name found [" + name + "]");
    }
    
    this.currentSheet = nextSheet;
  }

  /**
   * Set the current sheet by finding a match to the supplied Regex pattern. If more than one matchis found then a
   * {@link RuntimeException} is thrown. If no matchis found then a {@link NotFoundException} is thrown
   * @param sheetNamePattern a Regex pattern to match the sheet name
   * @throws NotFoundException if no match is found for the given pattern
   */
  public void findSheet(Pattern sheetNamePattern) throws NotFoundException {
    Sheet foundSheet = null;
    for (Iterator<Sheet> sheetIterator = this.workbook.sheetIterator(); sheetIterator.hasNext();) {
      Sheet sheet = sheetIterator.next();
      if (sheetNamePattern.matcher(StringUtils.strip(sheet.getSheetName())).matches()) {
        if (foundSheet == null) {
          foundSheet = sheet;
        } else {
          throw new RuntimeException("Multiple sheets found for " + sheetNamePattern.toString());
        }
      }
    }
    if (foundSheet == null) {
      throw new NotFoundException("Could not find sheet matching pattern " + sheetNamePattern.toString());
    } else {
      this.currentSheet = foundSheet;
    }
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

  public List<String> getSheetNameList() {
    List<String> sheetNames = new ArrayList<>();
    for (int i = 0; i < this.workbook.getNumberOfSheets(); i++) {
      sheetNames.add(this.workbook.getSheetName(i));
    }
    return sheetNames;
  }

  public void write(OutputStream out) throws IOException {
    this.workbook.write(out);
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
      RowWrapper row = getRow(i);
      if (row == null) {
        continue;
      }
      String note = row.getNullableText(0);
      if (note == null) {
        continue;
      }
      notes.add(note);
    }
    return notes;
  }
}
