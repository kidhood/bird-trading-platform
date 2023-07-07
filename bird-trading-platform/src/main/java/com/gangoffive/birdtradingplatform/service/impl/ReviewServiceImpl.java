package com.gangoffive.birdtradingplatform.service.impl;

import com.gangoffive.birdtradingplatform.api.response.ErrorResponse;
import com.gangoffive.birdtradingplatform.common.PagingAndSorting;
import com.gangoffive.birdtradingplatform.config.AppProperties;
import com.gangoffive.birdtradingplatform.dto.AccountDto;
import com.gangoffive.birdtradingplatform.dto.AccountReviewDto;
import com.gangoffive.birdtradingplatform.dto.OrderDetailShopOwnerDto;
import com.gangoffive.birdtradingplatform.dto.ReviewDto;
import com.gangoffive.birdtradingplatform.entity.Account;
import com.gangoffive.birdtradingplatform.entity.OrderDetail;
import com.gangoffive.birdtradingplatform.entity.Review;
import com.gangoffive.birdtradingplatform.enums.ReviewRating;
import com.gangoffive.birdtradingplatform.enums.SortOrderDetailColumn;
import com.gangoffive.birdtradingplatform.mapper.AccountMapper;
import com.gangoffive.birdtradingplatform.repository.AccountRepository;
import com.gangoffive.birdtradingplatform.repository.OrderDetailRepository;
import com.gangoffive.birdtradingplatform.repository.ReviewRepository;
import com.gangoffive.birdtradingplatform.service.ProductSummaryService;
import com.gangoffive.birdtradingplatform.service.ReviewService;
import com.gangoffive.birdtradingplatform.util.FileNameUtils;
import com.gangoffive.birdtradingplatform.util.S3Utils;
import com.gangoffive.birdtradingplatform.wrapper.PageNumberWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReviewServiceImpl implements ReviewService {
    private final ReviewRepository reviewRepository;
    private final AccountRepository accountRepository;
    private final OrderDetailRepository orderDetailRepository;
    private final AppProperties appProperties;
    private final ProductSummaryService productSummaryService;
    private final AccountMapper accountMapper;

    @Override
    public ResponseEntity<?> getAllReviewByOrderId(Long orderId) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<Account> account = accountRepository.findByEmail(authentication.getName());
        Optional<List<Review>> reviews = reviewRepository.findAllByAccountAndOrderDetail_Order_Id(account.get(), orderId);
        if (reviews.isPresent() && reviews.get().size() > 0) {
            return ResponseEntity.ok(reviews.get().stream().map(this::reviewToReviewDto).collect(Collectors.toList()));
        }
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                .errorMessage("Not found reviews of this order id")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @Override
    public ResponseEntity<?> addNewReviewByOrderDetailId(List<MultipartFile> multipartFiles, ReviewDto review) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Optional<Account> account = accountRepository.findByEmail(authentication.getName());
        Optional<OrderDetail> orderDetails = orderDetailRepository.findByIdAndOrder_PackageOrder_Account(review.getOrderDetailId(), account.get());
        log.info("review.getId(), {}", review.getId());
        log.info("account.get() {}", account.get().getEmail());
        log.info("orderDetails.isPresent() {}", orderDetails.isPresent());
        if (orderDetails.isPresent()) {
            Review reviewSave = new Review();
            reviewSave.setOrderDetail(orderDetails.get());
            reviewSave.setAccount(account.get());
            reviewSave.setComment(review.getDescription());
            reviewSave.setRating(ReviewRating.getReviewRatingByStar(review.getRating()));

            String originUrl = appProperties.getS3().getUrl();
            List<String> urlImgList = new ArrayList<>();
            if (multipartFiles != null && !multipartFiles.isEmpty()) {
                for (MultipartFile multipartFile : multipartFiles) {
                    String newFilename = FileNameUtils.getNewImageFileName(multipartFile);
                    urlImgList.add(originUrl + newFilename);
                    try {
                        S3Utils.uploadFile(newFilename, multipartFile.getInputStream());
                    } catch (Exception ex) {
                        ErrorResponse errorResponse = ErrorResponse.builder()
                                .errorCode(String.valueOf(HttpStatus.BAD_REQUEST.value()))
                                .errorMessage("Upload file fail")
                                .build();
                        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
                    }
                }
            }
            String imgUrl = urlImgList.stream()
                    .collect(Collectors.joining(","));
            reviewSave.setImgUrl(imgUrl);
            Review save = reviewRepository.save(reviewSave);
            productSummaryService.updateReviewTotal(save.getOrderDetail().getProduct());
            productSummaryService.updateProductStar(save.getOrderDetail().getProduct());
            return ResponseEntity.ok(reviewToReviewDto(save));
        }
        ErrorResponse errorResponse = ErrorResponse.builder()
                .errorCode(String.valueOf(HttpStatus.NOT_FOUND.value()))
                .errorMessage("Not found reviews of this order detail id")
                .build();
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @Override
    public ReviewDto reviewToReviewDto(Review review) {
        Account account = review.getAccount();
        AccountReviewDto accountReview = AccountReviewDto.builder()
                .id(account.getId())
                .fullName(account.getFullName())
                .imgUrl(account.getImgUrl())
                .build();
        if (!review.getImgUrl().isEmpty()) {
            return ReviewDto.builder()
                    .id(review.getId())
                    .account(accountReview)
                    .orderDetailId(review.getOrderDetail().getId())
                    .productId(review.getOrderDetail().getProduct().getId())
                    .description(review.getComment())
                    .rating(review.getRating().getStar())
                    .imgUrl(Arrays.asList(review.getImgUrl().split(",")))
                    .reviewDate(review.getReviewDate().getTime())
                    .build();
        }
        return ReviewDto.builder()
                .id(review.getId())
                .account(accountReview)
                .orderDetailId(review.getOrderDetail().getId())
                .productId(review.getOrderDetail().getProduct().getId())
                .description(review.getComment())
                .rating(review.getRating().getStar())
                .reviewDate(review.getReviewDate().getTime())
                .build();
    }

    @Override
    public ResponseEntity<?> getAllReviewByProductId(Long productId, int pageNumber) {
        if (pageNumber > 0) {
            pageNumber--;
            PageRequest pageRequest = PageRequest.of(
                    pageNumber,
                    PagingAndSorting.DEFAULT_PAGE_SIZE,
                    Sort.by(Sort.Direction.DESC, "reviewDate")
            );
            Optional<Page<Review>> reviews = reviewRepository.findAllByOrderDetail_Product_Id(productId, pageRequest);
            return getPageNumberWrapperWithReviews(reviews);
        }
        ErrorResponse error = new ErrorResponse(HttpStatus.BAD_REQUEST.toString(),
                "Page number cannot less than 1");
        return new ResponseEntity<>(error, HttpStatus.BAD_REQUEST);
    }

    private ResponseEntity<PageNumberWrapper<?>> getPageNumberWrapperWithReviews(Optional<Page<Review>> reviews) {
        List<ReviewDto> reviewList = reviews.get().stream()
                .map(this::reviewToReviewDto)
                .toList();
        PageNumberWrapper<ReviewDto> result = new PageNumberWrapper<>(
                reviewList,
                reviews.get().getTotalPages(),
                reviews.get().getTotalElements()
        );
        return ResponseEntity.ok(result);
    }
}
