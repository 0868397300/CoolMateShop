package sopvn.demo.order;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Service
public class ShippingFeeService {

    public static final BigDecimal FREESHIP_THRESHOLD = BigDecimal.valueOf(200_000);
    public static final BigDecimal INNER_CITY_FEE = BigDecimal.valueOf(20_000);
    public static final BigDecimal OUTER_CITY_FEE = BigDecimal.valueOf(25_000);
    public static final BigDecimal PROVINCE_FEE = BigDecimal.valueOf(30_000);

    private static final Set<String> INNER_DISTRICTS = new HashSet<>(Arrays.asList(
            "ba đình", "hoàn kiếm", "đống đa", "cầu giấy", "hai bà trưng", "thanh xuân", "tây hồ",
            "quận 1", "quận 3", "quận 4", "quận 5", "quận 10", "bình thạnh", "phú nhuận", "tân bình"
    ));

    public BigDecimal calculateShippingFee(BigDecimal subtotalAfterCombo, String province, String district) {
        if (subtotalAfterCombo != null && subtotalAfterCombo.compareTo(FREESHIP_THRESHOLD) >= 0) {
            return BigDecimal.ZERO;
        }

        if (province == null || province.isBlank()) {
            return PROVINCE_FEE;
        }

        String p = province.trim().toLowerCase();
        if (p.contains("hà nội") || p.contains("hồ chí minh") || p.contains("hcm")) {
            if (district != null && !district.isBlank()) {
                String d = district.trim().toLowerCase();
                for (String inner : INNER_DISTRICTS) {
                    if (d.contains(inner)) {
                        return INNER_CITY_FEE;
                    }
                }
            }
            return OUTER_CITY_FEE;
        }

        return PROVINCE_FEE;
    }
}
