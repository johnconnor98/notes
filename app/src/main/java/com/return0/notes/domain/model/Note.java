package com.return0.notes.domain.model;

public class Note {
    private String id;
    private String title;
    private String filename;
    private String uploadDate;
    private long fileSize;
    private String subject;
    private String semester;
    private String branch;
    private String college;
    private String thumbnail;
    private String filePath;

    public Note() {}

    public Note(String id, String title, String filename, String uploadDate, long fileSize, 
                String subject, String semester, String branch, String college, String thumbnail) {
        this.id = id;
        this.title = title;
        this.filename = filename;
        this.uploadDate = uploadDate;
        this.fileSize = fileSize;
        this.subject = subject;
        this.semester = semester;
        this.branch = branch;
        this.college = college;
        this.thumbnail = thumbnail;
    }

    public Note(String id, String title, String filename, String uploadDate, long fileSize, 
                String subject, String semester, String branch, String college, String thumbnail, String filePath) {
        this.id = id;
        this.title = title;
        this.filename = filename;
        this.uploadDate = uploadDate;
        this.fileSize = fileSize;
        this.subject = subject;
        this.semester = semester;
        this.branch = branch;
        this.college = college;
        this.thumbnail = thumbnail;
        this.filePath = filePath;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Note note = (Note) o;
        return fileSize == note.fileSize &&
                id != null && id.equals(note.id) &&
                title != null && title.equals(note.title) &&
                filename != null && filename.equals(note.filename) &&
                uploadDate != null && uploadDate.equals(note.uploadDate) &&
                subject != null && subject.equals(note.subject) &&
                (semester == null ? note.semester == null : semester.equals(note.semester)) &&
                (branch == null ? note.branch == null : branch.equals(note.branch)) &&
                (college == null ? note.college == null : college.equals(note.college)) &&
                (thumbnail == null ? note.thumbnail == null : thumbnail.equals(note.thumbnail));
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (title != null ? title.hashCode() : 0);
        result = 31 * result + (filename != null ? filename.hashCode() : 0);
        result = 31 * result + (uploadDate != null ? uploadDate.hashCode() : 0);
        result = 31 * result + (int) (fileSize ^ (fileSize >>> 32));
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (semester != null ? semester.hashCode() : 0);
        result = 31 * result + (branch != null ? branch.hashCode() : 0);
        result = 31 * result + (college != null ? college.hashCode() : 0);
        result = 31 * result + (thumbnail != null ? thumbnail.hashCode() : 0);
        return result;
    }
}

