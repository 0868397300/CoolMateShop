package sopvn.demo.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    @NotBlank(message = "Vui lòng nhập họ và tên của bạn.")
    private String fullName;

    @NotBlank(message = "Vui lòng nhập địa chỉ email.")
    @Email(message = "Địa chỉ email không đúng định dạng (Ví dụ hợp lệ: name@gmail.com).")
    private String email;

    @Pattern(regexp = "^$|^(0[3|5|7|8|9])+([0-9]{8})$", message = "Số điện thoại không hợp lệ. Vui lòng nhập số điện thoại di động 10 chữ số (bắt đầu bằng 03, 05, 07, 08, 09).")
    private String phone;

    @NotBlank(message = "Vui lòng nhập mật khẩu.")
    @Size(min = 6, message = "Mật khẩu quá ngắn. Vui lòng nhập mật khẩu có độ dài từ 6 ký tự trở lên để bảo mật tài khoản.")
    private String password;

    private Integer heightCm;
    private Integer weightKg;
    private String gender;

    public RegisterRequest() {
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getHeightCm() {
        return heightCm;
    }

    public void setHeightCm(Integer heightCm) {
        this.heightCm = heightCm;
    }

    public Integer getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(Integer weightKg) {
        this.weightKg = weightKg;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }
}
