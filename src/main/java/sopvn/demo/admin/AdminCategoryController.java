package sopvn.demo.admin;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import sopvn.demo.core.util.SlugUtils;
import sopvn.demo.entity.Category;
import sopvn.demo.entity.Collection;
import sopvn.demo.repository.CategoryRepository;
import sopvn.demo.repository.CollectionRepository;

import java.util.List;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin/danh-muc")
public class AdminCategoryController {

    private final CategoryRepository categoryRepository;
    private final CollectionRepository collectionRepository;

    public AdminCategoryController(CategoryRepository categoryRepository, CollectionRepository collectionRepository) {
        this.categoryRepository = categoryRepository;
        this.collectionRepository = collectionRepository;
    }

    @GetMapping
    public String listCategories(@RequestParam(value = "keyword", required = false) String keyword,
                                 @RequestParam(value = "level", required = false) String level,
                                 @RequestParam(value = "status", required = false) String status,
                                 Model model) {
        List<Category> categories = categoryRepository.findAll();

        if (keyword != null && !keyword.isBlank()) {
            String kw = keyword.trim().toLowerCase();
            categories = categories.stream()
                    .filter(c -> (c.getName() != null && c.getName().toLowerCase().contains(kw))
                            || (c.getSlug() != null && c.getSlug().toLowerCase().contains(kw)))
                    .collect(Collectors.toList());
        }

        if (level != null && !level.isBlank()) {
            if ("ROOT".equalsIgnoreCase(level)) {
                categories = categories.stream().filter(c -> c.getParent() == null).collect(Collectors.toList());
            } else if ("SUB".equalsIgnoreCase(level)) {
                categories = categories.stream().filter(c -> c.getParent() != null).collect(Collectors.toList());
            }
        }

        if (status != null && !status.isBlank()) {
            if ("ACTIVE".equalsIgnoreCase(status)) {
                categories = categories.stream().filter(c -> Boolean.TRUE.equals(c.getIsActive())).collect(Collectors.toList());
            } else if ("INACTIVE".equalsIgnoreCase(status)) {
                categories = categories.stream().filter(c -> !Boolean.TRUE.equals(c.getIsActive())).collect(Collectors.toList());
            }
        }

        List<Category> rootCategories = categoryRepository.findByParentIsNullAndIsActiveTrueOrderByDisplayOrderAsc();
        List<Collection> collections = collectionRepository.findAll();

        model.addAttribute("categories", categories);
        model.addAttribute("rootCategories", rootCategories);
        model.addAttribute("collections", collections);
        model.addAttribute("keyword", keyword);
        model.addAttribute("selectedLevel", level);
        model.addAttribute("selectedStatus", status);

        return "admin/category-list";
    }

    @PostMapping("/tao-moi")
    public String createCategory(@RequestParam("name") String name,
                                 @RequestParam(value = "parentId", required = false) Integer parentId,
                                 @RequestParam(value = "description", required = false) String description,
                                 @RequestParam(value = "imageUrl", required = false) String imageUrl,
                                 @RequestParam(value = "displayOrder", required = false, defaultValue = "1") Integer displayOrder,
                                 RedirectAttributes redirectAttributes) {
        try {
            Category c = new Category();
            c.setName(name.trim());
            c.setSlug(SlugUtils.toSlug(name));
            if (parentId != null && parentId > 0) {
                categoryRepository.findById(parentId).ifPresent(c::setParent);
            }
            c.setDescription(description);
            c.setImageUrl(imageUrl);
            c.setDisplayOrder(displayOrder != null ? displayOrder : 1);
            c.setIsActive(true);

            categoryRepository.save(c);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm danh mục mới thành công: " + c.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo danh mục: " + e.getMessage());
        }
        return "redirect:/admin/danh-muc";
    }

    @PostMapping("/{id}/trang-thai")
    public String toggleCategory(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        categoryRepository.findById(id).ifPresent(c -> {
            c.setIsActive(!Boolean.TRUE.equals(c.getIsActive()));
            categoryRepository.save(c);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái danh mục: " + c.getName());
        });
        return "redirect:/admin/danh-muc";
    }

    @PostMapping("/{id}/xoa")
    public String deleteCategory(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            categoryRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa danh mục thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa danh mục do có sản phẩm hoặc danh mục con đang phụ thuộc.");
        }
        return "redirect:/admin/danh-muc";
    }

    @PostMapping("/bo-suu-tap/tao-moi")
    public String createCollection(@RequestParam("name") String name,
                                   @RequestParam(value = "bannerUrl", required = false) String bannerUrl,
                                   @RequestParam(value = "description", required = false) String description,
                                   RedirectAttributes redirectAttributes) {
        try {
            Collection col = new Collection();
            col.setName(name.trim());
            col.setSlug(SlugUtils.toSlug(name));
            col.setBannerUrl(bannerUrl);
            col.setDescription(description);
            col.setIsActive(true);

            collectionRepository.save(col);
            redirectAttributes.addFlashAttribute("successMessage", "Thêm bộ sưu tập mới thành công: " + col.getName());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi khi tạo bộ sưu tập: " + e.getMessage());
        }
        return "redirect:/admin/danh-muc";
    }

    @PostMapping("/bo-suu-tap/{id}/trang-thai")
    public String toggleCollection(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        collectionRepository.findById(id).ifPresent(col -> {
            col.setIsActive(!Boolean.TRUE.equals(col.getIsActive()));
            collectionRepository.save(col);
            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái bộ sưu tập: " + col.getName());
        });
        return "redirect:/admin/danh-muc";
    }

    @PostMapping("/bo-suu-tap/{id}/xoa")
    public String deleteCollection(@PathVariable("id") Integer id, RedirectAttributes redirectAttributes) {
        try {
            collectionRepository.deleteById(id);
            redirectAttributes.addFlashAttribute("successMessage", "Đã xóa bộ sưu tập thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Không thể xóa bộ sưu tập: " + e.getMessage());
        }
        return "redirect:/admin/danh-muc";
    }
}