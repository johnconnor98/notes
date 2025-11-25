package com.return0.notes.data.remote;

import com.return0.notes.data.remote.dto.NoteDto;
import com.return0.notes.data.remote.dto.PreviewResponse;
import com.return0.notes.data.remote.dto.DownloadLinkResponse;
import com.return0.notes.data.remote.dto.AccessResponse;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
import retrofit2.http.Query;
import retrofit2.http.Streaming;
import java.util.List;

public interface ApiService {
    @GET("api/notes")
    Call<List<NoteDto>> getNotes(
            @retrofit2.http.Query("subject") String subject,
            @retrofit2.http.Query("semester") String semester,
            @retrofit2.http.Query("branch") String branch,
            @retrofit2.http.Query("college") String college
    );

    @Multipart
    @POST("api/notes")
    Call<NoteDto> uploadNote(
            @Part("title") okhttp3.RequestBody title,
            @Part("subject") okhttp3.RequestBody subject,
            @Part("semester") okhttp3.RequestBody semester,
            @Part("branch") okhttp3.RequestBody branch,
            @Part("college") okhttp3.RequestBody college,
            @Part MultipartBody.Part file,
            @Part MultipartBody.Part thumbnail
    );

    @Multipart
    @POST("api/notes")
    Call<NoteDto> uploadNoteMetadata(
            @Part("title") okhttp3.RequestBody title,
            @Part("subject") okhttp3.RequestBody subject,
            @Part("semester") okhttp3.RequestBody semester,
            @Part("branch") okhttp3.RequestBody branch,
            @Part("college") okhttp3.RequestBody college,
            @Part("file_url") okhttp3.RequestBody fileUrl,
            @Part("file_path") okhttp3.RequestBody filePath,
            @Part("thumbnail_url") okhttp3.RequestBody thumbnailUrl
    );

    @Streaming
    @GET("api/notes/{id}/download")
    Call<ResponseBody> downloadNote(@Path("id") String id);
    
    // Preview API - Get PDF preview images
    @GET("api/notes/{id}/preview")
    Call<PreviewResponse> getPreview(
        @Path("id") String noteId,
        @Query("pages") Integer pages,
        @Query("quality") String quality,
        @Query("width") Integer width
    );
    
    // Get secure download link (after payment/auth)
    @Multipart
    @POST("api/notes/{id}/download")
    Call<DownloadLinkResponse> getDownloadLink(
        @Path("id") String noteId,
        @Part("payment_id") RequestBody paymentId,
        @Part("payment_method") RequestBody paymentMethod,
        @Part("validate_payment") RequestBody validatePayment
    );
    
    // Check access permissions
    @GET("api/notes/{id}/access")
    Call<AccessResponse> checkAccess(@Path("id") String noteId);
}

