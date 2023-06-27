package com.gangoffive.birdtradingplatform.dto;

import com.gangoffive.birdtradingplatform.enums.ProductStatus;
import lombok.*;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
public class ProductShopOwnerSearchInfoDto {
    private Long productId;
    private String productName;
    private String typeName;
    private Double lowestPrice;
    private Double lowestPriceOfDiscountedPrice;
    private ProductStatus productStatus;
}