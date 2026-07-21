package com.Research.Rapid;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/rapid/r")
@CrossOrigin(origins = "*")
@AllArgsConstructor
public class ResearchController {


    private final ResearchService researchService;

    @PostMapping("/process")
    public ResponseEntity<String> processContent(@RequestBody ResearchRequest researchRequest )
    {
        String result = researchService.ProcessContent(researchRequest);
        return ResponseEntity.ok(result);

    }
}
