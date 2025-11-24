package com.return0.notes.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.return0.notes.R;
import com.return0.notes.domain.model.Note;
import com.return0.notes.util.Constants;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NoteAdapter extends ListAdapter<Note, NoteAdapter.NoteViewHolder> {
    private OnDownloadClickListener downloadListener;
    private OnItemClickListener itemClickListener;

    public interface OnDownloadClickListener {
        void onDownloadClick(Note note);
    }

    public interface OnItemClickListener {
        void onItemClick(Note note);
    }

    public NoteAdapter() {
        super(new NoteDiffCallback());
    }

    public void setOnDownloadClickListener(OnDownloadClickListener listener) {
        this.downloadListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }

    @NonNull
    @Override
    public NoteViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_note, parent, false);
        return new NoteViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NoteViewHolder holder, int position) {
        Note note = getItem(position);
        holder.bind(note);
    }

    class NoteViewHolder extends RecyclerView.ViewHolder {
        private final ImageView ivThumbnail;
        private final TextView tvTitle;
        private final TextView tvSubject;
        private final TextView tvDetails;
        private final TextView tvDate;
        private final TextView tvSize;
        private final com.google.android.material.button.MaterialButton btnDownload;

        NoteViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumbnail = itemView.findViewById(R.id.ivThumbnail);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvSubject = itemView.findViewById(R.id.tvSubject);
            tvDetails = itemView.findViewById(R.id.tvDetails);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvSize = itemView.findViewById(R.id.tvSize);
            btnDownload = itemView.findViewById(R.id.btnDownload);
        }

        void bind(Note note) {
            tvTitle.setText(note.getTitle());
            tvSubject.setText("Subject: " + note.getSubject());
            
            StringBuilder details = new StringBuilder();
            if (note.getSemester() != null && !note.getSemester().isEmpty()) {
                details.append("Sem: ").append(note.getSemester());
            }
            if (note.getBranch() != null && !note.getBranch().isEmpty()) {
                if (details.length() > 0) details.append(" | ");
                details.append("Branch: ").append(note.getBranch());
            }
            if (note.getCollege() != null && !note.getCollege().isEmpty()) {
                if (details.length() > 0) details.append(" | ");
                details.append(note.getCollege());
            }
            tvDetails.setText(details.toString());
            
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                Date date = inputFormat.parse(note.getUploadDate());
                tvDate.setText(outputFormat.format(date));
            } catch (Exception e) {
                tvDate.setText(note.getUploadDate());
            }
            
            long sizeKB = note.getFileSize() / 1024;
            tvSize.setText(sizeKB + " KB");
            
            if (note.getThumbnail() != null && !note.getThumbnail().isEmpty()) {
                String thumbnailUrl = Constants.BASE_URL + "api/thumbnails/" + note.getThumbnail();
                Glide.with(itemView.getContext())
                        .load(thumbnailUrl)
                        .placeholder(R.drawable.ic_launcher_background)
                        .error(R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(ivThumbnail);
            } else {
                ivThumbnail.setImageResource(R.drawable.ic_launcher_background);
            }
            
            btnDownload.setOnClickListener(v -> {
                if (downloadListener != null) {
                    downloadListener.onDownloadClick(note);
                }
            });
            
            // Make entire item clickable
            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(note);
                }
            });
        }
    }

    private static class NoteDiffCallback extends DiffUtil.ItemCallback<Note> {
        @Override
        public boolean areItemsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.getId().equals(newItem.getId());
        }

        @Override
        public boolean areContentsTheSame(@NonNull Note oldItem, @NonNull Note newItem) {
            return oldItem.equals(newItem);
        }
    }
}

