package org.cpicpgx.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidParameterException;

/**
 * Wrapper class for an Excel workbook. Helps read and setup supporting objects
 *
 * @author Ryan Whaley
 */
public class WorkbookWrapper {
  
  private Workbook workbook;
  private FormulaEvaluator formulaEvaluator;
  public Sheet currentSheet;

  /**
   * Constructor
   * @param in an {@link InputStream} for the excel file
   * @throws IOException can occur when reading the file
   * @throws InvalidFormatException can occur if the file is not an excel file
   */
  public WorkbookWrapper(InputStream in) throws IOException, InvalidFormatException {
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
}
