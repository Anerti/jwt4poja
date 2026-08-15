package com.techindna.anerti.dto;

import com.techindna.anerti.entity.HistoryEntry;
import java.util.List;

public record HistoryListResponse(List<HistoryEntry> data) {}
