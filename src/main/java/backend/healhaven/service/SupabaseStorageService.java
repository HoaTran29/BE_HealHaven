package backend.healhaven.service;

import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Service
public class SupabaseStorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Value("${supabase.bucket}")
    private String bucketName;

    private final OkHttpClient httpClient = new OkHttpClient();

    /**
     * Upload file lên Supabase Storage và trả về public URL
     */
    public String uploadFile(MultipartFile file, String folder) throws IOException {
        // Tạo tên file unique
        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String fileName = folder + "/" + UUID.randomUUID() + extension;

        // Tạo request body
        RequestBody body = RequestBody.create(
                file.getBytes(),
                MediaType.parse(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
        );

        // Gọi Supabase Storage REST API
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + fileName;

        Request request = new Request.Builder()
                .url(uploadUrl)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .addHeader("Content-Type", file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .post(body)
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful()) {
                String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                throw new IOException("Supabase upload failed: " + response.code() + " - " + errorBody);
            }
        }

        // Trả về public URL
        return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;
    }

    /**
     * Xóa file trên Supabase Storage
     */
    public void deleteFile(String fileUrl) throws IOException {
        // Trích xuất file path từ public URL
        String prefix = supabaseUrl + "/storage/v1/object/public/" + bucketName + "/";
        if (!fileUrl.startsWith(prefix)) {
            return; // Không phải Supabase URL, skip
        }
        String filePath = fileUrl.substring(prefix.length());

        String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + filePath;

        Request request = new Request.Builder()
                .url(deleteUrl)
                .addHeader("Authorization", "Bearer " + supabaseKey)
                .delete()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            if (!response.isSuccessful() && response.code() != 404) {
                throw new IOException("Supabase delete failed: " + response.code());
            }
        }
    }
}
