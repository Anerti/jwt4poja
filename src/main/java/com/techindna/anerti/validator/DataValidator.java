package com.techindna.anerti.validator;

import com.techindna.anerti.exception.http.BadRequestException;
import com.techindna.anerti.exception.http.UnprocessableContentException;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

@Component
public class DataValidator {

  private static final Pattern EMAIL_FORMAT =
      Pattern.compile("^[a-z0-9_.-]+@[a-z0-9_-]+(\\.[a-z]+){1,2}$");
  private static final Pattern NAME_FORMAT = Pattern.compile("^[A-Z][a-z-'éèê ]{2,}$");
  private static final Pattern USERNAME_FORMAT = Pattern.compile("^[a-zA-Z_0-9-]{2,}$");
  private static final Pattern SEARCH_FORMAT = Pattern.compile("^[a-zA-Z0-9_@.'éèê -]+$");
  private static final Pattern REF_FORMAT = Pattern.compile("^[A-Za-z0-9-]+$");
  private static final Pattern TITLE_FORMAT = Pattern.compile("^[A-Za-z0-9èéê -]+$");
  private static final Pattern ACADEMIC_YEAR_FORMAT = Pattern.compile("^\\d{4}-\\d{4}$");

  public void checkNullData(String field, String value) {
    if (value == null || value.isBlank()) {
      throw new UnprocessableContentException(
          String.format("%s is required and cannot be blank", field));
    }
  }

  public void checkStringLength(String field, String value, int maxLength) {
    if (value != null && value.length() > maxLength) {
      throw new UnprocessableContentException(
          String.format("%s must not exceed %s characters", field, maxLength));
    }
  }

  public void validateTitle(String value) {
    checkNullData("title", value);
    checkStringLength("title", value, 100);

    if (!TITLE_FORMAT.matcher(value).matches()) {
      throw new UnprocessableContentException(
          String.format(
              "Title %s is invalid, it may only contain letters, numbers, spaces, and the symbols -"
                  + " è é ê",
              value));
    }
  }

  public void validateCoefficient(BigDecimal coefficient) {
    if (coefficient == null) {
      throw new BadRequestException("coefficient is required and cannot be blank");
    }
    if (coefficient.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BadRequestException("coefficient must be greater than 0");
    }
    if (coefficient.compareTo(BigDecimal.ONE) > 0) {
      throw new UnprocessableContentException("coefficient must not exceed 1");
    }
    if (coefficient.stripTrailingZeros().scale() > 2) {
      throw new UnprocessableContentException("coefficient must have at most 2 decimal places");
    }
  }

  public void validateAcademicYear(String academicYear) {
    checkNullData("academicYear", academicYear);
    checkStringLength("academicYear", academicYear, 10);

    if (!ACADEMIC_YEAR_FORMAT.matcher(academicYear.strip()).matches()) {
      throw new UnprocessableContentException(
          "academicYear is invalid, the format must be YYYY-YYYY");
    }
  }

  public void validateSearchString(String value) {
    checkStringLength("search", value, 100);

    if (value != null && !value.isBlank() && !SEARCH_FORMAT.matcher(value).matches()) {
      throw new UnprocessableContentException(
          String.format(
              "Search %s is invalid, it may only contain letters, numbers, spaces, and the"
                  + " symbols @ _ . ' -",
              value));
    }
  }

  public void validateUsername(String value) {
    checkNullData("username", value);
    checkStringLength("username", value, 50);

    if (!USERNAME_FORMAT.matcher(value).matches()) {
      throw new UnprocessableContentException(String.format("Username %s is invalid", value));
    }
  }

  public void validateEmail(String field, String value) {
    checkNullData(field, value);
    checkStringLength(field, value, 100);

    if (!EMAIL_FORMAT.matcher(value.toLowerCase()).matches()) {
      throw new UnprocessableContentException(String.format("Email %s is not valid", value));
    }
  }

  public void validateRef(String value) {
    checkNullData("ref", value);
    checkStringLength("ref", value, 10);

    if (!REF_FORMAT.matcher(value).matches()) {
      throw new UnprocessableContentException(
          String.format(
              "Ref %s is invalid, it may only contain letters, numbers, and the symbol -", value));
    }
  }

  public void validateName(String field, String value) {
    checkNullData(field, value);
    checkStringLength(field, value, 100);

    if (!NAME_FORMAT.matcher(value).matches()) {
      throw new UnprocessableContentException(
          String.format(
              "%s must start with a capital letter and contain only letters, hyphens, apostrophes,"
                  + " and spaces",
              field));
    }
  }

  public void validateUserAccount(
      String username,
      String password,
      String firstName,
      String lastName,
      String email,
      String ref) {
    validateUsername(username);
    checkPasswordSecurityLevel(password);
    validateName("firstName", firstName);
    validateName("lastName", lastName);
    validateEmail("email", email);
    validateRef(ref);
  }

  public void checkPasswordSecurityLevel(String password) {
    checkNullData("password", password);

    if (password.length() < 12) {
      throw new UnprocessableContentException("Password must be at least 12 characters");
    }

    if (!password.matches(".*[A-Z].*")) {
      throw new UnprocessableContentException(
          "Password must contain at least one uppercase character");
    }

    if (!password.matches(".*[a-z].*")) {
      throw new UnprocessableContentException(
          "Password must contain at least one lowercase character");
    }

    if (!password.matches(".*[0-9].*")) {
      throw new UnprocessableContentException("Password must contain at least one digit");
    }

    if (!password.matches(".*[!?*+=@#$%^&()_\\-\\[\\]{}|\\\\:;\"'<>,./`~].*")) {
      throw new UnprocessableContentException(
          "Password must contain at least one special character");
    }
  }
}
