package com.return0.notes.data.remote;

import com.return0.notes.data.remote.dto.NoteDto;
import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
import retrofit2.http.Path;
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
    Call<NoteDto> uploadNoteWithFirebase(
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
}

