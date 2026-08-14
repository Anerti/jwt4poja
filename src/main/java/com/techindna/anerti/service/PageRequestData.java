package com.techindna.anerti.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PageRequestData(Pageable pageable, int page, int size) {
  public static PageRequestData of(int page, int size, Sort sort) {
    int validPage = defaultIfInvalid(page, 1, 100, 1);
    int validSize = defaultIfInvalid(size, 1, 100, 10);
    return new PageRequestData(
        PageRequest.of(validPage - 1, validSize, sort), validPage, validSize);
  }

  private static int defaultIfInvalid(int value, int min, int max, int defaultValue) {
    return (value < min || value > max) ? defaultValue : value;
  }
}
