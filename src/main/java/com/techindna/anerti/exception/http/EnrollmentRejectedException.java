package com.techindna.anerti.exception.http;

import com.techindna.anerti.dto.EnrollmentRejected;
import lombok.Getter;

@Getter
public class EnrollmentRejectedException extends RuntimeException {

  private final EnrollmentRejected body;

  public EnrollmentRejectedException(EnrollmentRejected body) {
    super(body.message());
    this.body = body;
  }
}
