package org.cpicpgx.exporter;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;

/**
 * Wrapper class to track state while writing to an Excel sheet
 *
 * @author Ryan Whaley
 */
public class SheetWrapper {
  public Sheet sheet;
  private Integer[] widths;
  private int colCount;
  private int rowIdx = 0;
  
  SheetWrapper(Sheet sheet, int colCount) {
    this.sheet = sheet;
    setColCount(colCount);
  }
  
  public String getName() {
    return this.sheet.getSheetName();
  }

  /**
   * Set the number of columns to expect in this sheet
   * @param colCount the number of columns in this sheet
   */
  void setColCount(int colCount) {
    this.colCount = colCount;
  }

  /**
   * Set the widths of the columns in this sheet. Each element of this array will be a width of a column. The width is 
   * using the same measurements as POI, in 1/256's of a point. This will override any autosizing that's done
   * @param widths an array of integer widths
   */
  void setWidths(Integer[] widths) {
    this.widths = widths;
  }

  /**
   * Create a new row and return it. Will start with the first row then return the immediate next row when called next.
   * @return a newly created Row
   */
  Row nextRow() {
    return this.sheet.createRow(this.rowIdx++);
  }

  /**
   * Automatically set the width of the columns to display the contents or follow the widths specified in setWidths
   */
  void autosizeColumns() {
    if (widths == null) {
      for (int i = 0; i < this.colCount; i++) {
        sheet.autoSizeColumn(i);
      }
    } else {
      for (int i = 0; i < this.colCount; i++) {
        if (widths.length > i && widths[i] != null) {
          sheet.setColumnWidth(i, widths[i]);
        } else {
          sheet.autoSizeColumn(i);
        }
      }
    }
  }
}
