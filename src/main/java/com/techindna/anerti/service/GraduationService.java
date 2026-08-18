package com.techindna.anerti.service;

import com.techindna.anerti.dto.GraduateOutput;
import com.techindna.anerti.dto.GraduationListResponse;
import com.techindna.anerti.dto.Meta;
import com.techindna.anerti.exception.http.NotFoundException;
import com.techindna.anerti.repository.ClassRepository;
import com.techindna.anerti.repository.GraduationRepository;
import com.techindna.anerti.repository.model.JClass;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class GraduationService {

  private final GraduationRepository graduationRepository;
  private final ClassRepository classRepository;

  @Transactional(readOnly = true)
  public GraduationListResponse listGraduates(UUID classId) {
    JClass clazz =
        classRepository
            .findById(classId)
            .orElseThrow(() -> new NotFoundException("Class %s not found".formatted(classId)));

    List<Object[]> rows = graduationRepository.findGraduates(classId);

    List<GraduateOutput> graduates =
        rows.stream()
            .map(
                row ->
                    new GraduateOutput(
                        ((Number) row[0]).intValue(),
                        UUID.fromString(row[1].toString()),
                        (String) row[2],
                        (String) row[3],
                        new BigDecimal(row[4].toString()).setScale(2)))
            .toList();

    return new GraduationListResponse(
        clazz.getName(),
        clazz.getYearOf(),
        graduates,
        new Meta(1, graduates.size(), graduates.size()));
  }
}
