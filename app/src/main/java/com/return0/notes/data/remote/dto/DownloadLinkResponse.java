package com.return0.notes.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class DownloadLinkResponse {
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("note_id")
    private String noteId;
    
    @SerializedName("download_url")
    private String downloadUrl;
    
    @SerializedName("expires_at")
    private String expiresAt;
    
    @SerializedName("expires_in_seconds")
    private int expiresInSeconds;
    
    @SerializedName("file_info")
    private FileInfo fileInfo;
    
    @SerializedName("error")
    private ErrorInfo error;
    
    public static class FileInfo {
        @SerializedName("filename")
        private String filename;
        
        @SerializedName("size_bytes")
        private long sizeBytes;
        
        @SerializedName("size_mb")
        private double sizeMb;
        
        @SerializedName("pages")
        private int pages;
        
        public String getFilename() { return filename; }
        public long getSizeBytes() { return sizeBytes; }
        public double getSizeMb() { return sizeMb; }
        public int getPages() { return pages; }
    }
    
    public static class ErrorInfo {
        @SerializedName("code")
        private String code;
        
        @SerializedName("message")
        private String message;
        
        @SerializedName("payment_amount")
        private Double paymentAmount;
        
        @SerializedName("currency")
        private String currency;
        
        @SerializedName("payment_url")
        private String paymentUrl;
        
        public String getCode() { return code; }
        public String getMessage() { return message; }
        public Double getPaymentAmount() { return paymentAmount; }
        public String getCurrency() { return currency; }
        public String getPaymentUrl() { return paymentUrl; }
    }
    
    public boolean isSuccess() { return success; }
    public String getNoteId() { return noteId; }
    public String getDownloadUrl() { return downloadUrl; }
    public String getExpiresAt() { return expiresAt; }
    public int getExpiresInSeconds() { return expiresInSeconds; }
    public FileInfo getFileInfo() { return fileInfo; }
    public ErrorInfo getError() { return error; }
}

