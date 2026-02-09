package backend.healhaven.service;

import backend.healhaven.entity.Media;
import backend.healhaven.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;

    // Upload directory - defaults to "uploads" in project root
    private final Path fileStorageLocation = Paths.get("uploads").toAbsolutePath().normalize();

    public Media uploadMedia(MultipartFile file, Integer targetId, String targetType, String mediaType) {
        try {
            // Ensure directory exists
            Files.createDirectories(fileStorageLocation);

            // Generate unique filename
            String fileName = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path targetLocation = fileStorageLocation.resolve(fileName);

            // Save file
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            // Create URL (relative path to be served)
            String fileUrl = "/uploads/" + fileName;

            // Save to DB
            Media media = Media.builder()
                    .targetId(targetId)
                    .targetType(targetType)
                    .mediaType(mediaType)
                    .cloudUrl(fileUrl) // storing local path as URL for now
                    .isPrimary(false)
                    .build();

            return mediaRepository.save(media);
        } catch (IOException ex) {
            throw new RuntimeException("Could not store file " + file.getOriginalFilename(), ex);
        }
    }

    public void deleteMedia(Integer mediaId) {
        mediaRepository.deleteById(mediaId);
    }
}
