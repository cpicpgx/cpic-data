package org.cpicpgx.util;

import org.apache.poi.openxml4j.exceptions.InvalidFormatException;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testing methods from the {@link RowWrapper} class that helps read data from Excel rows
 *
 * @author Ryan Whaley
 */
public class RowWrapperTest {
  private static final String TEST_EXCEL = "superscript_example.xlsx";
  
  @Test
  public void testHasNoText() throws IOException, InvalidFormatException {
    try (InputStream in = getClass().getResourceAsStream(TEST_EXCEL)) {
      WorkbookWrapper workbook = new WorkbookWrapper(in);
      
      RowWrapper row = workbook.getRow(0);
      assertNotNull(row);
      
      assertFalse(row.hasNoText(0));
      assertTrue(row.hasNoText(3));
    }
  }
  
  @Test
  public void testGetNullableText() throws IOException, InvalidFormatException {
    try (InputStream in = getClass().getResourceAsStream(TEST_EXCEL)) {
      WorkbookWrapper workbook = new WorkbookWrapper(in);
      
      RowWrapper row = workbook.getRow(0);
      assertNotNull(row.getNullableText(0));
      assertEquals("CYP2C19 Diplotype", row.getNullableText(0));
      
      assertNull(row.getNullableText(3));
      
      row = workbook.getRow(2);
      assertEquals("1.25", row.getNullableText(0));
      assertEquals("3.0", row.getNullableText(1));
      assertEquals("3", row.getNullableText(1, true));
      assertEquals("11/11/18", row.getNullableText(2));
      
      row = workbook.getRow(3);
      assertEquals("3.75", row.getNullableText(0));
    }
  }
  
  @Test
  public void testStripFootnote() throws IOException, InvalidFormatException {
    try (InputStream in = getClass().getResourceAsStream(TEST_EXCEL)) {
      WorkbookWrapper workbook = new WorkbookWrapper(in);

      RowWrapper row = workbook.getRow(0);
      assertEquals("EHR Priority Result Notation", row.stripFootnote(2));

      row = workbook.getRow(3);
      assertEquals("test a midstring styling", row.stripFootnote(1));
    }
  }

  @Test
  public void testGetFootnote() throws IOException, InvalidFormatException {
    try (InputStream in = getClass().getResourceAsStream(TEST_EXCEL)) {
      WorkbookWrapper workbook = new WorkbookWrapper(in);

      RowWrapper row = workbook.getRow(0);
      assertEquals("b", row.getFootnote(2));

      row = workbook.getRow(3);
      assertNull(row.getFootnote(1));
    }
  }

  @Test
  public void testGetNullableDouble() throws IOException, InvalidFormatException {
    try (InputStream in = getClass().getResourceAsStream(TEST_EXCEL)) {
      WorkbookWrapper workbook = new WorkbookWrapper(in);

      RowWrapper row = workbook.getRow(2);
      assertEquals(Double.valueOf(1.25), row.getNullableDouble(0));
      assertNull(row.getNullableDouble(3));
    }
  }

  @Test
  public void testGetNullableLong() throws IOException, InvalidFormatException {
    try (InputStream in = getClass().getResourceAsStream(TEST_EXCEL)) {
      WorkbookWrapper workbook = new WorkbookWrapper(in);

      RowWrapper row = workbook.getRow(2);
      assertEquals(Long.valueOf(3), row.getNullableLong(1));
      assertNull(row.getNullableLong(3));
    }
  }

  @Test
  public void testGetNullableDate() throws IOException, InvalidFormatException, ParseException {
    try (InputStream in = getClass().getResourceAsStream(TEST_EXCEL)) {
      WorkbookWrapper workbook = new WorkbookWrapper(in);

      RowWrapper row = workbook.getRow(2);
      assertEquals(new SimpleDateFormat("MM/dd/yyyy").parse("11/11/2018"), row.getNullableDate(2));
      assertNull(row.getNullableDate(3));
    }
  }
}
