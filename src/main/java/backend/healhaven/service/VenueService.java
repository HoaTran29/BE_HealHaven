package backend.healhaven.service;

import backend.healhaven.dto.request.VenueRequest;
import backend.healhaven.dto.response.VenueResponse;
import backend.healhaven.entity.Media;
import backend.healhaven.entity.User;
import backend.healhaven.entity.Venue;
import backend.healhaven.exception.BadRequestException;
import backend.healhaven.exception.ResourceNotFoundException;
import backend.healhaven.repository.MediaRepository;
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
    private final MediaRepository mediaRepository;

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

        // Lưu ảnh nếu có
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            saveVenueImages(venue.getVenueId(), request.getImageUrls());
        }

        return mapToResponse(venue);
    }

    @Transactional
    public VenueResponse updateVenue(Integer venueId, VenueRequest request, Integer providerId) {
        Venue venue = venueRepository.findById(venueId)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", venueId));

        // Kiểm tra quyền sở hữu
        if (!venue.getProvider().getUserId().equals(providerId)) {
            throw new BadRequestException("Bạn không có quyền cập nhật venue này");
        }

        venue.setName(request.getName());
        venue.setAddress(request.getAddress());
        venue.setDistrict(request.getDistrict());
        venue.setCapacity(request.getCapacity());
        venue.setPricePerHour(request.getPricePerHour());
        venue.setAmenities(request.getAmenities());
        venue.setDescription(request.getDescription());

        venue = venueRepository.save(venue);

        // Cập nhật ảnh: xóa ảnh cũ, lưu ảnh mới
        if (request.getImageUrls() != null) {
            List<Media> oldImages = mediaRepository.findByTargetIdAndTargetType(venueId, "VENUE");
            mediaRepository.deleteAll(oldImages);
            if (!request.getImageUrls().isEmpty()) {
                saveVenueImages(venue.getVenueId(), request.getImageUrls());
            }
        }

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

    public List<VenueResponse> getMyVenues(Integer providerId) {
        return venueRepository.findByProviderUserId(providerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteVenue(Integer id, Integer providerId) {
        Venue venue = venueRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", id));

        // Kiểm tra quyền sở hữu
        if (!venue.getProvider().getUserId().equals(providerId)) {
            throw new BadRequestException("Bạn không có quyền xóa venue này");
        }

        // Xóa ảnh liên quan
        List<Media> images = mediaRepository.findByTargetIdAndTargetType(id, "VENUE");
        mediaRepository.deleteAll(images);

        venueRepository.deleteById(id);
    }

    private void saveVenueImages(Integer venueId, List<String> imageUrls) {
        boolean isFirst = true;
        for (String url : imageUrls) {
            Media media = Media.builder()
                    .targetId(venueId)
                    .targetType("VENUE")
                    .mediaType("IMAGE")
                    .cloudUrl(url)
                    .isPrimary(isFirst)
                    .build();
            mediaRepository.save(media);
            isFirst = false;
        }
    }

    private VenueResponse mapToResponse(Venue venue) {
        // Lấy danh sách ảnh
        List<String> imageUrls = mediaRepository.findByTargetIdAndTargetType(
                venue.getVenueId(), "VENUE")
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
}
