package com.techindna.anerti.service;

import com.techindna.anerti.dto.ClassRankingEntry;
import com.techindna.anerti.dto.ClassRankingResponse;
import com.techindna.anerti.dto.StudentGeneralAverage;
import com.techindna.anerti.exception.http.ForbiddenException;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.exception.http.UnauthorizedException;
import com.techindna.anerti.repository.AuthRepository;
import com.techindna.anerti.repository.ClassRepository;
import com.techindna.anerti.repository.GradeRepository;
import com.techindna.anerti.repository.ReportRepository;
import com.techindna.anerti.repository.StudentInheritanceRepository;
import com.techindna.anerti.repository.enums.UserRole;
import com.techindna.anerti.repository.model.JClass;
import com.techindna.anerti.repository.model.JUser;
import com.techindna.anerti.validator.DataValidator;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class ReportService {

  private final ReportRepository reportRepository;
  private final ClassRepository classRepository;
  private final GradeRepository gradeRepository;
  private final StudentInheritanceRepository studentInheritanceRepository;
  private final AuthRepository authRepository;
  private final DataValidator dataValidator;

  @Transactional(readOnly = true)
  public ClassRankingResponse getClassRanking(UUID classId) {
    JClass clazz =
        classRepository
            .findById(classId)
            .orElseThrow(() -> new NotFoundException("Class %s not found".formatted(classId)));

    List<Object[]> rows = reportRepository.findClassRanking(classId);

    List<ClassRankingEntry> ranking =
        rows.stream()
            .map(
                row ->
                    new ClassRankingEntry(
                        ((Number) row[0]).intValue(),
                        UUID.fromString(row[1].toString()),
                        (String) row[2],
                        (String) row[3],
                        (String) row[4],
                        new BigDecimal(row[5].toString()).setScale(2)))
            .toList();

    return new ClassRankingResponse(clazz.getId(), clazz.getName(), clazz.getYearOf(), ranking);
  }

  @SneakyThrows
  public byte[] downloadClassRanking(UUID classId) {
    ClassRankingResponse response = getClassRanking(classId);
    return buildXlsx(response);
  }

  private byte[] buildXlsx(ClassRankingResponse response) throws IOException {
    try (Workbook workbook = new XSSFWorkbook();
        ByteArrayOutputStream out = new ByteArrayOutputStream()) {
      Sheet sheet = workbook.createSheet("Ranking");

      CellStyle headerStyle = workbook.createCellStyle();
      Font headerFont = workbook.createFont();
      headerFont.setBold(true);
      headerStyle.setFont(headerFont);

      Row header = sheet.createRow(0);
      String[] columns = {"Rank", "Student Ref", "First Name", "Last Name", "General Average"};
      for (int i = 0; i < columns.length; i++) {
        header.createCell(i).setCellValue(columns[i]);
        header.getCell(i).setCellStyle(headerStyle);
      }

      List<ClassRankingEntry> ranking = response.ranking();
      for (int i = 0; i < ranking.size(); i++) {
        ClassRankingEntry entry = ranking.get(i);
        Row row = sheet.createRow(i + 1);
        row.createCell(0).setCellValue(entry.rank());
        row.createCell(1).setCellValue(entry.ref());
        row.createCell(2).setCellValue(entry.firstName());
        row.createCell(3).setCellValue(entry.lastName());
        row.createCell(4).setCellValue(entry.generalAverage().doubleValue());
      }

      for (int i = 0; i < columns.length; i++) {
        sheet.autoSizeColumn(i);
      }

      workbook.write(out);
      return out.toByteArray();
    }
  }

  @Transactional(readOnly = true)
  public StudentGeneralAverage getStudentAverage(UUID studentInheritanceId, String academicYear) {
    if (academicYear != null && !academicYear.isBlank()) {
      dataValidator.checkStringLength("academicYear", academicYear, 10);
      dataValidator.validateAcademicYear(academicYear);
    }

    studentInheritanceRepository
        .findById(studentInheritanceId)
        .orElseThrow(
            () ->
                new NotFoundException(
                    "Student inheritance %s not found".formatted(studentInheritanceId)));

    JUser currentUser = currentUser();
    UUID teacherInheritanceId = null;

    if (currentUser.getRole() == UserRole.STUDENT) {
      if (currentUser.getStudentInheritance() == null
          || !currentUser.getStudentInheritance().getId().equals(studentInheritanceId)) {
        throw new ForbiddenException("Students can only compute their own average.");
      }
    } else if (currentUser.getRole() == UserRole.TEACHER) {
      if (currentUser.getTeacherInheritance() != null) {
        teacherInheritanceId = currentUser.getTeacherInheritance().getId();
      }
    }

    BigDecimal generalAverage =
        gradeRepository.computeGeneralAverage(
            studentInheritanceId,
            academicYear != null && !academicYear.isBlank() ? academicYear : null,
            teacherInheritanceId);

    return new StudentGeneralAverage(studentInheritanceId, academicYear, generalAverage);
  }

  private JUser currentUser() {
    return authRepository
        .findById(
            UUID.fromString(
                (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal()))
        .orElseThrow(() -> new UnauthorizedException("Authentication required."));
  }
}
