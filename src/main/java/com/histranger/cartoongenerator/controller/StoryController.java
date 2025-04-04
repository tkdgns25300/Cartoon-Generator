package com.histranger.cartoongenerator.controller;

import com.histranger.cartoongenerator.model.StoryForm;
import com.histranger.cartoongenerator.service.StoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class StoryController {

    @Autowired
    private StoryService storyService;

    @GetMapping("/")
    public String showForm(Model model) {
        model.addAttribute("story", new StoryForm());
        return "storyForm"; // resources/templates/storyForm.html
    }

    @PostMapping("/submit")
    public ResponseEntity<byte[]> processStory(@ModelAttribute StoryForm story) {
        try {
            byte[] pdfBytes = storyService.processStory(story.getText());
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.builder("attachment").filename("result.pdf").build());
            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
