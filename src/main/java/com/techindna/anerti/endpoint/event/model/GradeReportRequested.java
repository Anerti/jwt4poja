package com.techindna.anerti.endpoint.event.model;

import java.time.Duration;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Data
@EqualsAndHashCode(callSuper = false)
public class GradeReportRequested extends PojaEvent {
  private UUID studentInheritanceId;
  private String academicYear;
  private String studentEmail;

  @Override
  public Duration maxConsumerDuration() {
    return Duration.ofSeconds(90);
  }

  @Override
  public Duration maxConsumerBackoffBetweenRetries() {
    return Duration.ofSeconds(30);
  }
}
