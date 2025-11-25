package com.return0.notes.data.remote.dto;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PreviewResponse {
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("note_id")
    private String noteId;
    
    @SerializedName("title")
    private String title;
    
    @SerializedName("total_pages")
    private int totalPages;
    
    @SerializedName("preview_pages")
    private int previewPages;
    
    @SerializedName("images")
    private List<PreviewImage> images;
    
    @SerializedName("watermark")
    private Watermark watermark;
    
    @SerializedName("access_control")
    private AccessControl accessControl;
    
    @SerializedName("error")
    private ErrorInfo error;
    
    public static class PreviewImage {
        @SerializedName("page_number")
        private int pageNumber;
        
        @SerializedName("image_url")
        private String imageUrl;
        
        @SerializedName("thumbnail_url")
        private String thumbnailUrl;
        
        @SerializedName("width")
        private int width;
        
        @SerializedName("height")
        private int height;
        
        public int getPageNumber() { return pageNumber; }
        public String getImageUrl() { return imageUrl; }
        public String getThumbnailUrl() { return thumbnailUrl; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }
    
    public static class Watermark {
        @SerializedName("enabled")
        private boolean enabled;
        
        @SerializedName("text")
        private String text;
        
        public boolean isEnabled() { return enabled; }
        public String getText() { return text; }
    }
    
    public static class AccessControl {
        @SerializedName("can_view_full")
        private boolean canViewFull;
        
        @SerializedName("requires_payment")
        private boolean requiresPayment;
        
        @SerializedName("payment_amount")
        private double paymentAmount;
        
        @SerializedName("currency")
        private String currency;
        
        public boolean canViewFull() { return canViewFull; }
        public boolean requiresPayment() { return requiresPayment; }
        public double getPaymentAmount() { return paymentAmount; }
        public String getCurrency() { return currency; }
    }
    
    public static class ErrorInfo {
        @SerializedName("code")
        private String code;
        
        @SerializedName("message")
        private String message;
        
        public String getCode() { return code; }
        public String getMessage() { return message; }
    }
    
    public boolean isSuccess() { return success; }
    public String getNoteId() { return noteId; }
    public String getTitle() { return title; }
    public int getTotalPages() { return totalPages; }
    public int getPreviewPages() { return previewPages; }
    public List<PreviewImage> getImages() { return images; }
    public Watermark getWatermark() { return watermark; }
    public AccessControl getAccessControl() { return accessControl; }
    public ErrorInfo getError() { return error; }
}

