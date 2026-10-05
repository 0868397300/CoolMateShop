package sopvn.demo.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import sopvn.demo.entity.*;
import sopvn.demo.entity.Collection;
import sopvn.demo.repository.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Tự động khởi tạo dữ liệu mẫu nếu cơ sở dữ liệu còn trống.
 * Giúp ứng dụng chạy ngay lập tức mà không cần chạy SQL thủ công.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final CollectionRepository collectionRepository;
    private final ColorRepository colorRepository;
    private final SizeRepository sizeRepository;
    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final ProductImageRepository productImageRepository;
    private final PromotionRepository promotionRepository;
    private final ComboRuleRepository comboRuleRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(CategoryRepository categoryRepository,
                           CollectionRepository collectionRepository,
                           ColorRepository colorRepository,
                           SizeRepository sizeRepository,
                           RoleRepository roleRepository,
                           UserRepository userRepository,
                           ProductRepository productRepository,
                           ProductVariantRepository productVariantRepository,
                           ProductImageRepository productImageRepository,
                           PromotionRepository promotionRepository,
                           ComboRuleRepository comboRuleRepository,
                           PasswordEncoder passwordEncoder) {
        this.categoryRepository = categoryRepository;
        this.collectionRepository = collectionRepository;
        this.colorRepository = colorRepository;
        this.sizeRepository = sizeRepository;
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.productImageRepository = productImageRepository;
        this.promotionRepository = promotionRepository;
        this.comboRuleRepository = comboRuleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        // Q: Chỉ seed missing data, không ghi đè mật khẩu hoặc reset tài khoản khi khởi động lại
        if (categoryRepository.count() > 0 && userRepository.count() > 0) {
            return;
        }

        if (categoryRepository.count() > 0) {
            return; // Đã có dữ liệu
        }

        // 1. Roles
        Role adminRole = roleRepository.save(new Role("ROLE_ADMIN", "Quản trị viên toàn quyền hệ thống Coolmate"));
        Role staffRole = roleRepository.save(new Role("ROLE_STAFF", "Nhân viên vận hành đơn hàng, kho SKU"));
        Role customerRole = roleRepository.save(new Role("ROLE_CUSTOMER", "Khách hàng thành viên hội viên CoolClub"));

        // 2. Colors
        Color cBlack = colorRepository.save(new Color("Đen", "#111111"));
        Color cWhite = colorRepository.save(new Color("Trắng", "#FFFFFF"));
        Color cNavy = colorRepository.save(new Color("Xanh Navy", "#1B2A4A"));
        Color cGray = colorRepository.save(new Color("Xám Melange", "#8E9196"));
        Color cOlive = colorRepository.save(new Color("Xanh Rêu", "#3B5323"));
        Color cAqua = colorRepository.save(new Color("Xanh Biển (Aqua)", "#0077BE"));
        Color cSand = colorRepository.save(new Color("Be (Sand)", "#E1D5C9"));

        // 3. Sizes
        Size sS = sizeRepository.save(new Size("S", 155, 164, 48, 56, 1));
        Size sM = sizeRepository.save(new Size("M", 165, 169, 57, 64, 2));
        Size sL = sizeRepository.save(new Size("L", 170, 174, 65, 72, 3));
        Size sXL = sizeRepository.save(new Size("XL", 175, 179, 73, 80, 4));
        Size s2XL = sizeRepository.save(new Size("2XL", 180, 185, 81, 88, 5));
        Size s3XL = sizeRepository.save(new Size("3XL", 185, 195, 89, 100, 6));

        // 4. Categories (Root & Sub)
        Category catShirt = new Category("Áo Nam", "ao-nam", "Tất cả các dòng áo nam thiết kế tối giản, công nghệ cao", "https://media3.coolmate.me/cdn-cgi/image/width=672,height=990,quality=85/uploads/March2024/ao-nam-thumb.jpg", 1, true);
        catShirt = categoryRepository.save(catShirt);

        Category catPants = new Category("Quần Nam", "quan-nam", "Quần short, quần dài co giãn thoải mái suốt ngày dài", "https://media3.coolmate.me/cdn-cgi/image/width=672,height=990,quality=85/uploads/March2024/quan-nam-thumb.jpg", 2, true);
        catPants = categoryRepository.save(catPants);

        Category catUnderwear = new Category("Đồ Lót Nam", "do-lot-nam", "Quần lót nam kháng khuẩn, thoáng khí Bamboo, Excool", "https://media3.coolmate.me/cdn-cgi/image/width=672,height=990,quality=85/uploads/March2024/do-lot-thumb.jpg", 3, true);
        catUnderwear = categoryRepository.save(catUnderwear);

        Category catAccessories = new Category("Phụ Kiện Nam", "phu-kien-nam", "Tất/vớ thể thao, mũ lưỡi trai, túi tote", "https://media3.coolmate.me/cdn-cgi/image/width=672,height=990,quality=85/uploads/March2024/phu-kien-thumb.jpg", 4, true);
        catAccessories = categoryRepository.save(catAccessories);

        Category catTee = new Category("Áo Thun Nam", "ao-thun-nam", "Áo thun Cotton Compact chống nhăn", null, 1, true);
        catTee.setParent(catShirt);
        catTee = categoryRepository.save(catTee);

        Category catPolo = new Category("Áo Polo Nam", "ao-polo-nam", "Áo Polo Excool, Café khử mùi", null, 2, true);
        catPolo.setParent(catShirt);
        catPolo = categoryRepository.save(catPolo);

        Category catSinglet = new Category("Áo Tanktop & Singlet", "ao-singlet-nam", "Áo ba lỗ chạy bộ siêu nhẹ", null, 3, true);
        catSinglet.setParent(catShirt);
        catSinglet = categoryRepository.save(catSinglet);

        Category catJacket = new Category("Áo Khoác Nam", "ao-khoac-nam", "Áo khoác gió trượt nước chống UV", null, 4, true);
        catJacket.setParent(catShirt);
        catJacket = categoryRepository.save(catJacket);

        Category catShort = new Category("Quần Short Nam", "quan-short-nam", "Quần short chạy bộ 5 inch", null, 1, true);
        catShort.setParent(catPants);
        catShort = categoryRepository.save(catShort);

        Category catUTPants = new Category("Quần Dài & Pants", "quan-dai-nam", "Quần dài UT Pants đa năng", null, 2, true);
        catUTPants.setParent(catPants);
        catUTPants = categoryRepository.save(catUTPants);

        Category catTrunk = new Category("Quần Lót Trunk (Boxer)", "quan-lot-trunk", "Quần lót dáng đùi ôm vừa vặn", null, 1, true);
        catTrunk.setParent(catUnderwear);
        catTrunk = categoryRepository.save(catTrunk);

        Category catSocks = new Category("Tất / Vớ Nam", "tat-vo-nam", "Tất cổ ngắn chống trượt kháng khuẩn", null, 1, true);
        catSocks.setParent(catAccessories);
        catSocks = categoryRepository.save(catSocks);

        // 5. Collections
        Collection colEveryday = collectionRepository.save(new Collection("Mặc Hàng Ngày (Everyday Wear)", "mac-hang-ngay", "https://media3.coolmate.me/uploads/banner-everyday.jpg", "Tủ đồ cơ bản tiện lợi", true));
        Collection colRunning = collectionRepository.save(new Collection("Đồ Chạy Bộ (Coolmate Running)", "do-chay-bo", "https://media3.coolmate.me/uploads/banner-running.jpg", "Trang phục chạy bộ chuyên nghiệp", true));
        Collection colExcool = collectionRepository.save(new Collection("Công Nghệ Làm Mát Excool", "cong-nghe-excool", "https://media3.coolmate.me/uploads/banner-excool.jpg", "Sợi Sorona thực vật làm mát", true));
        Collection colCareShare = collectionRepository.save(new Collection("Care & Share", "care-and-share", "https://media3.coolmate.me/uploads/banner-careshare.jpg", "Thời trang thiện nguyện", true));

        // 6. Users
        String encPass = passwordEncoder.encode("123456");

        User uAdmin = new User();
        uAdmin.setFullName("Quản Trị Viên Coolmate");
        uAdmin.setEmail("admin@coolmate.me");
        uAdmin.setPhone("0901000001");
        uAdmin.setPasswordHash(encPass);
        uAdmin.setGender("Nam");
        uAdmin.setBirthDate(LocalDate.of(1995, 5, 15));
        uAdmin.setHeightCm(175);
        uAdmin.setWeightKg(70);
        uAdmin.setMembershipTier("PLATINUM");
        uAdmin.setCoolcashBalance(BigDecimal.valueOf(500000));
        uAdmin.setTotalSpent(BigDecimal.valueOf(10000000));
        uAdmin.setIsActive(true);
        uAdmin.getRoles().add(adminRole);
        uAdmin.getRoles().add(staffRole);
        userRepository.save(uAdmin);

        User uStaff = new User();
        uStaff.setFullName("Nhân Viên Kho Quận 12");
        uStaff.setEmail("staff@coolmate.me");
        uStaff.setPhone("0901000002");
        uStaff.setPasswordHash(encPass);
        uStaff.setGender("Nam");
        uStaff.setBirthDate(LocalDate.of(1998, 8, 20));
        uStaff.setHeightCm(170);
        uStaff.setWeightKg(65);
        uStaff.setMembershipTier("SILVER");
        uStaff.setCoolcashBalance(BigDecimal.valueOf(50000));
        uStaff.setTotalSpent(BigDecimal.valueOf(1200000));
        uStaff.setIsActive(true);
        uStaff.getRoles().add(staffRole);
        userRepository.save(uStaff);

        User uCustomer = new User();
        uCustomer.setFullName("Nguyễn Minh Tuấn");
        uCustomer.setEmail("tuan.nguyen@gmail.com");
        uCustomer.setPhone("0988111222");
        uCustomer.setPasswordHash(encPass);
        uCustomer.setGender("Nam");
        uCustomer.setBirthDate(LocalDate.of(1999, 11, 10));
        uCustomer.setHeightCm(173);
        uCustomer.setWeightKg(68);
        uCustomer.setMembershipTier("GOLD");
        uCustomer.setCoolcashBalance(BigDecimal.valueOf(85000));
        uCustomer.setTotalSpent(BigDecimal.valueOf(3450000));
        uCustomer.setIsActive(true);
        uCustomer.getRoles().add(customerRole);
        userRepository.save(uCustomer);

        // 7. Products (8 Products)
        // P1: Áo Thun Cotton Compact
        Product p1 = new Product();
        p1.setCategory(catTee);
        p1.setCollection(colCareShare);
        p1.setName("Áo Thun Nam Cotton Compact In Logo Care & Share");
        p1.setSlug("ao-thun-nam-cotton-compact-premium");
        p1.setShortDescription("Áo thun quốc dân chống nhăn, không xù lông sau 50 lần giặt, mềm mại vượt trội.");
        p1.setDescription("Được dệt từ sợi Cotton Compact 100% chải kỹ giúp bề mặt vải mịn màng, hạn chế tối đa tình trạng đổ lông và bai dão cổ áo.");
        p1.setMaterial("95% Cotton Compact, 5% Spandex");
        p1.setFitType("Regular Fit");
        p1.setFeatures("Co giãn 4 chiều, Thấm hút mồ hôi, Hạn chế xù lông");
        p1.setBasePrice(BigDecimal.valueOf(249000));
        p1.setRatingAvg(BigDecimal.valueOf(4.90));
        p1.setReviewCount(128);
        p1.setSoldCount(15420);
        p1.setStatus("ACTIVE");
        p1 = productRepository.save(p1);

        productImageRepository.save(new ProductImage(p1, cBlack, "https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/ao-thun-compact-den-1.jpg", true, 1));
        productImageRepository.save(new ProductImage(p1, cWhite, "https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/ao-thun-compact-trang-1.jpg", false, 2));
        productImageRepository.save(new ProductImage(p1, cNavy, "https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/ao-thun-compact-navy-1.jpg", false, 3));

        productVariantRepository.save(new ProductVariant(p1, cBlack, sM, "CM-TS01-BLK-M", BigDecimal.valueOf(299000), BigDecimal.valueOf(249000), 45, 220, true));
        productVariantRepository.save(new ProductVariant(p1, cBlack, sL, "CM-TS01-BLK-L", BigDecimal.valueOf(299000), BigDecimal.valueOf(249000), 80, 230, true));
        productVariantRepository.save(new ProductVariant(p1, cBlack, sXL, "CM-TS01-BLK-XL", BigDecimal.valueOf(299000), BigDecimal.valueOf(249000), 60, 240, true));
        productVariantRepository.save(new ProductVariant(p1, cWhite, sL, "CM-TS01-WHT-L", BigDecimal.valueOf(299000), BigDecimal.valueOf(249000), 70, 230, true));
        productVariantRepository.save(new ProductVariant(p1, cNavy, sL, "CM-TS01-NVY-L", BigDecimal.valueOf(299000), BigDecimal.valueOf(249000), 65, 230, true));

        // P2: Áo Polo Excool
        Product p2 = new Product();
        p2.setCategory(catPolo);
        p2.setCollection(colExcool);
        p2.setName("Áo Polo Nam Công Nghệ Excool Trơn Khử Mùi");
        p2.setSlug("ao-polo-nam-excool-khu-mui");
        p2.setShortDescription("Áo Polo nhẹ chỉ 160g, mát lạnh tức thì, khô nhanh gấp 2 lần áo Polo thông thường.");
        p2.setDescription("Ứng dụng công nghệ sợi Sorona thực vật đàn hồi tự nhiên, chống tia UV UPF 50+.");
        p2.setMaterial("56% Polyester, 44% Sorona (Excool)");
        p2.setFitType("Regular Fit");
        p2.setFeatures("Chống tia UV UPF50+, Nhanh khô, Không nhăn");
        p2.setBasePrice(BigDecimal.valueOf(349000));
        p2.setRatingAvg(BigDecimal.valueOf(5.00));
        p2.setReviewCount(94);
        p2.setSoldCount(9850);
        p2.setStatus("ACTIVE");
        p2 = productRepository.save(p2);

        productImageRepository.save(new ProductImage(p2, cNavy, "https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/polo-excool-navy-1.jpg", true, 1));
        productImageRepository.save(new ProductImage(p2, cBlack, "https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/polo-excool-den-1.jpg", false, 2));

        productVariantRepository.save(new ProductVariant(p2, cNavy, sL, "CM-PL02-NVY-L", BigDecimal.valueOf(399000), BigDecimal.valueOf(349000), 90, 190, true));
        productVariantRepository.save(new ProductVariant(p2, cBlack, sL, "CM-PL02-BLK-L", BigDecimal.valueOf(399000), BigDecimal.valueOf(349000), 75, 190, true));

        // P3: Áo Singlet ProMax
        Product p3 = new Product();
        p3.setCategory(catSinglet);
        p3.setCollection(colRunning);
        p3.setName("Áo Singlet Chạy Bộ Nam ProMax-S1 Siêu Nhẹ");
        p3.setSlug("ao-singlet-chay-bo-nam-promax-s1");
        p3.setShortDescription("Trọng lượng siêu nhẹ chỉ 75g, đường may ép nhiệt chống cọ xát.");
        p3.setDescription("Thiết kế chuyên biệt cho vận động viên chạy marathon.");
        p3.setMaterial("100% Polyester Recycled Quick-Dry");
        p3.setFitType("Athletic Fit");
        p3.setFeatures("Siêu nhẹ 75g, Thoáng khí tối đa");
        p3.setBasePrice(BigDecimal.valueOf(169000));
        p3.setRatingAvg(BigDecimal.valueOf(4.85));
        p3.setReviewCount(52);
        p3.setSoldCount(6230);
        p3.setStatus("ACTIVE");
        p3 = productRepository.save(p3);

        productImageRepository.save(new ProductImage(p3, cAqua, "https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/singlet-promax-xanh-1.jpg", true, 1));
        productVariantRepository.save(new ProductVariant(p3, cAqua, sL, "CM-SG03-AQU-L", BigDecimal.valueOf(199000), BigDecimal.valueOf(169000), 50, 95, true));

        // P4: Áo Khoác Daily Wear
        Product p4 = new Product();
        p4.setCategory(catJacket);
        p4.setCollection(colEveryday);
        p4.setName("Áo Khoác Nam Có Mũ Daily Wear Trượt Nước Chống UV");
        p4.setSlug("ao-khoac-nam-daily-wear-truot-nuoc");
        p4.setShortDescription("Áo khoác gió 1 lớp mỏng nhẹ, trượt nước mưa nhẹ và cản gió, chống nắng UPF 50+.");
        p4.setDescription("Lớp phủ DWR (Durable Water Repellent) giúp hạt nước trượt trên bề mặt.");
        p4.setMaterial("100% Polyester phủ trượt nước DWR");
        p4.setFitType("Regular Fit");
        p4.setFeatures("Trượt nước DWR, Chống nắng UPF50+, Gấp gọn tiện lợi");
        p4.setBasePrice(BigDecimal.valueOf(429000));
        p4.setRatingAvg(BigDecimal.valueOf(4.95));
        p4.setReviewCount(88);
        p4.setSoldCount(7400);
        p4.setStatus("ACTIVE");
        p4 = productRepository.save(p4);

        productImageRepository.save(new ProductImage(p4, cBlack, "https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/ao-khoac-daily-den-1.jpg", true, 1));
        productVariantRepository.save(new ProductVariant(p4, cBlack, sL, "CM-JK04-BLK-L", BigDecimal.valueOf(499000), BigDecimal.valueOf(429000), 40, 320, true));

        // P5: Quần Short 5 Inch
        Product p5 = new Product();
        p5.setCategory(catShort);
        p5.setCollection(colRunning);
        p5.setName("Quần Short Nam Chạy Bộ 5 Inch Advanced Running");
        p5.setSlug("quan-short-nam-chay-bo-5-inch");
        p5.setShortDescription("Quần short chạy bộ 2 lớp có lót đùi chống ma sát, tích hợp túi khóa kéo.");
        p5.setDescription("Độ dài 5 inch chuẩn chạy bộ giải phóng hoàn toàn chuyển động của đùi.");
        p5.setMaterial("92% Polyester, 8% Spandex");
        p5.setFitType("Athletic Fit");
        p5.setFeatures("2 lớp chống cọ đùi, Có túi khóa kéo sau lưng");
        p5.setBasePrice(BigDecimal.valueOf(259000));
        p5.setRatingAvg(BigDecimal.valueOf(4.90));
        p5.setReviewCount(140);
        p5.setSoldCount(11200);
        p5.setStatus("ACTIVE");
        p5 = productRepository.save(p5);

        productImageRepository.save(new ProductImage(p5, cBlack, "https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/short-running-5inch-den-1.jpg", true, 1));
        productVariantRepository.save(new ProductVariant(p5, cBlack, sL, "CM-SH05-BLK-L", BigDecimal.valueOf(299000), BigDecimal.valueOf(259000), 85, 170, true));

        // P6: Quần Dài UT Pants
        Product p6 = new Product();
        p6.setCategory(catUTPants);
        p6.setCollection(colEveryday);
        p6.setName("Quần Dài Nam UT Pants Đa Năng Co Giãn 4 Chiều");
        p6.setSlug("quan-dai-nam-ut-pants-da-nang");
        p6.setShortDescription("Chiếc quần dài mặc đi làm, đi chơi hay đi du lịch đều lịch sự và thoải mái.");
        p6.setDescription("Sợi Nylon kết hợp Spandex co giãn 4 chiều, đầu gối không bị phồng.");
        p6.setMaterial("88% Nylon (Polyamide), 12% Spandex");
        p6.setFitType("Slim Fit");
        p6.setFeatures("Co giãn 4 chiều, Trượt nước nhẹ, Không nhăn");
        p6.setBasePrice(BigDecimal.valueOf(449000));
        p6.setRatingAvg(BigDecimal.valueOf(4.80));
        p6.setReviewCount(67);
        p6.setSoldCount(5310);
        p6.setStatus("ACTIVE");
        p6 = productRepository.save(p6);

        productImageRepository.save(new ProductImage(p6, cBlack, "https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/quan-ut-pants-den-1.jpg", true, 1));
        productVariantRepository.save(new ProductVariant(p6, cBlack, sL, "CM-PT06-BLK-L", BigDecimal.valueOf(499000), BigDecimal.valueOf(449000), 55, 350, true));

        // P7: Combo 3 Quần Lót Trunk Bamboo
        Product p7 = new Product();
        p7.setCategory(catTrunk);
        p7.setCollection(colEveryday);
        p7.setName("Combo 3 Quần Lót Nam Trunk Bamboo Kháng Khuẩn");
        p7.setSlug("combo-3-quan-lot-nam-trunk-bamboo");
        p7.setShortDescription("Vải sợi tre tự nhiên mềm mịn gấp 2 lần Cotton, kháng khuẩn và khử mùi tự nhiên.");
        p7.setDescription("Đai lưng dệt Microfiber siêu êm không để lại vết hằn trên da.");
        p7.setMaterial("95% Sợi tre tự nhiên (Bamboo), 5% Spandex");
        p7.setFitType("Slim Fit");
        p7.setFeatures("Kháng khuẩn tự nhiên, Mềm mượt mát lạnh, Đai lưng chống hằn");
        p7.setBasePrice(BigDecimal.valueOf(259000));
        p7.setRatingAvg(BigDecimal.valueOf(5.00));
        p7.setReviewCount(310);
        p7.setSoldCount(28900);
        p7.setStatus("ACTIVE");
        p7 = productRepository.save(p7);

        productImageRepository.save(new ProductImage(p7, cBlack, "https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/boxer-bamboo-den-1.jpg", true, 1));
        productVariantRepository.save(new ProductVariant(p7, cBlack, sL, "CM-UN07-BLK-L", BigDecimal.valueOf(289000), BigDecimal.valueOf(259000), 120, 210, true));

        // P8: Combo 3 Đôi Tất Nam
        Product p8 = new Product();
        p8.setCategory(catSocks);
        p8.setCollection(colRunning);
        p8.setName("Combo 3 Đôi Tất Nam Cổ Ngắn Thể Thao Đệm Êm Chân");
        p8.setSlug("combo-3-doi-tat-nam-co-ngan-the-thao");
        p8.setShortDescription("Tất thể thao dệt đệm xù phần gót và mũi chân giúp giảm chấn.");
        p8.setDescription("Công nghệ kháng khuẩn ion bạc ngăn mùi hôi chân hiệu quả.");
        p8.setMaterial("80% Cotton Organic, 17% Spandex, 3% Rubber");
        p8.setFitType("Regular Fit");
        p8.setFeatures("Đệm gót chống phồng rộp, Lưới thoáng khí mu bàn chân");
        p8.setBasePrice(BigDecimal.valueOf(99000));
        p8.setRatingAvg(BigDecimal.valueOf(4.90));
        p8.setReviewCount(215);
        p8.setSoldCount(19400);
        p8.setStatus("ACTIVE");
        p8 = productRepository.save(p8);

        productImageRepository.save(new ProductImage(p8, cWhite, "https://media3.coolmate.me/cdn-cgi/image/quality=85/uploads/2024/tat-the-thao-trang-1.jpg", true, 1));
        productVariantRepository.save(new ProductVariant(p8, cWhite, sL, "CM-SK08-WHT-L", BigDecimal.valueOf(119000), BigDecimal.valueOf(99000), 150, 120, true));

        // 8. Promotions
        Promotion promo1 = new Promotion();
        promo1.setCode("COOLNEW50");
        promo1.setName("Giảm 50.000đ cho đơn hàng đầu tiên từ 299.000đ");
        promo1.setDiscountType("FIXED_AMOUNT");
        promo1.setDiscountValue(BigDecimal.valueOf(50000));
        promo1.setMaxDiscountAmount(BigDecimal.valueOf(50000));
        promo1.setMinOrderValue(BigDecimal.valueOf(299000));
        promo1.setUsageLimit(1000);
        promo1.setUsedCount(12);
        promo1.setStartDate(LocalDateTime.now().minusDays(30));
        promo1.setEndDate(LocalDateTime.now().plusDays(180));
        promo1.setIsActive(true);
        promotionRepository.save(promo1);

        // 9. Combo Rules
        ComboRule rule1 = new ComboRule();
        rule1.setName("Mix & Match: Mua từ 3 Áo Thun Nam giảm thêm 10%");
        rule1.setCategory(catTee);
        rule1.setMinQuantity(3);
        rule1.setDiscountPercentage(BigDecimal.valueOf(10.00));
        rule1.setIsActive(true);
        comboRuleRepository.save(rule1);

        System.out.println(">>> DataInitializer: Đã khởi tạo thành công dữ liệu mẫu cho CoolMateShop!");
    }
}
