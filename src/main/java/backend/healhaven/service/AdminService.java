package backend.healhaven.service;

import backend.healhaven.dto.response.AdminUserResponse;
import backend.healhaven.dto.response.PageResponse;
import backend.healhaven.entity.User;
import backend.healhaven.enums.UserRole;
import backend.healhaven.enums.WorkshopStatus;
import backend.healhaven.exception.ResourceNotFoundException;
import backend.healhaven.repository.UserRepository;
import backend.healhaven.repository.WorkshopRepository;
import backend.healhaven.dto.response.WorkshopResponse;
import backend.healhaven.entity.Workshop;
import backend.healhaven.entity.Media;
import backend.healhaven.repository.MediaRepository;
import backend.healhaven.entity.Venue;
import backend.healhaven.repository.VenueRepository;
import backend.healhaven.dto.response.VenueResponse;
import backend.healhaven.entity.Booking;
import backend.healhaven.repository.BookingRepository;
import backend.healhaven.repository.ReviewRepository;
import backend.healhaven.dto.response.AdminStatsResponse;
import backend.healhaven.dto.response.RevenueChartResponse;
import backend.healhaven.entity.WithdrawalRequestEntity;
import backend.healhaven.repository.WithdrawalRequestRepository;
import backend.healhaven.dto.response.WithdrawalResponse;
import backend.healhaven.enums.WithdrawalStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final WorkshopRepository workshopRepository;
    private final MediaRepository mediaRepository;
    private final VenueRepository venueRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final WithdrawalRequestRepository withdrawalRequestRepository;

    public PageResponse<AdminUserResponse> getUsers(UserRole role, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<User> userPage;

        if (role != null) {
            userPage = userRepository.findByRole(role, pageable);
        } else {
            userPage = userRepository.findAll(pageable);
        }

        var content = userPage.getContent().stream()
                .map(this::mapToAdminUserResponse)
                .collect(Collectors.toList());

        return PageResponse.<AdminUserResponse>builder()
                .content(content)
                .page(userPage.getNumber())
                .size(userPage.getSize())
                .totalElements(userPage.getTotalElements())
                .totalPages(userPage.getTotalPages())
                .first(userPage.isFirst())
                .last(userPage.isLast())
                .build();
    }

    @Transactional
    public AdminUserResponse updateUserStatus(Integer userId, String status) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Active = isBanned(false), INACTIVE = isBanned(true)
        if ("ACTIVE".equalsIgnoreCase(status)) {
            user.setIsBanned(false);
        } else if ("INACTIVE".equalsIgnoreCase(status)) {
            user.setIsBanned(true);
        } else {
            throw new backend.healhaven.exception.BadRequestException("Invalid status, must be ACTIVE or INACTIVE");
        }

        return mapToAdminUserResponse(userRepository.save(user));
    }

    @Transactional
    public AdminUserResponse updateUserRole(Integer userId, UserRole newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        user.setRole(newRole);
        return mapToAdminUserResponse(userRepository.save(user));
    }

    private AdminUserResponse mapToAdminUserResponse(User user) {
        return AdminUserResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phoneNumber(user.getPhoneNumber())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole())
                .isBanned(user.getIsBanned() != null ? user.getIsBanned() : false)
                .createdAt(user.getCreatedAt())
                .build();
    }

    // --- WORKSHOP APPROVAL ---

    public PageResponse<WorkshopResponse> getWorkshops(WorkshopStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Workshop> workshopPage;

        if (status != null) {
            workshopPage = workshopRepository.findByStatus(status, pageable);
        } else {
            workshopPage = workshopRepository.findAll(pageable);
        }

        var content = workshopPage.getContent().stream()
                .map(this::mapToWorkshopResponse)
                .collect(Collectors.toList());

        return PageResponse.<WorkshopResponse>builder()
                .content(content)
                .page(workshopPage.getNumber())
                .size(workshopPage.getSize())
                .totalElements(workshopPage.getTotalElements())
                .totalPages(workshopPage.getTotalPages())
                .first(workshopPage.isFirst())
                .last(workshopPage.isLast())
                .build();
    }

    @Transactional
    public WorkshopResponse approveWorkshop(Integer workshopId) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop", "id", workshopId));

        if (workshop.getStatus() != WorkshopStatus.PENDING_APPROVAL) {
            throw new backend.healhaven.exception.BadRequestException(
                    "Workshop must be in PENDING_APPROVAL status to be approved");
        }

        workshop.setStatus(WorkshopStatus.PUBLISHED);
        return mapToWorkshopResponse(workshopRepository.save(workshop));
    }

    @Transactional
    public WorkshopResponse rejectWorkshop(Integer workshopId, String reason) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop", "id", workshopId));

        if (workshop.getStatus() != WorkshopStatus.PENDING_APPROVAL) {
            throw new backend.healhaven.exception.BadRequestException(
                    "Workshop must be in PENDING_APPROVAL status to be rejected");
        }

        workshop.setStatus(WorkshopStatus.REJECTED);
        // Note: Could save this reason to a separate table or notification entity in
        // the future
        return mapToWorkshopResponse(workshopRepository.save(workshop));
    }

    private WorkshopResponse mapToWorkshopResponse(Workshop workshop) {
        java.util.List<String> imageUrls = mediaRepository
                .findByTargetIdAndTargetType(workshop.getWorkshopId(), "WORKSHOP")
                .stream()
                .map(Media::getCloudUrl)
                .collect(Collectors.toList());

        WorkshopResponse.HostInfo hostInfo = WorkshopResponse.HostInfo.builder()
                .userId(workshop.getHost().getUserId())
                .fullName(workshop.getHost().getFullName())
                .avatarUrl(workshop.getHost().getAvatarUrl())
                .bio(workshop.getHost().getBio())
                .build();

        WorkshopResponse.VenueInfo venueInfo = null;
        if (workshop.getVenue() != null) {
            venueInfo = WorkshopResponse.VenueInfo.builder()
                    .venueId(workshop.getVenue().getVenueId())
                    .name(workshop.getVenue().getName())
                    .address(workshop.getVenue().getAddress())
                    .district(workshop.getVenue().getDistrict())
                    .capacity(workshop.getVenue().getCapacity())
                    .amenities(workshop.getVenue().getAmenities())
                    .build();
        }

        // Calculate available seats
        int bookedSeats = bookingRepository.countTotalBookedSeats(workshop.getWorkshopId());
        int availableSeats = workshop.getMaxAttendees() - bookedSeats;

        // Calculate review statistics
        Double avgRating = reviewRepository.calculateAverageRating(workshop.getWorkshopId());
        int reviewCount = reviewRepository.countByWorkshopId(workshop.getWorkshopId());

        return WorkshopResponse.builder()
                .workshopId(workshop.getWorkshopId())
                .title(workshop.getTitle())
                .category(workshop.getCategory())
                .description(workshop.getDescription())
                .price(workshop.getPrice())
                .minAttendees(workshop.getMinAttendees())
                .maxAttendees(workshop.getMaxAttendees())
                .availableSeats(availableSeats)
                .startTime(workshop.getStartTime())
                .endTime(workshop.getEndTime())
                .status(workshop.getStatus().name())
                .isFeatured(workshop.getIsFeatured())
                .createdAt(workshop.getCreatedAt())
                .host(hostInfo)
                .venue(venueInfo)
                .images(imageUrls)
                .averageRating(avgRating)
                .reviewCount(reviewCount)
                .build();
    }

    // --- VENUE APPROVAL ---

    public PageResponse<VenueResponse> getVenues(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<Venue> venuePage;

        if (status != null && !status.trim().isEmpty()) {
            venuePage = venueRepository.findByStatus(status, pageable);
        } else {
            venuePage = venueRepository.findAll(pageable);
        }

        var content = venuePage.getContent().stream()
                .map(this::mapToVenueResponse)
                .collect(Collectors.toList());

        return PageResponse.<VenueResponse>builder()
                .content(content)
                .page(venuePage.getNumber())
                .size(venuePage.getSize())
                .totalElements(venuePage.getTotalElements())
                .totalPages(venuePage.getTotalPages())
                .first(venuePage.isFirst())
                .last(venuePage.isLast())
                .build();
    }

    @Transactional
    public VenueResponse approveVenue(Integer venueId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));

        if (!"PENDING".equalsIgnoreCase(venue.getStatus())) {
            throw new backend.healhaven.exception.BadRequestException("Venue must be in PENDING status to be approved");
        }

        venue.setStatus("AVAILABLE");
        return mapToVenueResponse(venueRepository.save(venue));
    }

    @Transactional
    public VenueResponse rejectVenue(Integer venueId, String reason) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));

        if (!"PENDING".equalsIgnoreCase(venue.getStatus())) {
            throw new backend.healhaven.exception.BadRequestException("Venue must be in PENDING status to be rejected");
        }

        venue.setStatus("REJECTED");
        // Could save the rejection reason as well
        return mapToVenueResponse(venueRepository.save(venue));
    }

    private VenueResponse mapToVenueResponse(Venue venue) {
        java.util.List<String> imageUrls = mediaRepository.findByTargetIdAndTargetType(venue.getVenueId(), "VENUE")
                .stream()
                .map(Media::getCloudUrl)
                .collect(Collectors.toList());

        return VenueResponse.builder()
                .venueId(venue.getVenueId())
                .providerId(venue.getProvider().getUserId())
                .providerName(venue.getProvider().getFullName())
                .name(venue.getName())
                .address(venue.getAddress())
                .district(venue.getDistrict())
                .capacity(venue.getCapacity())
                .pricePerHour(venue.getPricePerHour())
                .amenities(venue.getAmenities())
                .description(venue.getDescription())
                .status(venue.getStatus())
                .imageUrls(imageUrls)
                .createdAt(venue.getCreatedAt())
                .build();
    }

    // --- DASHBOARD ANALYTICS ---

    public AdminStatsResponse getOverviewStats() {
        long totalUsers = userRepository.count();
        long totalWorkshops = workshopRepository.count();
        long totalVenues = venueRepository.count();
        BigDecimal netRevenue = bookingRepository.calculateSystemNetRevenue();

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalWorkshops(totalWorkshops)
                .totalVenues(totalVenues)
                .netRevenue(netRevenue)
                .build();
    }

    public List<RevenueChartResponse> getRevenueChart(String type, int year) {
        List<Booking> bookings = bookingRepository.findPaidBookingsByYear(year);

        if ("monthly".equalsIgnoreCase(type)) {
            // Group by month
            Map<Integer, BigDecimal> monthlyRevenue = new HashMap<>();
            for (int i = 1; i <= 12; i++) {
                monthlyRevenue.put(i, BigDecimal.ZERO);
            }

            for (Booking b : bookings) {
                int month = b.getCreatedAt().getMonthValue();
                monthlyRevenue.put(month, monthlyRevenue.get(month).add(b.getTotalPrice()));
            }

            List<RevenueChartResponse> response = new ArrayList<>();
            String[] monthNames = { "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov",
                    "Dec" };
            for (int i = 1; i <= 12; i++) {
                response.add(new RevenueChartResponse(monthNames[i - 1], monthlyRevenue.get(i)));
            }
            return response;
        }

        // Add weekly/daily logic later if needed
        return new ArrayList<>();
    }

    // --- WITHDRAWALS ---

    public PageResponse<WithdrawalResponse> getWithdrawals(WithdrawalStatus status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<WithdrawalRequestEntity> withdrawalPage;

        if (status != null) {
            withdrawalPage = withdrawalRequestRepository.findByStatus(status, pageable);
        } else {
            withdrawalPage = withdrawalRequestRepository.findAll(pageable);
        }

        var content = withdrawalPage.getContent().stream()
                .map(this::mapToWithdrawalResponse)
                .collect(Collectors.toList());

        return PageResponse.<WithdrawalResponse>builder()
                .content(content)
                .page(withdrawalPage.getNumber())
                .size(withdrawalPage.getSize())
                .totalElements(withdrawalPage.getTotalElements())
                .totalPages(withdrawalPage.getTotalPages())
                .first(withdrawalPage.isFirst())
                .last(withdrawalPage.isLast())
                .build();
    }

    @Transactional
    public WithdrawalResponse completeWithdrawal(Integer withdrawalId) {
        WithdrawalRequestEntity withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("WithdrawalRequest", "id", withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new backend.healhaven.exception.BadRequestException(
                    "Withdrawal must be in PENDING status to be completed");
        }

        withdrawal.setStatus(WithdrawalStatus.COMPLETED);
        withdrawal.setProcessedAt(LocalDateTime.now());
        return mapToWithdrawalResponse(withdrawalRequestRepository.save(withdrawal));
    }

    @Transactional
    public WithdrawalResponse rejectWithdrawal(Integer withdrawalId, String reason) {
        WithdrawalRequestEntity withdrawal = withdrawalRequestRepository.findById(withdrawalId)
                .orElseThrow(() -> new ResourceNotFoundException("WithdrawalRequest", "id", withdrawalId));

        if (withdrawal.getStatus() != WithdrawalStatus.PENDING) {
            throw new backend.healhaven.exception.BadRequestException(
                    "Withdrawal must be in PENDING status to be rejected");
        }

        withdrawal.setStatus(WithdrawalStatus.REJECTED);
        withdrawal.setProcessedAt(LocalDateTime.now());
        withdrawal.setNote(reason);
        return mapToWithdrawalResponse(withdrawalRequestRepository.save(withdrawal));
    }

    private WithdrawalResponse mapToWithdrawalResponse(WithdrawalRequestEntity withdrawal) {
        return WithdrawalResponse.builder()
                .withdrawalId(withdrawal.getWithdrawalId())
                .userId(withdrawal.getUser().getUserId())
                .fullName(withdrawal.getUser().getFullName())
                .amount(withdrawal.getAmount())
                .bankInfo(withdrawal.getBankInfo())
                .status(withdrawal.getStatus())
                .note(withdrawal.getNote())
                .requestedAt(withdrawal.getRequestedAt())
                .processedAt(withdrawal.getProcessedAt())
                .build();
    }
}
