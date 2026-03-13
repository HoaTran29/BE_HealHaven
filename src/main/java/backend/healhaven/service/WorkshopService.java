package backend.healhaven.service;

import backend.healhaven.dto.request.WorkshopSearchRequest;
import backend.healhaven.dto.response.PageResponse;
import backend.healhaven.dto.response.WorkshopResponse;
import backend.healhaven.entity.Media;
import backend.healhaven.entity.Workshop;
import backend.healhaven.enums.WorkshopStatus;
import backend.healhaven.exception.ResourceNotFoundException;
import backend.healhaven.repository.BookingRepository;
import backend.healhaven.repository.MediaRepository;
import backend.healhaven.repository.ReviewRepository;
import backend.healhaven.repository.WorkshopRepository;
import backend.healhaven.repository.UserRepository;
import backend.healhaven.repository.VenueRepository;
import backend.healhaven.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WorkshopService {

        private final WorkshopRepository workshopRepository;
        private final BookingRepository bookingRepository;
        private final ReviewRepository reviewRepository;
        private final MediaRepository mediaRepository;
        private final UserRepository userRepository;
        private final VenueRepository venueRepository;

        public PageResponse<WorkshopResponse> searchWorkshops(WorkshopSearchRequest request) {
                // Native query uses SQL column names, not Java field names. Map accordingly.
                String sortByColumn = switch (request.getSortBy()) {
                        case "startTime" -> "start_time";
                        case "endTime" -> "end_time";
                        case "createdAt" -> "created_at";
                        case "maxAttendees" -> "max_attendees";
                        case "minAttendees" -> "min_attendees";
                        default -> request.getSortBy(); // "price", "title" etc. are same in SQL
                };

                Sort sort = request.getSortDir().equalsIgnoreCase("desc")
                                ? Sort.by(sortByColumn).descending()
                                : Sort.by(sortByColumn).ascending();

                Pageable pageable = PageRequest.of(request.getPage(), request.getSize(), sort);

                Page<Workshop> workshopPage = workshopRepository.searchWorkshops(
                                WorkshopStatus.PUBLISHED.name(),
                                request.getCategory(),
                                request.getDistrict(),
                                request.getMinPrice(),
                                request.getMaxPrice(),
                                request.getStartDate(),
                                request.getKeyword(),
                                pageable);

                List<WorkshopResponse> content = workshopPage.getContent().stream()
                                .map(this::mapToResponse)
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

        public WorkshopResponse getWorkshopById(Integer workshopId) {
                Workshop workshop = workshopRepository.findById(workshopId)
                                .orElseThrow(() -> new ResourceNotFoundException("Workshop", "id", workshopId));
                return mapToResponse(workshop);
        }

        public List<WorkshopResponse> getFeaturedWorkshops() {
                return workshopRepository.findByIsFeaturedTrueAndStatus(WorkshopStatus.PUBLISHED)
                                .stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        @Transactional
        public WorkshopResponse createWorkshop(backend.healhaven.dto.request.WorkshopRequest request, Integer hostId) {
                User host = userRepository.findById(hostId)
                                .orElseThrow(() -> new ResourceNotFoundException("User", "id", hostId));

                Workshop workshop = Workshop.builder()
                                .host(host)
                                .title(request.getTitle())
                                .category(request.getCategory())
                                .description(request.getDescription())
                                .price(request.getPrice())
                                .minAttendees(request.getMinAttendees() != null ? request.getMinAttendees() : 1)
                                .maxAttendees(request.getMaxAttendees())
                                .startTime(request.getStartTime())
                                .endTime(request.getEndTime())
                                .materials(request.getMaterials())
                                .itinerary(request.getItinerary())
                                .address(request.getAddress())
                                .district(request.getDistrict())
                                .isFeatured(request.getIsFeatured() != null ? request.getIsFeatured() : false)
                                .status(WorkshopStatus.DRAFT)
                                .build();

                if (request.getVenueId() != null) {
                        backend.healhaven.entity.Venue venue = venueRepository.findById(request.getVenueId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Venue", "id",
                                                        request.getVenueId()));
                        workshop.setVenue(venue);
                }

                workshop = workshopRepository.save(workshop);
                return mapToResponse(workshop);
        }

        @Transactional
        public WorkshopResponse updateWorkshop(Integer workshopId,
                        backend.healhaven.dto.request.WorkshopRequest request, Integer hostId) {
                Workshop workshop = workshopRepository.findById(workshopId)
                                .orElseThrow(() -> new ResourceNotFoundException("Workshop", "id", workshopId));

                // Verify ownership
                if (!workshop.getHost().getUserId().equals(hostId)) {
                        throw new backend.healhaven.exception.BadRequestException(
                                        "You are not authorized to update this workshop");
                }

                workshop.setTitle(request.getTitle());
                workshop.setCategory(request.getCategory());
                workshop.setDescription(request.getDescription());
                workshop.setPrice(request.getPrice());
                workshop.setMinAttendees(request.getMinAttendees() != null ? request.getMinAttendees()
                                : workshop.getMinAttendees());
                workshop.setMaxAttendees(request.getMaxAttendees());
                workshop.setStartTime(request.getStartTime());
                workshop.setEndTime(request.getEndTime());
                workshop.setMaterials(request.getMaterials());
                workshop.setItinerary(request.getItinerary());
                workshop.setAddress(request.getAddress());
                workshop.setDistrict(request.getDistrict());
                if (request.getIsFeatured() != null) {
                        workshop.setIsFeatured(request.getIsFeatured());
                }

                if (request.getVenueId() != null) {
                        backend.healhaven.entity.Venue venue = venueRepository.findById(request.getVenueId())
                                        .orElseThrow(() -> new ResourceNotFoundException("Venue", "id",
                                                        request.getVenueId()));
                        workshop.setVenue(venue);
                }

                workshop = workshopRepository.save(workshop);
                return mapToResponse(workshop);
        }

        @Transactional
        public void deleteWorkshop(Integer workshopId, Integer hostId) {
                Workshop workshop = workshopRepository.findById(workshopId)
                                .orElseThrow(() -> new ResourceNotFoundException("Workshop", "id", workshopId));

                // Verify ownership
                if (!workshop.getHost().getUserId().equals(hostId)) {
                        throw new backend.healhaven.exception.BadRequestException(
                                        "You are not authorized to delete this workshop");
                }

                // Check if workshop has bookings
                int confirmedBookings = workshopRepository.countConfirmedBookings(workshopId);
                if (confirmedBookings > 0) {
                        throw new backend.healhaven.exception.BadRequestException(
                                        "Cannot delete workshop with confirmed bookings");
                }

                workshopRepository.delete(workshop);
        }

        @Transactional
        public WorkshopResponse submitWorkshop(Integer workshopId, Integer hostId) {
                Workshop workshop = workshopRepository.findById(workshopId)
                                .orElseThrow(() -> new ResourceNotFoundException("Workshop", "id", workshopId));

                // Verify ownership
                if (!workshop.getHost().getUserId().equals(hostId)) {
                        throw new backend.healhaven.exception.BadRequestException(
                                        "You are not authorized to submit this workshop");
                }

                // Only DRAFT workshops can be submitted for review
                if (workshop.getStatus() != WorkshopStatus.DRAFT) {
                        throw new backend.healhaven.exception.BadRequestException(
                                        "Only DRAFT workshops can be submitted for review. Current status: "
                                                        + workshop.getStatus());
                }

                workshop.setStatus(WorkshopStatus.PENDING_APPROVAL);
                workshop = workshopRepository.save(workshop);
                return mapToResponse(workshop);
        }

        public List<WorkshopResponse> getMyWorkshops(Integer hostId) {
                return workshopRepository.findByHostUserId(hostId)
                                .stream()
                                .map(this::mapToResponse)
                                .collect(Collectors.toList());
        }

        private WorkshopResponse mapToResponse(Workshop workshop) {
                // Get available seats
                int bookedSeats = bookingRepository.countTotalBookedSeats(workshop.getWorkshopId());
                int availableSeats = workshop.getMaxAttendees() - bookedSeats;

                // Get reviews stats
                Double avgRating = reviewRepository.calculateAverageRating(workshop.getWorkshopId());
                int reviewCount = reviewRepository.countByWorkshopId(workshop.getWorkshopId());

                // Get images
                List<String> images = mediaRepository.findByTargetIdAndTargetType(
                                workshop.getWorkshopId(), "WORKSHOP")
                                .stream()
                                .map(Media::getCloudUrl)
                                .collect(Collectors.toList());

                WorkshopResponse.HostInfo hostInfo = null;
                if (workshop.getHost() != null) {
                        hostInfo = WorkshopResponse.HostInfo.builder()
                                        .userId(workshop.getHost().getUserId())
                                        .fullName(workshop.getHost().getFullName())
                                        .avatarUrl(workshop.getHost().getAvatarUrl())
                                        .bio(workshop.getHost().getBio())
                                        .build();
                }

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

                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

                return WorkshopResponse.builder()
                                .workshopId(workshop.getWorkshopId())
                                .id(workshop.getWorkshopId())
                                .title(workshop.getTitle())
                                .category(workshop.getCategory())
                                .description(workshop.getDescription())
                                .price(workshop.getPrice())
                                .minAttendees(workshop.getMinAttendees())
                                .maxAttendees(workshop.getMaxAttendees())
                                .availableSeats(availableSeats)
                                .capacity(workshop.getMaxAttendees())
                                .startTime(workshop.getStartTime())
                                .endTime(workshop.getEndTime())
                                .startDate(workshop.getStartTime().format(formatter))
                                .endDate(workshop.getEndTime().format(formatter))
                                .status(workshop.getStatus().name())
                                .isFeatured(workshop.getIsFeatured())
                                .materials(workshop.getMaterials())
                                .itinerary(workshop.getItinerary())
                                .address(workshop.getAddress() != null ? workshop.getAddress() : (workshop.getVenue() != null ? workshop.getVenue().getAddress() : null))
                                .district(workshop.getDistrict() != null ? workshop.getDistrict() : (workshop.getVenue() != null ? workshop.getVenue().getDistrict() : null))
                                .createdAt(workshop.getCreatedAt())
                                .host(hostInfo)
                                .venue(venueInfo)
                                .images(images)
                                .averageRating(avgRating)
                                .reviewCount(reviewCount)
                                .build();
        }
}
