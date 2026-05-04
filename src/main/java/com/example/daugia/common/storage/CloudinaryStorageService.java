package com.example.daugia.common.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.daugia.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;

    @Override
    public UploadResult upload(byte[] fileBytes, String originalFilename, String folder) {
        try {
            Map<?, ?> result = cloudinary.uploader().upload(fileBytes, ObjectUtils.asMap(
                    "folder",         folder,
                    "resource_type",  "image",
                    "transformation", "q_auto,f_auto"
            ));
            String url      = (String) result.get("secure_url");
            String publicId = (String) result.get("public_id");
            return new UploadResult(url, publicId);
        } catch (IOException e) {
            log.error("Cloudinary upload failed for file={}, folder={}", originalFilename, folder, e);
            throw new AppException("Failed to upload image: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Cloudinary delete failed for publicId={}", publicId, e);
            // Non-fatal — log and continue
        }
    }
}
