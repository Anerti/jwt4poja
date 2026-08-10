package com.techindna.anerti.endpoint.event.consumer.model;

import com.techindna.anerti.PojaGenerated;
import com.techindna.anerti.endpoint.event.model.PojaEvent;

@PojaGenerated
public record TypedEvent(String typeName, PojaEvent payload) {}
