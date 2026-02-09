package backend.healhaven.controller;

import backend.healhaven.dto.response.ApiResponse;
import backend.healhaven.entity.Media;
import backend.healhaven.service.MediaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/media")
@RequiredArgsConstructor
@Tag(name = "Media", description = "Media Upload APIs")
public class MediaController {

    private final MediaService mediaService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload media", description = "Upload image for Workshop or Venue")
    public ResponseEntity<ApiResponse<Media>> uploadMedia(
            @RequestParam("file") MultipartFile file,
            @RequestParam("targetId") Integer targetId,
            @RequestParam("targetType") String targetType, // WORKSHOP, VENUE
            @RequestParam(value = "mediaType", defaultValue = "IMAGE") String mediaType) {

        Media media = mediaService.uploadMedia(file, targetId, targetType, mediaType);
        return ResponseEntity.ok(ApiResponse.success("Upload successful", media));
    }

    @DeleteMapping("/{mediaId}")
    @Operation(summary = "Delete media", description = "Delete media by ID")
    public ResponseEntity<ApiResponse<Void>> deleteMedia(@PathVariable Integer mediaId) {
        mediaService.deleteMedia(mediaId);
        return ResponseEntity.ok(ApiResponse.success("Media deleted successfully", null));
    }
}
