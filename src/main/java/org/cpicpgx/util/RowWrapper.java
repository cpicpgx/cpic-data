package org.cpicpgx.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellValue;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for handling {@link Row} objects
 *
 * @author Ryan Whaley
 */
public class RowWrapper {
  private static final Pattern NUMBER_PATTERN = Pattern.compile("[\\d\\.]+");
  
  public Row row;
  private FormulaEvaluator formulaEvaluator;

  /**
   * Constructor. Requires a formula evaluator so we can get a value out of Formula columns
   * @param row a {@link Row}, possibly null
   * @param formulaEvaluator a {@link FormulaEvaluator} that comes from the containing workbook
   */
  RowWrapper(Row row, FormulaEvaluator formulaEvaluator) {
    this.row = row;
    this.formulaEvaluator = formulaEvaluator;
  }

  /**
   * Tests if the cell at the given index has text in it
   * @param cellIdx the index of a cell in this row
   * @return true if the cell is non-blank (only whitespace counts as blank), false otherwise
   */
  public boolean hasTextIn(int cellIdx) {
    return this.row != null 
        && getNullableText(cellIdx) != null;
  }

  /**
   * Gets text value from the cell at the given index. This will convert non-STRING columns to text. This will also 
   * strip text down to null values. This will give the words True/False for a BOOLEAN cell type.
   * @param cellIdx the index of a cell in this row
   * @return a {@link String} representation of the value in the cell at the given index
   */
  public String getNullableText(int cellIdx) {
    if (cellIdx < 0) {
      throw new RuntimeException("Bad cell index, must be >= 0");
    }
    
    Cell cell = this.row.getCell(cellIdx);
    if (cell == null) return null;
    
    switch (cell.getCellTypeEnum()) {
      case STRING:
        return StringUtils.stripToNull(cell.getStringCellValue());
      case NUMERIC:
        return String.valueOf(cell.getNumericCellValue());
      case BOOLEAN:
        return cell.getBooleanCellValue() ? "True" : "False";
      case BLANK:
        return null;
      case FORMULA:
        CellValue cellValue = formulaEvaluator.evaluate(cell);
        switch (cellValue.getCellTypeEnum()) {
          case STRING:
            return StringUtils.stripToNull(cellValue.getStringValue());
          case NUMERIC:
            return String.valueOf(cellValue.getNumberValue());
          case BOOLEAN:
            return cellValue.getBooleanValue() ? "True" : "False";
          default:
            throw new RuntimeException(cell.getAddress() + " Type not supported " + cellValue.getCellTypeEnum());
        }
      default:
        throw new RuntimeException(cell.getAddress() + " Type not supported " + cell.getCellTypeEnum());
        
    }
  }

  /**
   * Gets long value from the cell at the given index. This will try to convert STRING columns to long. This will also 
   * strip text down to just numerical content for conversion to long. Non-supported types will throw an exception.
   * @param cellIdx the index of a cell in this row
   * @return a {@link Long} representation of the value in the cell at the given index
   */
  public Long getNullableLong(int cellIdx) {
    if (cellIdx < 0) {
      throw new RuntimeException("Bad cell index, must be >= 0");
    }
    
    Cell cell = this.row.getCell(cellIdx);
    if (cell == null) return null;
    
    switch (cell.getCellTypeEnum()) {
      case NUMERIC:
        return Math.round(cell.getNumericCellValue());
      case BLANK:
        return null;
      case STRING:
        Matcher m = NUMBER_PATTERN.matcher(cell.getStringCellValue());
        if (m.find()) {
          return Long.valueOf(m.group());
        } else {
          throw new RuntimeException(cell.getAddress() + " No number found in string cell");
        }
      case FORMULA:
        CellValue cellValue = formulaEvaluator.evaluate(cell);
        switch (cell.getCellTypeEnum()) {
          case NUMERIC:
            return Math.round(cellValue.getNumberValue());
          default:
            throw new RuntimeException(cell.getAddress() + " Type not supported " + cellValue.getCellTypeEnum());
        }
      default:
        throw new RuntimeException(cell.getAddress() + " Type not supported " + cell.getCellTypeEnum());
    }
  }

  /**
   * Gets double value from the cell at the given index. This will NOT try to convert STRING columns. Only NUMBER and 
   * FORMULA columns that evaluate to numbers are supported. Non-supported types will return null.
   * @param cellIdx the index of a cell in this row
   * @return a {@link Double} representation of the value in the cell at the given index, or null
   */
  public Double getNullableDouble(int cellIdx) {
    if (cellIdx < 0) {
      throw new RuntimeException("Bad cell index, must be >= 0");
    }
    
    Cell cell = this.row.getCell(cellIdx);
    if (cell == null) return null;
    
    switch (cell.getCellTypeEnum()) {
      case NUMERIC:
        return cell.getNumericCellValue();
      case FORMULA:
        CellValue cellValue = formulaEvaluator.evaluate(cell);
        switch (cell.getCellTypeEnum()) {
          case NUMERIC:
            return cellValue.getNumberValue();
          default:
            return null;
        }
      default:
        return null;
        
    }
  }
}
