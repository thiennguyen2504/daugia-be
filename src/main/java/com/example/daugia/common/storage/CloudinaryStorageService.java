package com.example.daugia.common.storage;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.example.daugia.common.audit.AuditAction;
import com.example.daugia.common.audit.AuditJsonUtils;
import com.example.daugia.common.audit.AuditOutcome;
import com.example.daugia.common.audit.AuditService;
import com.example.daugia.common.exception.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryStorageService implements StorageService {

    private final Cloudinary cloudinary;
    private final AuditService auditService;

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
            
            String truncatedUrl = url != null && url.length() > 80 ? url.substring(0, 80) + "..." : url;
            log.info("Cloudinary upload successful: publicId={} url={}", publicId, truncatedUrl);
            
            return new UploadResult(url, publicId);
        } catch (IOException e) {
            log.error("Cloudinary upload failed for file={}, folder={}", originalFilename, folder, e);
            String actor = MDC.get("actor");
            auditService.log(actor, AuditAction.CLOUDINARY_UPLOAD_FAILED, "IMAGE", null,
                    AuditOutcome.FAILURE, AuditJsonUtils.toJson("originalFilename", originalFilename, "folder", folder, "error", e.getMessage()));
            throw new AppException("Failed to upload image: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public void delete(String publicId) {
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Cloudinary delete failed for publicId={}, reason={}", publicId, e.getMessage(), e);
            // Non-fatal — log and continue
        }
    }
}
