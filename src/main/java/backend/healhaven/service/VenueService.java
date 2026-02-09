package backend.healhaven.service;

import backend.healhaven.dto.request.VenueRequest;
import backend.healhaven.dto.response.VenueResponse;
import backend.healhaven.entity.User;
import backend.healhaven.entity.Venue;
import backend.healhaven.exception.ResourceNotFoundException;
import backend.healhaven.repository.UserRepository;
import backend.healhaven.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueService {

    private final VenueRepository venueRepository;
    private final UserRepository userRepository;

    @Transactional
    public VenueResponse createVenue(VenueRequest request, Integer providerId) {
        User provider = userRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", providerId));

        Venue venue = Venue.builder()
                .name(request.getName())
                .address(request.getAddress())
                .district(request.getDistrict())
                .capacity(request.getCapacity())
                .pricePerHour(request.getPricePerHour())
                .amenities(request.getAmenities())
                .description(request.getDescription())
                .provider(provider)
                .status("AVAILABLE")
                .build();

        venue = venueRepository.save(venue);
        return mapToResponse(venue);
    }

    public List<VenueResponse> getAllVenues() {
        return venueRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    public VenueResponse getVenueById(Integer id) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));
        return mapToResponse(venue);
    }

    @Transactional
    public void deleteVenue(Integer id) {
        if (!venueRepository.existsById(id)) {
            throw new ResourceNotFoundException("Venue", "id", id);
        }
        venueRepository.deleteById(id);
    }

    private VenueResponse mapToResponse(Venue venue) {
        return VenueResponse.builder()
                .venueId(venue.getVenueId())
                .providerId(venue.getProvider().getUserId())
                .name(venue.getName())
                .address(venue.getAddress())
                .district(venue.getDistrict())
                .capacity(venue.getCapacity())
                .pricePerHour(venue.getPricePerHour())
                .amenities(venue.getAmenities())
                .description(venue.getDescription())
                .status(venue.getStatus())
                .build();
    }
}
