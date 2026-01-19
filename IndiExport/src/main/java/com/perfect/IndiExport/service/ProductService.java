package com.perfect.IndiExport.service;

import com.perfect.IndiExport.dto.ProductDto;
import com.perfect.IndiExport.entity.Buyer;
import com.perfect.IndiExport.entity.Product;
import com.perfect.IndiExport.entity.ProductSellingCountry;
import com.perfect.IndiExport.entity.ProductViewTracking;
import com.perfect.IndiExport.entity.Seller;
import com.perfect.IndiExport.entity.User;
import com.perfect.IndiExport.repository.BuyerRepository;
import com.perfect.IndiExport.repository.ProductRepository;
import com.perfect.IndiExport.repository.ProductSellingCountryRepository;
import com.perfect.IndiExport.repository.ProductViewTrackingRepository;
import com.perfect.IndiExport.repository.SellerRepository;
import com.perfect.IndiExport.util.CountryUtil;
import com.perfect.IndiExport.util.CurrencyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final SellerRepository sellerRepository;
    private final ProductSellingCountryRepository sellingCountryRepository;
    private final BuyerRepository buyerRepository;
    private final ProductViewTrackingRepository viewTrackingRepository;
    private final EntityManager entityManager;

    @Transactional
    public ProductDto addProduct(User user, ProductDto dto) {
        Seller seller = sellerRepository.findById(user.getId())
                .orElseThrow(() -> new RuntimeException("Seller profile not found"));

        // Check limits for BASIC sellers
        if ("BASIC".equals(seller.getSellerMode())) {
            long productCount = productRepository.countBySellerId(seller.getId());
            if (productCount >= 5) {
                throw new RuntimeException(
                        "Product limit reached for BASIC sellers. Upgrade to ADVANCED for unlimited listings.");
            }
        }

        Product product = Product.builder()
                .seller(seller)
                .name(dto.getName())
                .category(dto.getCategory())
                .price(dto.getPrice())
                .minQuantity(dto.getMinQuantity())
                .description(dto.getDescription())
                .imageUrl(dto.getImageUrl())
                .status(dto.getStatus() != null ? dto.getStatus() : "ACTIVE")
                .declaredStock(dto.getDeclaredStock() != null ? dto.getDeclaredStock() : 0)
                .reservedStock(0) // Always start with 0 reserved
                .allowDirectBuy(dto.getAllowDirectBuy() != null ? dto.getAllowDirectBuy() : false)
                .build();

        Product saved = productRepository.save(product);

        // Save selling countries
        if (dto.getSellingCountries() != null && !dto.getSellingCountries().isEmpty()) {
            // Remove duplicates and filter empty values
            Set<String> uniqueCountryCodes = dto.getSellingCountries().stream()
                    .filter(code -> code != null && !code.trim().isEmpty())
                    .map(code -> code.toUpperCase().trim())
                    .collect(Collectors.toSet());

            List<ProductSellingCountry> countries = new ArrayList<>();
            for (String countryCode : uniqueCountryCodes) {
                ProductSellingCountry psc = ProductSellingCountry.builder()
                        .product(saved)
                        .countryCode(countryCode)
                        .countryName(CountryUtil.getCountryName(countryCode))
                        .build();
                countries.add(psc);
            }

            if (!countries.isEmpty()) {
                sellingCountryRepository.saveAll(countries);
                saved.setSellingCountries(countries);
            }
        }

        return mapToDto(saved);
    }

    public List<ProductDto> getSellerProducts(User user) {
        return productRepository.findBySellerId(user.getId())
                .stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public ProductDto updateProduct(User user, Long productId, ProductDto dto) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Verify product belongs to seller
        if (!product.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to update this product");
        }

        // Update fields
        if (dto.getName() != null)
            product.setName(dto.getName());
        if (dto.getCategory() != null)
            product.setCategory(dto.getCategory());
        if (dto.getPrice() != null)
            product.setPrice(dto.getPrice());
        if (dto.getMinQuantity() != null)
            product.setMinQuantity(dto.getMinQuantity());
        if (dto.getDescription() != null)
            product.setDescription(dto.getDescription());
        if (dto.getImageUrl() != null)
            product.setImageUrl(dto.getImageUrl());
        if (dto.getStatus() != null)
            product.setStatus(dto.getStatus());
        if (dto.getDeclaredStock() != null)
            product.setDeclaredStock(dto.getDeclaredStock());
        if (dto.getAllowDirectBuy() != null)
            product.setAllowDirectBuy(dto.getAllowDirectBuy());

        // Update selling countries
        if (dto.getSellingCountries() != null) {
            // Remove duplicates and filter empty values
            Set<String> uniqueCountryCodes = dto.getSellingCountries().stream()
                    .filter(code -> code != null && !code.trim().isEmpty())
                    .map(code -> code.toUpperCase().trim())
                    .collect(Collectors.toSet());

            // Get existing countries for this product
            List<ProductSellingCountry> existingCountries = sellingCountryRepository.findByProductId(productId);
            Set<String> existingCountryCodes = existingCountries.stream()
                    .map(ProductSellingCountry::getCountryCode)
                    .collect(Collectors.toSet());

            // Find countries to delete (exist in DB but not in new list)
            List<ProductSellingCountry> toDelete = existingCountries.stream()
                    .filter(country -> !uniqueCountryCodes.contains(country.getCountryCode()))
                    .collect(Collectors.toList());

            // Find countries to add (in new list but not in DB)
            Set<String> toAdd = uniqueCountryCodes.stream()
                    .filter(code -> !existingCountryCodes.contains(code))
                    .collect(Collectors.toSet());

            // Delete countries that are no longer needed
            if (!toDelete.isEmpty()) {
                sellingCountryRepository.deleteAll(toDelete);
            }

            // Add new countries
            if (!toAdd.isEmpty()) {
                List<ProductSellingCountry> newCountries = new ArrayList<>();
                for (String countryCode : toAdd) {
                    ProductSellingCountry psc = ProductSellingCountry.builder()
                            .product(product)
                            .countryCode(countryCode)
                            .countryName(CountryUtil.getCountryName(countryCode))
                            .build();
                    newCountries.add(psc);
                }
                sellingCountryRepository.saveAll(newCountries);
            }

            // Flush to ensure all changes are persisted
            entityManager.flush();

            // Refresh product to get updated countries list
            product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
        }

        Product updated = productRepository.save(product);
        return mapToDto(updated);
    }

    @Transactional
    public void deleteProduct(User user, Long productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Verify product belongs to seller
        if (!product.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to delete this product");
        }

        productRepository.delete(product);
    }

    @Transactional
    public void reserveStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        int remaining = product.getRemainingStock();
        if (remaining < quantity) {
            throw new RuntimeException("Insufficient stock. Available: " + remaining + ", Requested: " + quantity);
        }

        product.setReservedStock(product.getReservedStock() + quantity);
        productRepository.save(product);
    }

    @Transactional
    public void releaseStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        int currentReserved = product.getReservedStock();
        int newReserved = Math.max(0, currentReserved - quantity);
        product.setReservedStock(newReserved);
        productRepository.save(product);
    }

    @Transactional
    public void deductStock(Long productId, Integer quantity) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Deduct from both declared and reserved
        product.setDeclaredStock(Math.max(0, product.getDeclaredStock() - quantity));
        product.setReservedStock(Math.max(0, product.getReservedStock() - quantity));
        productRepository.save(product);
    }

    // Buyer methods - Get products available for buyer's country
    // Buyer methods - Get products available for buyer's country
    public List<ProductDto> getProductsForBuyer(
            User buyerUser,
            String category,
            String searchTerm) {

        // ================= GUEST USER =================
        if (buyerUser == null) {

            List<Product> products = productRepository.findAll().stream()
                    .filter(p -> "ACTIVE".equals(p.getStatus()))
                    .collect(Collectors.toList());

            // Apply category filter
            if (category != null && !category.isEmpty()) {
                products = products.stream()
                        .filter(p -> category.equalsIgnoreCase(p.getCategory()))
                        .collect(Collectors.toList());
            }

            // Apply search filter
            if (searchTerm != null && !searchTerm.isEmpty()) {
                String lowerSearch = searchTerm.toLowerCase();
                products = products.stream()
                        .filter(p -> p.getName().toLowerCase().contains(lowerSearch) ||
                                (p.getDescription() != null &&
                                        p.getDescription().toLowerCase().contains(lowerSearch)))
                        .collect(Collectors.toList());
            }

            // Guest → normal DTO mapping (NO buyer logic)
            return products.stream()
                    .map(this::mapToDto) // 👈 your normal mapper
                    .collect(Collectors.toList());
        }

        // ================= LOGGED-IN BUYER =================

        // Get buyer profile
        Buyer buyer = buyerRepository.findByUserId(buyerUser.getId())
                .orElseThrow(() -> new RuntimeException("Buyer profile not found. Please complete your profile."));

        String buyerCountry = buyer.getCountry();
        if (buyerCountry == null || buyerCountry.isEmpty()) {
            throw new RuntimeException("Buyer country not set. Please update your profile.");
        }

        // Get active products
        List<Product> allProducts = productRepository.findAll().stream()
                .filter(p -> "ACTIVE".equals(p.getStatus()))
                .collect(Collectors.toList());

        // Filter by buyer country
        List<Product> filteredProducts = allProducts.stream()
                .filter(product -> {
                    List<ProductSellingCountry> countries = sellingCountryRepository.findByProductId(product.getId());

                    return countries.stream()
                            .anyMatch(psc -> psc.getCountryCode().equalsIgnoreCase(buyerCountry));
                })
                .collect(Collectors.toList());

        // Apply category filter
        if (category != null && !category.isEmpty()) {
            filteredProducts = filteredProducts.stream()
                    .filter(p -> category.equalsIgnoreCase(p.getCategory()))
                    .collect(Collectors.toList());
        }

        // Apply search filter
        if (searchTerm != null && !searchTerm.isEmpty()) {
            String lowerSearch = searchTerm.toLowerCase();
            filteredProducts = filteredProducts.stream()
                    .filter(p -> p.getName().toLowerCase().contains(lowerSearch) ||
                            (p.getDescription() != null &&
                                    p.getDescription().toLowerCase().contains(lowerSearch)))
                    .collect(Collectors.toList());
        }

        // Buyer-specific DTO mapping (currency, wishlist, etc.)
        return filteredProducts.stream()
                .map(product -> mapToDtoForBuyer(product, buyer))
                .collect(Collectors.toList());
    }

    public ProductDto getProductDetailsForBuyer(User buyerUser, Long productId) {
        Buyer buyer = buyerRepository.findByUserId(buyerUser.getId())
                .orElseThrow(() -> new RuntimeException("Buyer profile not found"));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Product not found"));

        // Verify product is available in buyer's country
        String buyerCountry = buyer.getCountry();
        List<ProductSellingCountry> countries = sellingCountryRepository.findByProductId(productId);
        boolean isAvailable = countries.stream()
                .anyMatch(psc -> psc.getCountryCode().equalsIgnoreCase(buyerCountry));

        if (!isAvailable) {
            throw new RuntimeException("This product is not available in your country");
        }

        // Track product view
        trackProductView(productId, buyerUser.getId());

        return mapToDtoForBuyer(product, buyer);
    }

    @Transactional
    private void trackProductView(Long productId, Long buyerId) {
        // Check if already viewed today
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        LocalDateTime endOfDay = LocalDateTime.now().withHour(23).withMinute(59).withSecond(59);

        boolean alreadyViewed = viewTrackingRepository.existsByProductIdAndBuyerIdAndViewDateBetween(
                productId, buyerId, startOfDay, endOfDay);

        if (!alreadyViewed) {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new RuntimeException("Product not found"));
            User buyer = new User();
            buyer.setId(buyerId);

            ProductViewTracking tracking = ProductViewTracking.builder()
                    .product(product)
                    .buyer(buyer)
                    .viewDate(LocalDateTime.now())
                    .build();

            viewTrackingRepository.save(tracking);
        }
    }

    private ProductDto mapToDtoForBuyer(Product product, Buyer buyer) {
        ProductDto dto = mapToDto(product);

        // Add currency conversion
        String buyerCurrency = buyer.getCurrency() != null ? buyer.getCurrency() : "USD";
        BigDecimal convertedPrice = CurrencyUtil.convertFromINR(product.getPrice(), buyerCurrency);
        dto.setConvertedPrice(convertedPrice);
        dto.setCurrency(buyerCurrency);
        dto.setCurrencySymbol(CurrencyUtil.getCurrencySymbol(buyerCurrency));

        // Add seller info
        dto.setSellerId(product.getSeller().getId());
        dto.setSellerBusinessName(product.getSeller().getBusinessName());

        return dto;
    }

    private ProductDto mapToDto(Product product) {
        ProductDto dto = new ProductDto();
        dto.setId(product.getId());
        dto.setName(product.getName());
        dto.setCategory(product.getCategory());
        dto.setPrice(product.getPrice());
        dto.setMinQuantity(product.getMinQuantity());
        dto.setDescription(product.getDescription());
        dto.setImageUrl(product.getImageUrl());
        dto.setStatus(product.getStatus());
        dto.setDeclaredStock(product.getDeclaredStock());
        dto.setReservedStock(product.getReservedStock());
        dto.setRemainingStock(product.getRemainingStock());
        dto.setStockStatus(product.getStockStatus());
        dto.setAllowDirectBuy(product.getAllowDirectBuy());

        // Fetch and map selling countries
        List<ProductSellingCountry> countries = sellingCountryRepository.findByProductId(product.getId());
        List<String> countryCodes = countries.stream()
                .map(ProductSellingCountry::getCountryCode)
                .collect(Collectors.toList());
        dto.setSellingCountries(countryCodes);

        return dto;
    }
}
