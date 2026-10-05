package sopvn.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import sopvn.demo.core.service.SizeRecommendationService;

@RestController
public class SizeAdvisorController {

    private final SizeRecommendationService sizeRecommendationService;

    public SizeAdvisorController(SizeRecommendationService sizeRecommendationService) {
        this.sizeRecommendationService = sizeRecommendationService;
    }

    @GetMapping("/api/size-advisor")
    public ResponseEntity<SizeRecommendationService.SizeRecommendationResult> getAdvice(
            @RequestParam("height") Integer height,
            @RequestParam("weight") Integer weight,
            @RequestParam(value = "fitType", defaultValue = "REGULAR") String fitType) {
        SizeRecommendationService.SizeRecommendationResult result = 
                sizeRecommendationService.recommendSize(height, weight, fitType);
        return ResponseEntity.ok(result);
    }
}
