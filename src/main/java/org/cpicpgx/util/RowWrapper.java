package org.cpicpgx.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellAddress;
import org.apache.poi.xssf.usermodel.XSSFRichTextString;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Helper class for handling {@link Row} objects
 *
 * @author Ryan Whaley
 */
public class RowWrapper {
  private static final Pattern NUMBER_PATTERN = Pattern.compile("[\\d\\.]+");
  private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM/dd/yy");
  
  public Row row;
  private final FormulaEvaluator formulaEvaluator;

  /**
   * Constructor. Requires a formula evaluator so we can get a value out of Formula columns
   * @param row a {@link Row}, possibly null
   * @param formulaEvaluator a {@link FormulaEvaluator} that comes from the containing workbook
   */
  RowWrapper(Row row, FormulaEvaluator formulaEvaluator) {
    this.row = row;
    this.formulaEvaluator = formulaEvaluator;
  }

  public short getLastCellNum() {
    if (this.row == null) {
      return 0;
    }
    return this.row.getLastCellNum();
  }

  public CellAddress getAddress(int cellIdx) {
    Cell cell = this.row.getCell(cellIdx);
    if (cell == null) {
      return null;
    } else
      return cell.getAddress();
  }

  /**
   * Tests if the cell at the given index has text in it
   * @param cellIdx the index of a cell in this row, 0-based
   * @return true if the cell is blank (only whitespace counts as blank), false otherwise
   */
  public boolean hasNoText(int cellIdx) {
    return this.row == null || getNullableText(cellIdx) == null;
  }

  /**
   * Gets text value from the cell at the given index. This will convert non-STRING columns to text. This will also 
   * strip text down to null values. This will give the words True/False for a BOOLEAN cell type.
   * @param cellIdx the index of a cell in this row, 0-based
   * @return a {@link String} representation of the value in the cell at the given index
   */
  public String getNullableText(int cellIdx) {
    return getNullableText(cellIdx, false);
  }

  /**
   * Gets text value from the cell and will throw an exception if no value is found.
   * @param cellIdx the index of a cell in this row, 0-based
   * @return a {@link String} representation of the value in the cell at the given index
   */
  public String getText(int cellIdx) {
    String text = getNullableText(cellIdx, false);
    if (text != null) {
      return text;
    } else {
      Cell cell = this.row.getCell(cellIdx);
      if (cell == null) {
        cell = this.row.createCell(cellIdx);
      }
      throw new RuntimeException("Found unexpected null value at " + cell.getAddress());
    }
  }

