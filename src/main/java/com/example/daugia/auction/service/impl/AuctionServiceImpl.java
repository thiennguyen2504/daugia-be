package com.example.daugia.auction.service.impl;

import com.example.daugia.auction.dto.*;
import com.example.daugia.auction.entity.Auction;
import com.example.daugia.auction.entity.AuctionImage;
import com.example.daugia.auction.entity.AuctionStatus;
import com.example.daugia.auction.mapper.AuctionMapper;
import com.example.daugia.auction.repository.AuctionImageRepository;
import com.example.daugia.auction.repository.AuctionRepository;
import com.example.daugia.auction.service.AuctionService;
import com.example.daugia.category.entity.Category;
import com.example.daugia.category.repository.CategoryRepository;
import com.example.daugia.common.dto.PageResponse;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

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

    // ─── SELLER ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuctionResponse create(AuctionCreateRequest request, String sellerEmail) {
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
                .status(AuctionStatus.PENDING)
                .seller(seller)
                .category(category)
                .build();

        Auction saved = auctionRepository.save(auction);

        eventPublisher.publish(new AuctionCreatedEvent(
                saved.getId(), saved.getProductName(), seller.getEmail()));

        return auctionMapper.toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuctionSummaryResponse> getMyAuctions(String sellerEmail, int page, int size) {
        User seller = userRepository.findByEmail(sellerEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Seller not found: " + sellerEmail));

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        Page<AuctionSummaryResponse> resultPage = auctionRepository
                .findAllBySeller_Id(seller.getId(), pageable)
                .map(auctionMapper::toSummary);
        return PageResponse.from(resultPage);
    }

    // ─── PUBLIC / BIDDER ──────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuctionSummaryResponse> searchPublic(AuctionFilterRequest filter, int page, int size) {
        Pageable pageable = buildPageable(filter, page, size);
        Page<AuctionSummaryResponse> resultPage = auctionRepository
                .searchPublic(filter.getSearch(), filter.getCategoryId(),
                        filter.getMinPrice(), filter.getMaxPrice(),
                        filter.getStartFrom(), filter.getStartTo(), pageable)
                .map(auctionMapper::toSummary);
        return PageResponse.from(resultPage);
    }

    @Override
    @Transactional(readOnly = true)
    public AuctionResponse getById(Long id, String currentUserEmail, boolean isAdmin) {
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

        // Public / bidder: only APPROVED, ACTIVE, ENDED
        if (auction.getStatus() == AuctionStatus.APPROVED
                || auction.getStatus() == AuctionStatus.ACTIVE
                || auction.getStatus() == AuctionStatus.ENDED) {
            return auctionMapper.toResponse(auction);
        }

        throw new ResourceNotFoundException("Auction not found");
    }

    // ─── ADMIN ────────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public PageResponse<AuctionSummaryResponse> searchAdmin(AuctionFilterRequest filter, int page, int size) {
        Pageable pageable = buildPageable(filter, page, size);
        Page<AuctionSummaryResponse> resultPage = auctionRepository
                .searchAdmin(filter.getStatus(), filter.getSearch(), filter.getCategoryId(), pageable)
                .map(auctionMapper::toSummary);
        return PageResponse.from(resultPage);
    }

    @Override
    @Transactional
    public AuctionResponse review(Long id, AuctionReviewRequest request, String adminEmail) {
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
                    saved.getSeller().getFirstname() + " " + saved.getSeller().getLastname(),
                    reason));

            return auctionMapper.toResponse(saved);
        }

        // Approved
        auction.setStatus(AuctionStatus.APPROVED);
        auction.setRejectionReason(null);
        auction.setReviewedBy(adminEmail);
        auction.setReviewedAt(LocalDateTime.now());
        Auction saved = auctionRepository.save(auction);

        eventPublisher.publish(new AuctionApprovedEvent(
                saved.getId(), saved.getProductName(),
                saved.getSeller().getEmail(),
                saved.getSeller().getFirstname() + " " + saved.getSeller().getLastname(),
                saved.getBiddingStartTime()));

        return auctionMapper.toResponse(saved);
    }

    // ─── IMAGE UPLOAD ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuctionImageResponse uploadImage(Long auctionId, MultipartFile file, String sellerEmail) throws IOException {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new ResourceNotFoundException("Auction not found"));

        if (!auction.getSeller().getEmail().equals(sellerEmail)) {
            throw new AppException("Not authorized to upload images for this auction", HttpStatus.FORBIDDEN);
        }

        if (auction.getStatus() != AuctionStatus.PENDING) {
            throw new AppException("Images can only be uploaded when auction is pending", HttpStatus.BAD_REQUEST);
        }

        byte[] bytes = file.getBytes();
        int sortOrder = auctionImageRepository.countByAuction_Id(auctionId);

        UploadResult result = storageService.upload(bytes, file.getOriginalFilename(),
                "auctions/" + auctionId);

        AuctionImage image = AuctionImage.builder()
                .auction(auction)
                .imageUrl(result.url())
                .publicId(result.publicId())
                .sortOrder(sortOrder)
                .build();

        AuctionImage saved = auctionImageRepository.save(image);
        return auctionMapper.toImageResponse(saved);
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
}
