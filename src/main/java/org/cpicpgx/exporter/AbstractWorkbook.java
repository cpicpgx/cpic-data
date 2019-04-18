package org.cpicpgx.exporter;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Shared workbook code that all exported workbooks can use.
 *
 * @author Ryan Whaley
 */
public abstract class AbstractWorkbook {

  private Workbook workbook;
  private CreationHelper createHelper;
  private List<SheetWrapper> sheets = new ArrayList<>();
  private CellStyle dateStyle;
  private CellStyle centerTextStyle;
  private CellStyle leftTextStyle;
  private CellStyle headerStyle;
  CellStyle wrapStyle;

  int colIdx = 1;

  AbstractWorkbook() {
    workbook = new XSSFWorkbook();
    createHelper = this.workbook.getCreationHelper();

    Font newFont = this.workbook.createFont();
    newFont.setFontHeightInPoints((short)12);
    
    this.dateStyle = this.workbook.createCellStyle();
    this.dateStyle.setDataFormat(
        createHelper.createDataFormat().getFormat("m/d/yy")
    );
    this.dateStyle.setAlignment(HorizontalAlignment.CENTER);
    this.dateStyle.setVerticalAlignment(VerticalAlignment.TOP);
    this.dateStyle.setFont(newFont);

    this.centerTextStyle = this.workbook.createCellStyle();
    this.centerTextStyle.setAlignment(HorizontalAlignment.CENTER);
    this.centerTextStyle.setVerticalAlignment(VerticalAlignment.TOP);
    this.centerTextStyle.setFont(newFont);
    
    this.leftTextStyle = this.workbook.createCellStyle();
    this.leftTextStyle.setAlignment(HorizontalAlignment.LEFT);
    this.leftTextStyle.setVerticalAlignment(VerticalAlignment.TOP);
    this.leftTextStyle.setFont(newFont);

    this.wrapStyle = this.workbook.createCellStyle();
    this.wrapStyle.setAlignment(HorizontalAlignment.LEFT);
    this.wrapStyle.setWrapText(true);
    this.wrapStyle.setFont(newFont);

    Font boldFont = this.workbook.createFont();
    boldFont.setFontHeightInPoints((short)14);
    boldFont.setBold(true);

    this.headerStyle = this.workbook.createCellStyle();
    this.headerStyle.setAlignment(HorizontalAlignment.CENTER);
    this.headerStyle.setFont(boldFont);
  }
  
  abstract String getFilename();

  protected Workbook getWorkbook() {
    return this.workbook;
  }
  
  List<SheetWrapper> getSheets() {
    return this.sheets;
  }
  
  SheetWrapper findSheet(String name) {
    if (name == null) return null;
    
    Optional<SheetWrapper> sheet = sheets.stream()
        .filter(s -> s.getName().equals("name"))
        .findFirst();
    
    if (sheet.isPresent()) {
      return sheet.get();
    } else {
      SheetWrapper sheetWrapper = new SheetWrapper(getWorkbook().createSheet(name), 0);
      this.sheets.add(sheetWrapper);
      return sheetWrapper;
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
    writeStringCell(row, colIdx, value, centered ? this.centerTextStyle : this.leftTextStyle);
  }
  
  void writeHeaderCell(Row row, int colIdx, String value) {
    writeStringCell(row, colIdx, value, headerStyle);
  }

  void writeStringCell(Row row, int colIdx, String value, CellStyle style) {
    Cell nameCell = row.createCell(colIdx);
    nameCell.setCellType(CellType.STRING);
    nameCell.setCellValue(StringUtils.strip(value));
    nameCell.setCellStyle(style);
  }

  void writeLinkCell(Row row, int colIdx, String text, String url) {
    if (StringUtils.isBlank(text)) {
      return;
    }
    
    Cell nameCell = row.createCell(colIdx);
    nameCell.setCellType(CellType.STRING);
    nameCell.setCellValue(StringUtils.strip(text));
    nameCell.setCellStyle(leftTextStyle);
    
    Hyperlink link = this.createHelper.createHyperlink(HyperlinkType.URL);
    link.setAddress(url);
    nameCell.setHyperlink(link);
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
