package sopvn.demo.core.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class SizeRecommendationService {

    public static class SizeRecommendationResult {
        private String recommendedSize;
        private String confidence;
        private String explanation;
        private List<String> nearbySizes = new ArrayList<>();

        public SizeRecommendationResult() {}

        public SizeRecommendationResult(String recommendedSize, String confidence, String explanation, List<String> nearbySizes) {
            this.recommendedSize = recommendedSize;
            this.confidence = confidence;
            this.explanation = explanation;
            this.nearbySizes = nearbySizes;
        }

        public String getRecommendedSize() { return recommendedSize; }
        public void setRecommendedSize(String recommendedSize) { this.recommendedSize = recommendedSize; }

        public String getConfidence() { return confidence; }
        public void setConfidence(String confidence) { this.confidence = confidence; }

        public String getExplanation() { return explanation; }
        public void setExplanation(String explanation) { this.explanation = explanation; }

        public List<String> getNearbySizes() { return nearbySizes; }
        public void setNearbySizes(List<String> nearbySizes) { this.nearbySizes = nearbySizes; }
    }

    public SizeRecommendationResult recommendSize(Integer heightCm, Integer weightKg, String fitType) {
        if (heightCm == null || weightKg == null || heightCm <= 0 || weightKg <= 0) {
            return new SizeRecommendationResult("L", "LOW", "Vui lòng nhập chiều cao và cân nặng để được tư vấn chính xác.", Arrays.asList("M", "XL"));
        }

        String fit = fitType != null ? fitType.trim().toUpperCase() : "REGULAR";

        double heightM = heightCm / 100.0;
        double bmi = weightKg / (heightM * heightM);

        String baseSize;
        if (heightCm < 165 && weightKg < 57) {
            baseSize = "S";
        } else if (heightCm <= 170 && weightKg <= 65) {
            baseSize = "M";
        } else if (heightCm <= 175 && weightKg <= 73) {
            baseSize = "L";
        } else if (heightCm <= 180 && weightKg <= 81) {
            baseSize = "XL";
        } else if (heightCm <= 185 && weightKg <= 88) {
            baseSize = "2XL";
        } else {
            baseSize = "3XL";
        }

        List<String> sizeLadder = Arrays.asList("S", "M", "L", "XL", "2XL", "3XL");
        int idx = sizeLadder.indexOf(baseSize);

        if ("OVERSIZE".equals(fit) && idx < sizeLadder.size() - 1) {
            idx++;
        }

        String finalSize = sizeLadder.get(idx);
        List<String> nearby = new ArrayList<>();
        if (idx > 0) nearby.add(sizeLadder.get(idx - 1));
        if (idx < sizeLadder.size() - 1) nearby.add(sizeLadder.get(idx + 1));

        String explanation = "Dựa trên chiều cao " + heightCm + "cm, cân nặng " + weightKg + "kg (BMI: " + String.format("%.1f", bmi) + ") và phom dáng " + fit + ", size " + finalSize + " là lựa chọn tối ưu nhất.";

        return new SizeRecommendationResult(finalSize, "HIGH", explanation, nearby);
    }
}