  /**
   * Gets text value from the cell at the given index. This will convert non-STRING columns to text. This will also 
   * strip text down to null values. This will give the words True/False for a BOOLEAN cell type.
   * @param cellIdx the index of a cell in this row, 0-based
   * @param roundNumerics if true numbers will be rounded and returned as an integer
   * @return a {@link String} representation of the value in the cell at the given index
   */
  public String getNullableText(int cellIdx, boolean roundNumerics) {
    if (cellIdx < 0) {
      throw new RuntimeException("Bad cell index, must be >= 0");
    }

    if (this.row == null) {
      return null;
    }
    
    Cell cell = this.row.getCell(cellIdx);
    if (cell == null) return null;
    
    switch (cell.getCellType()) {
      case STRING:
        return TextUtils.normalize(stripFootnote(cellIdx));
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          return DATE_FORMAT.format(cell.getDateCellValue());
        } else {
          double numVal = cell.getNumericCellValue();
          if (!roundNumerics) {
            return String.valueOf(numVal);
          } else {
            return String.valueOf(Math.round(numVal));
          }
        }
      case BOOLEAN:
        return cell.getBooleanCellValue() ? "True" : "False";
      case BLANK:
        return null;
      case FORMULA:
        CellValue cellValue = formulaEvaluator.evaluate(cell);
        switch (cellValue.getCellType()) {
          case STRING:
            return TextUtils.normalize(cellValue.getStringValue());
          case NUMERIC:
            if (DateUtil.isCellDateFormatted(cell)) {
              return cellValue.getStringValue();
            } else {
              if (roundNumerics) {
                return String.valueOf(Math.round(cellValue.getNumberValue()));
              } else {
                return String.valueOf(cellValue.getNumberValue());
              }
            }
          case BOOLEAN:
            return cellValue.getBooleanValue() ? "True" : "False";
          default:
            throw new RuntimeException(cell.getAddress() + " Type not supported " + cellValue.getCellType());
        }
      default:
        throw new RuntimeException(cell.getAddress() + " Type not supported " + cell.getCellType());
        
    }
  }

  /**
   * Parses PubMed IDs out of a cell at the given index. Will split semicolon-separated PMID lists and automatically
   * convert PMIDs that are stored as NUMERICs for some reason.
   * @param cellIdx the index of the cell to get PMIDs from
   * @return a nullable String array of PMIDs found in the given cell
   */
  public String[] getNullablePmids(int cellIdx) {
    if (cellIdx < 0) {
      throw new RuntimeException("Bad cell index, must be >= 0");
    }

    Cell cell = this.row.getCell(cellIdx);
    if (cell == null) return null;

    switch (cell.getCellType()) {
      case BLANK:
        return null;
      case STRING:
        if (StringUtils.isBlank(cell.getStringCellValue())) {
          return null;
        } else {
          return cell.getStringCellValue().split(";");
        }
      case NUMERIC:
        if (DateUtil.isCellDateFormatted(cell)) {
          throw new RuntimeException(cell.getAddress() + " Type not supported " + cell.getCellType());
        } else {
          return new String[]{String.valueOf(Math.round(cell.getNumericCellValue()))};
        }
      default:
        throw new RuntimeException(cell.getAddress() + " Type not supported " + cell.getCellType());

    }
  }

  /**
   * Gets long value from the cell at the given index. This will try to convert STRING columns to long. This will also 
   * strip text down to just numerical content for conversion to long. Non-supported types will throw an exception.
   * 
   * String cells with absolutely no numerical characters in them (e.g. "var") will return 0.
   * 
   * @param cellIdx the index of a cell in this row
   * @return a {@link Long} representation of the value in the cell at the given index
   * @throws NumberFormatException when a cell value can't be translated into a number
   */
  public Long getNullableLong(int cellIdx) {
    if (cellIdx < 0) {
      throw new RuntimeException("Bad cell index, must be >= 0");
    }
    
    Cell cell = this.row.getCell(cellIdx);
    if (cell == null) return null;
    
    switch (cell.getCellType()) {
      case NUMERIC:
        return Math.round(cell.getNumericCellValue());
      case BLANK:
        return 0L;
      case STRING:
        Matcher m = NUMBER_PATTERN.matcher(cell.getStringCellValue());
        if (m.find()) {
          return Long.valueOf(m.group());
        } else {
          throw new NumberFormatException("[" + cell.getStringCellValue() + "] is not a valid number in cell " + cell.getAddress());
        }
      case FORMULA:
        CellValue cellValue = formulaEvaluator.evaluate(cell);
        if (cellValue.getCellType() == CellType.NUMERIC) {
          return Math.round(cellValue.getNumberValue());
        }
        throw new RuntimeException(cell.getAddress() + " Type not supported " + cellValue.getCellType());
      default:
        throw new RuntimeException(cell.getAddress() + " Type not supported " + cell.getCellType());
    }
  }

  /**
   * Gets double value from the cell at the given index. This will try to convert STRING columns. This will evaluate
   * FORMULA columns and fail if they don't evaluate to NUMERICs. Non-supported types will return null.
   * @param cellIdx the index of a cell in this row, 0-based
   * @return a {@link Double} representation of the value in the cell at the given index, or null
   */
  public Double getNullableDouble(int cellIdx) {
    if (cellIdx < 0) {
      throw new RuntimeException("Bad cell index, must be >= 0");
    }
    
    Cell cell = this.row.getCell(cellIdx);
    if (cell == null) return null;
    
    switch (cell.getCellType()) {
      case NUMERIC:
        return cell.getNumericCellValue();
      case STRING:
        if (StringUtils.isBlank(cell.getStringCellValue())) {
          return null;
        }
        try {
          return Double.valueOf(cell.getStringCellValue());
        } catch (NumberFormatException e) {
          throw new RuntimeException("Field is not a number " + cell.getAddress() + " [" + cell.getStringCellValue() + "]", e);
        }
      case FORMULA:
        CellValue cellValue = formulaEvaluator.evaluate(cell);
        if (cellValue.getCellType() == CellType.NUMERIC) {
          return cellValue.getNumberValue();
        } else if (cellValue.getCellType() == CellType.STRING) {
          Matcher m = NUMBER_PATTERN.matcher(cellValue.getStringValue());
          if (m.find()) {
            return Double.valueOf(m.group());
          } else {
            throw new NumberFormatException("[" + cellValue.getStringValue() + "] is not a valid number in cell " + cell.getAddress());
          }
        } else {
          throw new NumberFormatException("not a valid number in cell " + cell.getAddress());
        }
      default:
        return null;
        
    }
  }

  /**
   * Gets a date value from the cell at the given cell index. Returns null if no cell exists at index. Will throw an 
   * exception if the cell format is not Date.
   * @param cellIdx the index of the cell to get a value for
   * @return a Date value represented in the cell
   * @throws RuntimeException if the cell is not date-formatted
   */
  public Date getNullableDate(int cellIdx) {
    Cell cell = this.row.getCell(cellIdx);
    if (cell == null) return null;

    if (cell.getCellType() == CellType.BLANK) {
      return null;
    }

    if (!DateUtil.isCellDateFormatted(cell)) {
      throw new RuntimeException("Cell is not date formatted " + cell.getAddress());
    }
    
    return cell.getDateCellValue();
  }

  /**
   * Gets a date value from the cell at the given cell index and will throw a {@link RuntimeException} if the value is
   * not present. Can also throw an exception if the content is not a Date
   * @param cellIdx the index of the cell to the value for
   * @return a Date value represented in the cell
   * @throws RuntimeException if the cell is not date-formatted or the cell is blank
   */
  public Date getDate(int cellIdx) {
    Date date = getNullableDate(cellIdx);
    if (date == null) {
      Cell cell = this.row.getCell(cellIdx);
      throw new RuntimeException("No date in " + cell.getAddress());
    }
    return date;
  }

  /**
   * Strip any whitespace from the given cell index and also any superscripted footnotes
   * @param cellIdx the index of the cell to strip
   * @return a String of the cell contents without any superscripts at the end, null if empty
   */
  String stripFootnote(int cellIdx) {
    if (row == null || row.getCell(cellIdx) == null) return null;
    
    RichTextString rts = row.getCell(cellIdx).getRichStringCellValue();
    int rtsLength = rts.length();
    if (rts instanceof XSSFRichTextString) {
      String superScript = getSuperScript((XSSFRichTextString)rts);
      if (StringUtils.isNotBlank(superScript)) {
        return StringUtils.stripToNull(StringUtils.substring(row.getCell(cellIdx).getStringCellValue(), 0, rtsLength-1));
      }
    }
    return StringUtils.stripToNull(row.getCell(cellIdx).getStringCellValue());
  }

  /**
   * Parses out any superscript and returns that
   * @param cellIdx the index of the cell to parse
   * @return a String of the superscript of this cell, null if empty
   */
  String getFootnote(int cellIdx) {
    if (row == null || row.getCell(cellIdx) == null) return null;
    
    RichTextString rts = row.getCell(cellIdx).getRichStringCellValue();
    if (rts instanceof XSSFRichTextString) {
      String superScript = getSuperScript((XSSFRichTextString)rts);
      if (StringUtils.isNotBlank(superScript)) {
        return superScript;
      }
    }
    return null;
  }

  /**
   * Reads the superscript String from a {@link RichTextString} object. This only applies to XSSF Cells
   * @param cellContent a {@link XSSFRichTextString} representation of cell contents
   * @return the superscript text, null if none exists
   */
  private static String getSuperScript(XSSFRichTextString cellContent) {
    int lenSuper = cellContent.getLengthOfFormattingRun(cellContent.numFormattingRuns() - 1);
    int iSuper = cellContent.getIndexOfFormattingRun(cellContent.numFormattingRuns() - 1);
    if (lenSuper < 0 || iSuper != cellContent.length() - 1) return null;
    return cellContent.toString().substring(iSuper, lenSuper + iSuper);
  }
}
