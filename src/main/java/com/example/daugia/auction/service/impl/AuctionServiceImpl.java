package com.example.daugia.auction.service.impl;

import com.example.daugia.auction.dto.*;
import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionImage;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.mapper.AuctionMapper;
import com.example.daugia.auction.repository.AuctionImageRepository;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.auction.service.AuctionService;
import com.example.daugia.auction.specification.AuctionSpecification;
import com.example.daugia.category.entity.Category;
import com.example.daugia.category.repository.CategoryRepository;
import com.example.daugia.common.dto.PageResponse;
import com.example.daugia.common.audit.AuditAction;
import com.example.daugia.common.audit.AuditJsonUtils;
import com.example.daugia.common.audit.AuditOutcome;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.common.event.AuctionApprovedEvent;
import com.example.daugia.common.event.AuctionCreatedEvent;
import com.example.daugia.common.event.AuctionRejectedEvent;
import com.example.daugia.common.event.DomainEventPublisher;
import com.example.daugia.common.exception.AppException;
import com.example.daugia.common.exception.ResourceNotFoundException;
import com.example.daugia.common.storage.StorageService;
import com.example.daugia.common.storage.UploadResult;
import com.example.daugia.user.entity.User;
import com.example.daugia.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuctionServiceImpl implements AuctionService {

    private static final Set<String> VALID_SORT_FIELDS = Set.of("createdAt", "startingPrice", "biddingStartTime");

    private final AuctionRepository      auctionRepository;
    private final AuctionImageRepository auctionImageRepository;
    private final UserRepository         userRepository;
    private final CategoryRepository     categoryRepository;
    private final AuctionMapper          auctionMapper;
    private final StorageService         storageService;
    private final DomainEventPublisher   eventPublisher;
    private final AuditService           auditService;

    // ─── SELLER ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    @CacheEvict(value = "auctions", allEntries = true)
    public AuctionResponse create(AuctionCreateRequest request, List<MultipartFile> images, String sellerEmail) throws IOException {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found: " + sellerEmail));

        if (!request.getBiddingEndTime().isAfter(request.getBiddingStartTime())) {
            throw new AppException("End time must be after start time", HttpStatus.BAD_REQUEST);
        }
        if (request.getBuyNowPrice() != null
                && request.getBuyNowPrice().compareTo(request.getStartingPrice()) <= 0) {
            throw new AppException("Buy now price must be greater than starting price", HttpStatus.BAD_REQUEST);
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + request.getCategoryId()));

        Auction auction = Auction.builder()
                .productName(request.getProductName())
                .description(request.getDescription())
                .startingPrice(request.getStartingPrice())
                .bidIncrement(request.getBidIncrement())
                .buyNowPrice(request.getBuyNowPrice())
                .biddingStartTime(request.getBiddingStartTime())
                .biddingEndTime(request.getBiddingEndTime())
                .endTime(request.getBiddingEndTime())
                .status(AuctionStatus.PENDING)
                .seller(seller)
                .category(category)
                .build();

        Auction saved = auctionRepository.save(auction);

        // Upload images if any
        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                MultipartFile file = images.get(i);
                if (file.isEmpty()) continue;

                byte[] bytes = file.getBytes();
                UploadResult result = storageService.upload(bytes, file.getOriginalFilename(),
                        "auctions/" + saved.getId());

                AuctionImage image = AuctionImage.builder()
                        .auction(saved)
                        .imageUrl(result.url())
                        .publicId(result.publicId())
                        .sortOrder(i)
                        .build();
                AuctionImage savedImage = auctionImageRepository.save(image);
                saved.getImages().add(savedImage);
            }
        }

        eventPublisher.publish(new AuctionCreatedEvent(
                saved.getId(), saved.getProductName(), seller.getEmail()));
        auditService.log(sellerEmail, AuditAction.AUCTION_CREATED, "AUCTION", saved.getId(),
            AuditOutcome.SUCCESS,
            AuditJsonUtils.toJson("productName", saved.getProductName(), "seller", seller.getEmail()));

        return auctionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuctionSummaryResponse> getMyAuctions(String sellerEmail, int page, int size) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found: " + sellerEmail));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<Auction> resultPage = auctionRepository.findAllBySeller_Id(seller.getId(), pageable);
        return toSummaryPage(resultPage);
    }

    // ─── PUBLIC / BIDDER ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuctionSummaryResponse> searchPublic(AuctionFilterRequest filter, int page, int size) {
        Pageable pageable = buildPageable(filter, page, size);
        Page<Auction> resultPage = auctionRepository.findAll(buildPublicSpecification(filter), pageable);
        return toSummaryPage(resultPage);
    }

    @Override
    @Transactional(readOnly = true)
    public AuctionResponse getById(String id, String currentUserEmail, boolean isAdmin) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        if (isAdmin) {
            return auctionMapper.toResponse(auction);
        }

        // Seller can see their own auction regardless of status
        if (currentUserEmail != null
                && currentUserEmail.equals(auction.getSeller().getEmail())) {
            return auctionMapper.toResponse(auction);
        }

        // Public / bidder: only APPROVED, ACTIVE, ENDED — use cached public path
        if (auction.getStatus() == AuctionStatus.APPROVED
                || auction.getStatus() == AuctionStatus.ACTIVE
                || auction.getStatus() == AuctionStatus.ENDED) {
            return getByIdPublicCached(id);
        }

        throw new ResourceNotFoundException("Auction not found");
    }

    @Cacheable(value = "auctions", key = "#id + '-public'")
    @Transactional(readOnly = true)
    public AuctionResponse getByIdPublicCached(String id) {
        return auctionRepository.findById(id)
                .map(auctionMapper::toResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));
    }

    // ─── ADMIN ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuctionSummaryResponse> searchAdmin(AuctionFilterRequest filter, int page, int size) {
        Pageable pageable = buildPageable(filter, page, size);
        Page<Auction> resultPage = auctionRepository.findAll(buildAdminSpecification(filter), pageable);
        return toSummaryPage(resultPage);
    }

    @Override
    @Transactional
    @CacheEvict(value = "auctions", key = "#id + '-public'")
    public AuctionResponse review(String id, AuctionReviewRequest request, String adminEmail) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        if (auction.getStatus() != AuctionStatus.PENDING) {
            throw new AppException("Auction is not pending review", HttpStatus.BAD_REQUEST);
        }

        if (Boolean.FALSE.equals(request.getApproved())) {
            String reason = request.getRejectionReason();
            if (reason == null || reason.isBlank()) {
                throw new AppException("Rejection reason is required when rejecting", HttpStatus.BAD_REQUEST);
            }
            auction.setStatus(AuctionStatus.REJECTED);
            auction.setRejectionReason(reason);
            auction.setReviewedBy(adminEmail);
            auction.setReviewedAt(LocalDateTime.now());
            Auction saved = auctionRepository.save(auction);

            eventPublisher.publish(new AuctionRejectedEvent(
                    saved.getId(), saved.getProductName(),
                    saved.getSeller().getEmail(),
                    saved.getSeller().getFullName(),
                    reason));
                auditService.log(adminEmail, AuditAction.AUCTION_REVIEWED_REJECTED, "AUCTION", saved.getId(),
                    AuditOutcome.SUCCESS,
                    AuditJsonUtils.toJson("productName", saved.getProductName(), "reviewer", adminEmail, "reason", reason));

            return auctionMapper.toResponse(saved);
        }

        // Approved
        auction.setStatus(AuctionStatus.APPROVED);
        auction.setCurrentPrice(auction.getStartingPrice());
        auction.setEndTime(auction.getBiddingEndTime());
        auction.setRejectionReason(null);
        auction.setReviewedBy(adminEmail);
        auction.setReviewedAt(LocalDateTime.now());
        Auction saved = auctionRepository.save(auction);

        eventPublisher.publish(new AuctionApprovedEvent(
                    saved.getId(), saved.getProductName(),
                    saved.getSeller().getEmail(),
                    saved.getSeller().getFullName(),
                    saved.getBiddingStartTime()));
        auditService.log(adminEmail, AuditAction.AUCTION_REVIEWED_APPROVED, "AUCTION", saved.getId(),
            AuditOutcome.SUCCESS,
            AuditJsonUtils.toJson("productName", saved.getProductName(), "reviewer", adminEmail));

        return auctionMapper.toResponse(saved);
    }


    // ─── HELPERS ──────────────────────────────────────────────────────────────

    private Pageable buildPageable(AuctionFilterRequest filter, int page, int size) {
        String sortBy  = (filter.getSortBy() != null && VALID_SORT_FIELDS.contains(filter.getSortBy()))
                ? filter.getSortBy() : "createdAt";
        String sortDir = "asc".equalsIgnoreCase(filter.getSortDir()) ? "asc" : "desc";
        Sort sort = "asc".equals(sortDir)
                ? Sort.by(Sort.Direction.ASC,  sortBy)
                : Sort.by(Sort.Direction.DESC, sortBy);
        return PageRequest.of(page, size, sort);
    }

    private Specification<Auction> buildPublicSpecification(AuctionFilterRequest filter) {
        return Specification.allOf(
                AuctionSpecification.publicVisible(),
                AuctionSpecification.withSearch(filter.getSearch()),
                AuctionSpecification.withCategory(filter.getCategoryId()),
                AuctionSpecification.withMinPrice(filter.getMinPrice()),
                AuctionSpecification.withMaxPrice(filter.getMaxPrice()),
                AuctionSpecification.withStartFrom(filter.getStartFrom()),
                AuctionSpecification.withStartTo(filter.getStartTo())
        );
    }

    private Specification<Auction> buildAdminSpecification(AuctionFilterRequest filter) {
        return Specification.allOf(
                AuctionSpecification.withStatus(filter.getStatus()),
                AuctionSpecification.withSearch(filter.getSearch()),
                AuctionSpecification.withCategory(filter.getCategoryId())
        );
    }

    private PageResponse<AuctionSummaryResponse> toSummaryPage(Page<Auction> page) {
        List<String> ids = page.getContent().stream()
                .map(Auction::getId)
                .toList();
        if (ids.isEmpty()) {
            return PageResponse.<AuctionSummaryResponse>builder()
                    .content(List.of())
                    .pageNumber(page.getNumber())
                    .pageSize(page.getSize())
                    .totalElements(page.getTotalElements())
                    .totalPages(page.getTotalPages())
                    .last(page.isLast())
                    .build();
        }

        Map<String, Auction> auctionById = auctionRepository.findAllWithImagesByIds(ids).stream()
                .collect(Collectors.toMap(Auction::getId, Function.identity()));
        List<AuctionSummaryResponse> content = page.getContent().stream()
                .map(auction -> auctionById.getOrDefault(auction.getId(), auction))
                .map(auctionMapper::toSummary)
                .toList();

        return PageResponse.<AuctionSummaryResponse>builder()
                .content(content)
                .pageNumber(page.getNumber())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .last(page.isLast())
                .build();
    }
}
