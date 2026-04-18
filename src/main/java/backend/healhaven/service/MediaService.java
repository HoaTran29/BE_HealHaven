package backend.healhaven.service;

import backend.healhaven.entity.Media;
import backend.healhaven.repository.MediaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MediaService {

    private final MediaRepository mediaRepository;
    private final SupabaseStorageService supabaseStorageService;

    public Media uploadMedia(MultipartFile file, Integer targetId, String targetType, String mediaType) {
        try {
            // Upload to Supabase Storage
            String folder = targetType.toLowerCase(); // "workshop", "venue", "user"
            String fileUrl = supabaseStorageService.uploadFile(file, folder);

            // Save to DB
            Media media = Media.builder()
                    .targetId(targetId)
                    .targetType(targetType)
                    .mediaType(mediaType)
                    .cloudUrl(fileUrl) // Supabase public URL
                    .isPrimary(false)
                    .build();

            return mediaRepository.save(media);
        } catch (IOException ex) {
            throw new RuntimeException("Could not upload file " + file.getOriginalFilename(), ex);
        }
    }

    public void deleteMedia(Integer mediaId) {
        // Xóa file trên Supabase trước khi xóa record trong DB
        mediaRepository.findById(mediaId).ifPresent(media -> {
            try {
                supabaseStorageService.deleteFile(media.getCloudUrl());
            } catch (Exception e) {
                log.warn("Failed to delete file from Supabase: {}", media.getCloudUrl(), e);
            }
        });
        mediaRepository.deleteById(mediaId);
    }
}
