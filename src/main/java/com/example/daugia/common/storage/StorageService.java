package com.example.daugia.common.storage;

public interface StorageService {

    /**
     * Upload a file and return its public URL + publicId.
     *
     * @param fileBytes         raw bytes
     * @param originalFilename  used to detect mime type
     * @param folder            Cloudinary folder path e.g. "auctions/123"
     * @return UploadResult containing the public HTTPS URL and publicId
     */
    UploadResult upload(byte[] fileBytes, String originalFilename, String folder);

    /**
     * Delete a file by its Cloudinary public ID.
     */
    void delete(String publicId);
}
