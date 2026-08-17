package com.techindna.anerti.service;

import com.techindna.anerti.dto.GraduateOutput;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class ExcelGenerator {

  public byte[] generateGraduationExcel(
      String className, int yearOf, List<GraduateOutput> graduates) {
    try (XSSFWorkbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      XSSFSheet sheet = workbook.createSheet("Graduates");

      CellStyle headerStyle = workbook.createCellStyle();
      headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
      headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
      headerStyle.setAlignment(HorizontalAlignment.CENTER);
      org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
      headerFont.setColor(IndexedColors.WHITE.getIndex());
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);
      setBorders(headerStyle);

      CellStyle dataStyle = workbook.createCellStyle();
      dataStyle.setAlignment(HorizontalAlignment.CENTER);
      setBorders(dataStyle);

      XSSFRow titleRow = sheet.createRow(0);
      XSSFCell titleCell = titleRow.createCell(0);
      titleCell.setCellValue("Promotion: %s (%d)".formatted(className, yearOf));
      sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 4));

      String[] headers = {"Rank", "Student ID", "First Name", "Last Name", "Average"};
      XSSFRow headerRow = sheet.createRow(2);
      for (int i = 0; i < headers.length; i++) {
        XSSFCell cell = headerRow.createCell(i);
        cell.setCellValue(headers[i]);
        cell.setCellStyle(headerStyle);
      }

      for (int i = 0; i < graduates.size(); i++) {
        GraduateOutput g = graduates.get(i);
        XSSFRow row = sheet.createRow(i + 3);
        createCell(row, 0, String.valueOf(g.rank()), dataStyle);
        createCell(row, 1, g.studentId().toString(), dataStyle);
        createCell(row, 2, g.firstName(), dataStyle);
        createCell(row, 3, g.lastName(), dataStyle);
        createCell(row, 4, g.average().toPlainString(), dataStyle);
      }

      for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();
    } catch (IOException e) {
      throw new RuntimeException("Failed to generate Excel file", e);
    }
  }

  private void createCell(XSSFRow row, int col, String value, CellStyle style) {
    XSSFCell cell = row.createCell(col);
    cell.setCellValue(value);
    cell.setCellStyle(style);
  }

  private void setBorders(CellStyle style) {
    style.setBorderTop(BorderStyle.THIN);
    style.setBorderBottom(BorderStyle.THIN);
    style.setBorderLeft(BorderStyle.THIN);
    style.setBorderRight(BorderStyle.THIN);
  }
}
