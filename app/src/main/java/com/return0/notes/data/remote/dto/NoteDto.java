package com.return0.notes.data.remote.dto;

import com.google.gson.annotations.SerializedName;

public class NoteDto {
    @SerializedName("id")
    private String id;

    @SerializedName("notesid")
    private String notesid;

    @SerializedName("title")
    private String title;

    @SerializedName("filename")
    private String filename;

    @SerializedName("upload_date")
    private String uploadDate;

    @SerializedName("file_size")
    private long fileSize;

    @SerializedName("subject")
    private String subject;

    @SerializedName("semester")
    private String semester;

    @SerializedName("branch")
    private String branch;

    @SerializedName("college")
    private String college;

    @SerializedName("thumbnail")
    private String thumbnail;

    @SerializedName("file_path")
    private String filePath;

    @SerializedName("file_url")
    private String fileUrl;

    @SerializedName("thumbnail_url")
    private String thumbnailUrl;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNotesid() {
        return notesid;
    }

    public void setNotesid(String notesid) {
        this.notesid = notesid;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(String uploadDate) {
        this.uploadDate = uploadDate;
    }

    public long getFileSize() {
        return fileSize;
    }

    public void setFileSize(long fileSize) {
        this.fileSize = fileSize;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getSemester() {
        return semester;
    }

    public void setSemester(String semester) {
        this.semester = semester;
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public String getCollege() {
        return college;
    }

    public void setCollege(String college) {
        this.college = college;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getFileUrl() {
        return fileUrl;
    }

    public void setFileUrl(String fileUrl) {
        this.fileUrl = fileUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
}

