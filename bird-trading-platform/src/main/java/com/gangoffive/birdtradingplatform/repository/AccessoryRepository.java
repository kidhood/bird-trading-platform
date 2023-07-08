
/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Interface.java to edit this template
 */
package com.gangoffive.birdtradingplatform.repository;

import com.gangoffive.birdtradingplatform.entity.Accessory;
import com.gangoffive.birdtradingplatform.entity.Product;
import com.gangoffive.birdtradingplatform.entity.ShopOwner;
import com.gangoffive.birdtradingplatform.entity.Tag;
import com.gangoffive.birdtradingplatform.enums.ProductStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AccessoryRepository extends JpaRepository<Accessory, Long> {
    Optional<List<Accessory>> findByNameLikeAndStatusInAndQuantityGreaterThanEqual(String name, List<ProductStatus> productStatuses, int quantity);

    @Query(value = "SELECT a.product_id " +
            "FROM `bird-trading-platform`.tbl_accessory a " +
            "INNER JOIN `bird-trading-platform`.tbl_product_summary ps " +
            "ON a.product_id= ps.product_id " +
            "WHERE (MATCH(a.name) AGAINST (?1 IN NATURAL LANGUAGE MODE) OR a.name LIKE %?1%)" +
            "AND (COALESCE(?2, a.type_id) = a.type_id OR ?2 IS NULL) " +
            "And ps.star >= ?3 " +
            "And ps.discounted_price >= ?4 " +
            "And ps.discounted_price <= ?5 " +
            "And a.status = 'ACTIVE' " +
            "And a.quantity > 0 ", nativeQuery = true)
    Page<Long> idFilter(String name, List<Long> listTypeId, double star,
                        double lowestPrice, double highestPrice, Pageable pageable);

    Page<Accessory> findAllByQuantityGreaterThanAndStatusIn(int quantity, List<ProductStatus> productStatuses, Pageable pageable);

    Optional<Page<Product>> findByShopOwner_Id(long id, Pageable pageable);

    @Query(value = "SELECT a.product_id " +
            "FROM `bird-trading-platform`.tbl_accessory a " +
            "INNER JOIN `bird-trading-platform`.tbl_product_summary ps " +
            "ON a.product_id = ps.product_id " +
            "where a.shop_id = ?1 " +
            "And a.name LIKE %?2% " +
            "And a.type_id in (?3) " +
            "And ps.star >= ?4 " +
            "And ps.discounted_price >= ?5 " +
            "And ps.discounted_price <= ?6 " +
            "And a.quantity > 0 " +
            "And a.status = 'ACTIVE' ", nativeQuery = true)

    Page<Long> idFilterShop(Long idShop, String name, List<Long> listTypeId, double star,
                            double lowestPrice, double highestPrice, Pageable pageable);

    Optional<Page<Product>> findByShopOwner_IdAndStatusIn(long id, List<ProductStatus> productStatuses, Pageable pageable);

    Optional<Page<Accessory>> findAllByShopOwner_IdAndStatusIn(
            Long shopId, List<ProductStatus> productStatuses, Pageable pageable
    );


    Optional<Page<Accessory>> findByIdAndShopOwner_IdAndStatusIn(
            Long accessoryId, Long shopId, List<ProductStatus> productStatuses, Pageable pageable
    );

    Optional<Page<Accessory>> findAllByNameLikeAndShopOwner_IdAndStatusIn(
            String name, Long shopId, List<ProductStatus> productStatuses, Pageable pageable
    );

    Optional<Page<Accessory>> findAllByShopOwner_IdAndTypeAccessory_IdInAndStatusIn(
            Long shopOwnerId, List<Long> typeAccessoryIds, List<ProductStatus> productStatuses, Pageable pageable
    );


    Optional<Page<Accessory>> findAllByShopOwner_IdAndPriceGreaterThanEqualAndStatusIn(
            Long shopOwnerId, double price, List<ProductStatus> productStatuses, Pageable pageable
    );

    Optional<Page<Accessory>> findAllByShopOwner_IdAndProductSummary_DiscountedPriceGreaterThanEqualAndStatusIn(
            Long shopOwnerId, double discountedPrice, List<ProductStatus> productStatuses, Pageable pageable
    );

    Optional<Page<Accessory>> findAllByShopOwner_IdAndStatus(
            Long shopOwnerId, ProductStatus productStatus, Pageable pageable
    );

    Accessory findByIdAndShopOwner(Long id, ShopOwner shopOwner);

    @Query("SELECT b FROM Accessory b WHERE b.id IN " +
            "(SELECT DISTINCT b2.id FROM Accessory b2 JOIN b2.productSummary ps JOIN b2.tags t " +
            "WHERE b2.typeAccessory.id = :typeId OR t.id IN :tagIds OR TRUE = TRUE AND b2.quantity > 0 AND b2.status IN :status) " +
            "ORDER BY b.productSummary.totalQuantityOrder DESC")
    List<Product> findDistinctBirdsByTypeAndTagsSortedByTotalQuantity(@Param("typeId") long typeId,
                                                                      @Param("tagIds") List<Long> tagIds,
                                                                      @Param("status") List<ProductStatus> statusList,
                                                                      Pageable pageable);
}
