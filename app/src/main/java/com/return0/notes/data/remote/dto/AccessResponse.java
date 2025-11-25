package com.return0.notes.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class AccessResponse {
    @SerializedName("success")
    private boolean success;
    
    @SerializedName("note_id")
    private String noteId;
    
    @SerializedName("has_access")
    private boolean hasAccess;
    
    @SerializedName("access_type")
    private String accessType; // "free", "paid", "subscription", "trial"
    
    @SerializedName("expires_at")
    private String expiresAt;
    
    @SerializedName("downloads_remaining")
    private Integer downloadsRemaining;
    
    @SerializedName("can_preview")
    private boolean canPreview;
    
    @SerializedName("can_download")
    private boolean canDownload;
    
    @SerializedName("payment_required")
    private PaymentRequired paymentRequired;
    
    public static class PaymentRequired {
        @SerializedName("amount")
        private double amount;
        
        @SerializedName("currency")
        private String currency;
        
        @SerializedName("payment_url")
        private String paymentUrl;
        
        public double getAmount() { return amount; }
        public String getCurrency() { return currency; }
        public String getPaymentUrl() { return paymentUrl; }
    }
    
    public boolean isSuccess() { return success; }
    public String getNoteId() { return noteId; }
    public boolean hasAccess() { return hasAccess; }
    public String getAccessType() { return accessType; }
    public String getExpiresAt() { return expiresAt; }
    public Integer getDownloadsRemaining() { return downloadsRemaining; }
    public boolean canPreview() { return canPreview; }
    public boolean canDownload() { return canDownload; }
    public PaymentRequired getPaymentRequired() { return paymentRequired; }
}

