package backend.healhaven.service;

import backend.healhaven.dto.request.VenueBookingRequest;
import backend.healhaven.dto.response.VenueBookingResponse;
import backend.healhaven.entity.User;
import backend.healhaven.entity.Venue;
import backend.healhaven.entity.VenueBooking;
import backend.healhaven.enums.VenueBookingStatus;
import backend.healhaven.exception.BadRequestException;
import backend.healhaven.exception.ResourceNotFoundException;
import backend.healhaven.repository.UserRepository;
import backend.healhaven.repository.VenueBookingRepository;
import backend.healhaven.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueBookingService {

    private final VenueBookingRepository venueBookingRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;

    @Transactional
    public VenueBookingResponse createBooking(VenueBookingRequest request, Integer hostId) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", hostId));

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", request.getVenueId()));

        // Validate time
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        // Calculate cost
        long hours = Duration.between(request.getStartTime(), request.getEndTime()).toHours();
        if (hours <= 0)
            hours = 1; // Minimum 1 hour charge
        BigDecimal totalCost = venue.getPricePerHour().multiply(BigDecimal.valueOf(hours));

        // Create booking
        VenueBooking booking = VenueBooking.builder()
                .host(host)
                .venue(venue)
                .bookingDate(request.getBookingDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalCost(totalCost)
                .status(VenueBookingStatus.REQUESTING)
                .build();

        booking = venueBookingRepository.save(booking);

        return mapToResponse(booking);
    }

    public List<VenueBookingResponse> getMyBookings(Integer hostId) {
        return venueBookingRepository.findByHostUserId(hostId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public List<VenueBookingResponse> getVenueBookings(Integer venueId) {
        return venueBookingRepository.findByVenueVenueId(venueId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    private VenueBookingResponse mapToResponse(VenueBooking booking) {
        return VenueBookingResponse.builder()
                .bookingId(booking.getVBookingId())
                .venueId(booking.getVenue().getVenueId())
                .venueName(booking.getVenue().getName())
                .venueAddress(booking.getVenue().getAddress())
                .hostId(booking.getHost().getUserId())
                .hostName(booking.getHost().getFullName())
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .totalCost(booking.getTotalCost())
                .status(booking.getStatus())
                .build();
    }
}
