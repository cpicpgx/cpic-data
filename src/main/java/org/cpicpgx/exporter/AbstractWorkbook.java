package org.cpicpgx.exporter;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Date;

/**
 * Shared workbook code that all exported workbooks can use.
 *
 * @author Ryan Whaley
 */
public abstract class AbstractWorkbook {

  private Workbook workbook;
  private CellStyle dateStyle;
  private CellStyle centerTextStyle;
  private CellStyle leftTextStyle;
  private CellStyle headerStyle;

  int rowIdx;
  int colIdx = 1;

  AbstractWorkbook() {
    workbook = new XSSFWorkbook();

    CreationHelper createHelper = this.workbook.getCreationHelper();

    Font newFont = this.workbook.createFont();
    newFont.setFontHeightInPoints((short)12);
    
    this.dateStyle = this.workbook.createCellStyle();
    this.dateStyle.setDataFormat(
        createHelper.createDataFormat().getFormat("m/d/yy")
    );
    this.dateStyle.setAlignment(HorizontalAlignment.CENTER);
    this.dateStyle.setFont(newFont);

    this.centerTextStyle = this.workbook.createCellStyle();
    this.centerTextStyle.setAlignment(HorizontalAlignment.CENTER);
    this.centerTextStyle.setFont(newFont);
    
    this.leftTextStyle = this.workbook.createCellStyle();
    this.leftTextStyle.setAlignment(HorizontalAlignment.LEFT);
    this.leftTextStyle.setFont(newFont);

    Font boldFont = this.workbook.createFont();
    boldFont.setFontHeightInPoints((short)14);
    boldFont.setBold(true);

    this.headerStyle = this.workbook.createCellStyle();
    this.headerStyle.setAlignment(HorizontalAlignment.CENTER);
    this.headerStyle.setFont(boldFont);
  }
  
  abstract String getFilename();
  abstract String getSheetName();
  
  protected Workbook getWorkbook() {
    return this.workbook;
  }
  
  Sheet getSheet(String name) {
    if (name == null) return null;

    Sheet sheet = getWorkbook().getSheet(name);
    if (sheet == null) {
      return getWorkbook().createSheet(name);
    } else {
      return sheet;
    }
  }

  void autosizeColumns() {
    Sheet sheet = getSheet(getSheetName());
    sheet.setColumnWidth(0, 14*256);
    for (int i=1; i <= this.colIdx; i++) {
      sheet.autoSizeColumn(i);
    }
  }

  void writeDateCell(Row row, Date value) {
    Cell nameCell = row.createCell(1);
    nameCell.setCellStyle(this.dateStyle);
    nameCell.setCellValue(value);
  }

  void writeStringCell(Row row, int colIdx, String value) {
    writeStringCell(row, colIdx, value, true);
  }

  void writeStringCell(Row row, int colIdx, String value, boolean centered) {
    Cell nameCell = row.createCell(colIdx);
    nameCell.setCellType(CellType.STRING);
    nameCell.setCellValue(StringUtils.strip(value));
    nameCell.setCellStyle(centered ? this.centerTextStyle : this.leftTextStyle);
  }
  
  void writeHeaderCell(Row row, int colIdx, String value) {
    Cell headerCell = row.createCell(colIdx);
    headerCell.setCellType(CellType.STRING);
    headerCell.setCellValue(StringUtils.strip(value));
    headerCell.setCellStyle(this.headerStyle);
  }

  /**
   * Wrapper around the default POI write method
   * @param out an initialized {@link OutputStream}
   * @throws IOException can occur when writing the workbook
   */
  void write(OutputStream out) throws IOException {
    getWorkbook().write(out);
  }
}
