package com.techindna.anerti.service;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Table;
import com.lowagie.text.pdf.PdfWriter;
import java.io.ByteArrayOutputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PdfGenerator {

  public byte[] generateGradeReport(
      String studentName, String studentRef, java.util.List<GradeRow> grades) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Document document = new Document(PageSize.A4, 36, 36, 36, 36);

    try {
      PdfWriter.getInstance(document, out);
      document.open();

      Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
      Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 12);
      Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
      Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

      Paragraph title = new Paragraph("Releve de Notes", titleFont);
      title.setAlignment(Element.ALIGN_CENTER);
      document.add(title);

      Paragraph student =
          new Paragraph("Etudiant: %s (%s)".formatted(studentName, studentRef), subtitleFont);
      student.setAlignment(Element.ALIGN_CENTER);
      document.add(student);
      document.add(new Paragraph(" "));

      if (grades.isEmpty()) {
        document.add(new Paragraph("Aucune note enregistree.", normalFont));
      } else {
        Table table = new Table(5);
        table.setWidth(100);
        table.setPadding(4);
        table.setSpacing(1);

        addHeaderCell(table, "Cours", headerFont);
        addHeaderCell(table, "Examen", headerFont);
        addHeaderCell(table, "Date", headerFont);
        addHeaderCell(table, "Note", headerFont);
        addHeaderCell(table, "Coeff.", headerFont);

        for (GradeRow grade : grades) {
          addCell(table, grade.courseRef(), normalFont);
          addCell(table, grade.examDate(), normalFont);
          addCell(table, grade.date(), normalFont);
          addCell(table, grade.value(), normalFont);
          addCell(table, grade.coefficient(), normalFont);
        }

        document.add(table);
      }

      document.close();
    } catch (DocumentException e) {
      throw new RuntimeException("Failed to generate PDF", e);
    }

    return out.toByteArray();
  }

  private void addHeaderCell(Table table, String text, Font font) {
    var cell = new com.lowagie.text.Cell(new Paragraph(text, font));
    cell.setBackgroundColor(new java.awt.Color(200, 200, 255));
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    table.addCell(cell);
  }

  private void addCell(Table table, String text, Font font) {
    var cell = new com.lowagie.text.Cell(new Paragraph(text, font));
    cell.setHorizontalAlignment(Element.ALIGN_CENTER);
    table.addCell(cell);
  }

  public record GradeRow(
      String courseRef, String examDate, String date, String value, String coefficient) {}
}
