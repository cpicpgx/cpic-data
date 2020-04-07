package org.cpicpgx.exporter;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
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

  public static final String HISTORY_SHEET_NAME = "Change log";
  public static final String NOTES_SHEET_NAME = "Notes";
  private Workbook workbook;
  private CreationHelper createHelper;
  private List<SheetWrapper> sheets = new ArrayList<>();
  private CellStyle dateStyle;
  private CellStyle boldDateStyle;
  private CellStyle centerTextStyle;
  CellStyle leftTextStyle;
  private CellStyle rightNumberStyle;
  private CellStyle decimalNumberStyle;
  private CellStyle headerStyle;
  private CellStyle boldStyle;
  private CellStyle topBorderStyle;
  private CellStyle highlightStyle;
  private CellStyle highlightDoubleStyle;
  private CellStyle noteStyle;
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

    this.noteStyle = this.workbook.createCellStyle();
    this.noteStyle.setAlignment(HorizontalAlignment.LEFT);
    this.noteStyle.setVerticalAlignment(VerticalAlignment.TOP);
    this.noteStyle.setWrapText(true);
    this.noteStyle.setFont(newFont);

    this.rightNumberStyle = this.workbook.createCellStyle();
    this.rightNumberStyle.setAlignment(HorizontalAlignment.RIGHT);
    this.rightNumberStyle.setVerticalAlignment(VerticalAlignment.TOP);
    this.rightNumberStyle.setFont(newFont);
    
    this.decimalNumberStyle = this.workbook.createCellStyle();
    this.decimalNumberStyle.setDataFormat(this.workbook.createDataFormat().getFormat("0.0000"));
    this.decimalNumberStyle.setAlignment(HorizontalAlignment.RIGHT);
    this.decimalNumberStyle.setVerticalAlignment(VerticalAlignment.TOP);
    this.decimalNumberStyle.setFont(newFont);

    this.wrapStyle = this.workbook.createCellStyle();
    this.wrapStyle.setAlignment(HorizontalAlignment.LEFT);
    this.wrapStyle.setWrapText(true);
    this.wrapStyle.setFont(newFont);

    Font boldFont = this.workbook.createFont();
    boldFont.setFontHeightInPoints((short)14);
    boldFont.setBold(true);
    
    this.boldStyle = this.workbook.createCellStyle();
    this.boldStyle.setFont(boldFont);

    this.headerStyle = this.workbook.createCellStyle();
    this.headerStyle.setAlignment(HorizontalAlignment.CENTER);
    this.headerStyle.setFont(boldFont);

    this.boldDateStyle = this.workbook.createCellStyle();
    this.boldDateStyle.setDataFormat(
        createHelper.createDataFormat().getFormat("m/d/yy")
    );
    this.boldDateStyle.setAlignment(HorizontalAlignment.CENTER);
    this.boldDateStyle.setVerticalAlignment(VerticalAlignment.TOP);
    this.boldDateStyle.setFont(boldFont);
    
    this.topBorderStyle = this.workbook.createCellStyle();
    this.topBorderStyle.setBorderTop(BorderStyle.THIN);
    
    this.highlightStyle = this.workbook.createCellStyle();
    this.highlightStyle.setAlignment(HorizontalAlignment.CENTER);
    this.highlightStyle.setVerticalAlignment(VerticalAlignment.TOP);
    this.highlightStyle.setFont(newFont);
    this.highlightStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.index);
    this.highlightStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    this.highlightStyle.setBorderTop(BorderStyle.HAIR);
    this.highlightStyle.setBorderBottom(BorderStyle.HAIR);
    this.highlightStyle.setBorderLeft(BorderStyle.HAIR);
    this.highlightStyle.setBorderRight(BorderStyle.HAIR);

    this.highlightDoubleStyle = this.workbook.createCellStyle();
    this.highlightDoubleStyle.setDataFormat(this.workbook.createDataFormat().getFormat("0.0000"));
    this.highlightDoubleStyle.setAlignment(HorizontalAlignment.RIGHT);
    this.highlightDoubleStyle.setVerticalAlignment(VerticalAlignment.TOP);
    this.highlightDoubleStyle.setFont(newFont);
    this.highlightDoubleStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.index);
    this.highlightDoubleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
    this.highlightDoubleStyle.setBorderTop(BorderStyle.HAIR);
    this.highlightDoubleStyle.setBorderBottom(BorderStyle.HAIR);
    this.highlightDoubleStyle.setBorderLeft(BorderStyle.HAIR);
    this.highlightDoubleStyle.setBorderRight(BorderStyle.HAIR);
  }
  
  abstract String getFilename();

  private Workbook getWorkbook() {
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

  void writeBoldDateCell(Row row, Date value) {
    writeDateCell(row, value, this.boldDateStyle);
  }
  
  void writeDateCell(Row row, int idx, Date value) {
    Cell nameCell = row.createCell(idx);
    nameCell.setCellStyle(this.dateStyle);
    nameCell.setCellValue(value);
  }

  private void writeDateCell(Row row, Date value, CellStyle style) {
    Cell nameCell = row.createCell(1);
    nameCell.setCellStyle(this.dateStyle);
    nameCell.setCellValue(value);
    if (style != null) {
      nameCell.setCellStyle(style);
    }
  }

  void writeStringCell(Row row, int colIdx, String value) {
    writeStringCell(row, colIdx, value, true);
  }

  void writeStringCell(Row row, int colIdx, String value, boolean centered) {
    writeStringCell(row, colIdx, value, centered ? this.centerTextStyle : this.leftTextStyle);
  }
  
  void writeBoldStringCell(Row row, int colIdx, String value) {
    writeStringCell(row, colIdx, value, this.boldStyle);
  }
  
  void writeHeaderCell(Row row, int colIdx, String value) {
    writeStringCell(row, colIdx, value, headerStyle);
  }
  
  void writeTopBorderCell(Row row, int colIdx, String value) {
    writeStringCell(row, colIdx, value, this.topBorderStyle);
  }

  void writeStringCell(Row row, int colIdx, String value, CellStyle style) {
    Cell nameCell = row.createCell(colIdx);
    nameCell.setCellValue(StringUtils.strip(value));
    nameCell.setCellStyle(style);
  }

  void writeMergedNoteCell(Row row, String value, int lastCol) {
    Cell noteCell = row.createCell(0);
    noteCell.setCellValue(StringUtils.strip(value));
    noteCell.setCellStyle(noteStyle);
    row.getSheet().addMergedRegion(new CellRangeAddress(row.getRowNum(), row.getRowNum(), 0, lastCol));
  }
  
  void writeNoteCell(Row row, String value) {
    Cell noteCell = row.createCell(0);
    noteCell.setCellValue(StringUtils.strip(value));
    noteCell.setCellStyle(noteStyle);
  }

  void writeIntegerCell(Row row, int colIdx, int value) {
    Cell cell = row.createCell(colIdx);
    cell.setCellValue(value);
    cell.setCellStyle(this.rightNumberStyle);
  }

  void writeDoubleCell(Row row, int colIdx, Double value) {
    if (value != null) {
      Cell cell = row.createCell(colIdx);
      cell.setCellValue(value);
      cell.setCellStyle(this.decimalNumberStyle);
    }
  }

  void writeHighlightCell(Row row, int colIdx, String value) {
    writeStringCell(row, colIdx, value, this.highlightStyle);
  }

  void writeHighlightCell(Row row, int colIdx, Double value) {
    if (value != null) {
      Cell cell = row.createCell(colIdx);
      cell.setCellValue(value);
      cell.setCellStyle(this.highlightDoubleStyle);
    }
  }

  void writeLinkCell(Row row, int colIdx, String text, String url) {
    if (StringUtils.isBlank(text)) {
      return;
    }
    
    Cell nameCell = row.createCell(colIdx);
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

  /**
   * Create the "Notes" tab and populate it with note lines
   * @param notes a List of notes to write, one per line
   */
  void writeNotes(List<String> notes) {
    SheetWrapper sheet = findSheet(NOTES_SHEET_NAME);

    Row headerRow = sheet.nextRow();
    writeHeaderCell(headerRow, 0, NOTES_SHEET_NAME);

    for (String note : notes) {
      Row noteRow = sheet.nextRow();
      noteRow.setRowStyle(noteStyle);
      writeNoteCell(noteRow, note);
      noteRow.setHeight((short)-1);
    }

    sheet.sheet.setColumnWidth(0, 100 * 256);
  }
}
