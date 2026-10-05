package sopvn.demo.entity;

import jakarta.persistence.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Collections")
public class Collection {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "name", length = 150, nullable = false)
    private String name;

    @Column(name = "slug", length = 150, nullable = false, unique = true)
    private String slug;

    @Column(name = "banner_url", length = 500)
    private String bannerUrl;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "collection", fetch = FetchType.LAZY)
    private List<Product> products = new ArrayList<>();

    public Collection() {
    }

    public Collection(String name, String slug, String bannerUrl, String description, Boolean isActive) {
        this.name = name;
        this.slug = slug;
        this.bannerUrl = bannerUrl;
        this.description = description;
        this.isActive = isActive;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getBannerUrl() {
        if (bannerUrl == null || bannerUrl.trim().isEmpty()) {
            return "/images/banners/banner-everyday.jpg";
        }
        if (bannerUrl.contains("coolmate.me")) {
            String filename = bannerUrl.substring(bannerUrl.lastIndexOf("/") + 1);
            return "/images/banners/" + filename;
        }
        return bannerUrl;
    }

    public void setBannerUrl(String bannerUrl) {
        this.bannerUrl = bannerUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public List<Product> getProducts() {
        return products;
    }

    public void setProducts(List<Product> products) {
        this.products = products;
    }
}
