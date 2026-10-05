package sopvn.demo.core.exception;

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public String handleCustomException(CustomException ex, Model model) {
        model.addAttribute("status", 400);
        model.addAttribute("title", "Thông Báo Từ Hệ Thống");
        model.addAttribute("message", ex.getMessage());
        return "error";
    }

    @ExceptionHandler(ResponseStatusException.class)
    public String handleResponseStatusException(ResponseStatusException ex, Model model) {
        model.addAttribute("status", ex.getStatusCode().value());
        model.addAttribute("title", "Yêu Cầu Không Hợp Lệ");
        model.addAttribute("message", ex.getReason() != null ? ex.getReason() : "Trang hoặc tài nguyên bạn tìm kiếm không khả dụng.");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleGeneralException(Exception ex, Model model) {
        model.addAttribute("status", 500);
        model.addAttribute("title", "Thông Báo");
        model.addAttribute("message", "Hệ thống tạm thời chưa thể xử lý yêu cầu này. Vui lòng thử lại sau giây lát hoặc liên hệ hỗ trợ Coolmate.");
        return "error";
    }
}
