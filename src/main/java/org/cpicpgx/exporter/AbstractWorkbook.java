package org.cpicpgx.exporter;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.regex.Pattern;

/**
 * Shared workbook code that all exported workbooks can use.
 *
 * @author Ryan Whaley
 */
public abstract class AbstractWorkbook {

  public static final String HISTORY_SHEET_NAME = "Change log";
  public static final String NOTES_SHEET_NAME = "Notes";
  public static final String LOG_FILE_CREATED = "File generated and data queried";
  public static final String METHODS_SHEET_NAME = "Methods";
  public static final Pattern METHODS_SHEET_PATTERN = Pattern.compile("^[Mm]ethods( and [Cc]aveats)?$");
  private final Workbook workbook;
  private final CreationHelper createHelper;
  private final List<SheetWrapper> sheets = new ArrayList<>();
  private final CellStyle dateStyle;
  private final CellStyle centerTextStyle;
  CellStyle leftTextStyle;
  CellStyle rightNumberStyle;
  private final CellStyle decimalNumberStyle;
  private final CellStyle headerStyle;
  private final CellStyle boldStyle;
  private final CellStyle topBorderStyle;
  private final CellStyle highlightStyle;
  private final CellStyle highlightDoubleStyle;
  private final CellStyle noteStyle;
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
    this.dateStyle.setAlignment(HorizontalAlignment.LEFT);
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

  /**
   * Finds a {@link SheetWrapper} with the given name. If the sheet doesn't exist, it will be created with the given
   * name.
   * @param name the name of the sheet
   * @return a {@link SheetWrapper} instance
   */
  SheetWrapper findSheet(String name) {
    if (name == null) return null;
    
    Optional<SheetWrapper> sheet = sheets.stream()
        .filter(s -> s.getName().equalsIgnoreCase(name))
        .findFirst();
    
    if (sheet.isPresent()) {
      return sheet.get();
    } else {
      SheetWrapper sheetWrapper = new SheetWrapper(getWorkbook().createSheet(name), 0);
      this.sheets.add(sheetWrapper);
      return sheetWrapper;
    }
  }

  void writeDateCell(Row row, int idx, Date value) {
    Cell nameCell = row.createCell(idx);
    nameCell.setCellStyle(this.dateStyle);
    nameCell.setCellValue(value);
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

  void writeChangeLog(List<Object[]> changeLogEvents) {
    boolean sheetExists = this.getSheets().stream().anyMatch((s) -> s.getName().equals(HISTORY_SHEET_NAME));
    SheetWrapper historySheet = this.findSheet(HISTORY_SHEET_NAME);
    historySheet.setWidths(new Integer[]{20*256, 100*256});
    historySheet.setColCount(2);
    if (!sheetExists) {
      Row headerRow = historySheet.nextRow();
      writeHeaderCell(headerRow, 0, "Date");
      writeHeaderCell(headerRow, 1, "Note");

      Row generatedRow = historySheet.nextRow();
      writeDateCell(generatedRow, 0, new Date());
      writeStringCell(generatedRow, 1, LOG_FILE_CREATED, false);
    }
    for (Object[] changeLogEvent : changeLogEvents) {
      Row row = historySheet.nextRow();
      writeDateCell(row, 0, (Date)changeLogEvent[0]);
      writeStringCell(row, 1, (String)changeLogEvent[1], false);
    }
  }
}
