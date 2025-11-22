package com.return0.notes.data.mapper;

import com.return0.notes.data.remote.dto.NoteDto;
import com.return0.notes.domain.model.Note;
import java.util.ArrayList;
import java.util.List;

public class NoteMapper {
    public static Note toDomain(NoteDto dto) {
        if (dto == null) return null;
        Note note = new Note(
                dto.getId(),
                dto.getTitle(),
                dto.getFilename(),
                dto.getUploadDate(),
                dto.getFileSize(),
                dto.getSubject(),
                dto.getSemester() != null ? dto.getSemester() : "",
                dto.getBranch() != null ? dto.getBranch() : "",
                dto.getCollege() != null ? dto.getCollege() : "",
                dto.getThumbnail() != null ? dto.getThumbnail() : ""
        );
        if (dto.getFilePath() != null) {
            note.setFilePath(dto.getFilePath());
        }
        return note;
    }

    public static List<Note> toDomainList(List<NoteDto> dtos) {
        if (dtos == null) return new ArrayList<>();
        List<Note> notes = new ArrayList<>();
        for (NoteDto dto : dtos) {
            notes.add(toDomain(dto));
        }
        return notes;
    }
}

