package com.techindna.anerti.endpoint.web;

import com.techindna.anerti.repository.ClassRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@AllArgsConstructor
public class PromotionController {

  private final ClassRepository classRepository;

  @GetMapping("/promotions")
  public String listPromotions(Model model) {
    model.addAttribute("promotions", classRepository.findAll());
    return "promotions";
  }
}
