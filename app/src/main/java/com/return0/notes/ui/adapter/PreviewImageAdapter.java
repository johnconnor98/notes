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
import com.return0.notes.data.remote.dto.PreviewResponse;

public class PreviewImageAdapter extends ListAdapter<PreviewResponse.PreviewImage, PreviewImageAdapter.PreviewImageViewHolder> {
    
    public PreviewImageAdapter() {
        super(new PreviewImageDiffCallback());
    }

    @NonNull
    @Override
    public PreviewImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_preview_image, parent, false);
        return new PreviewImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PreviewImageViewHolder holder, int position) {
        holder.bind(getItem(position));
    }

    static class PreviewImageViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView tvPageNumber;

        PreviewImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.ivPreview);
            tvPageNumber = itemView.findViewById(R.id.tvPageNumber);
        }

        void bind(PreviewResponse.PreviewImage image) {
            tvPageNumber.setText("Page " + image.getPageNumber());
            Glide.with(itemView.getContext())
                    .load(image.getImageUrl())
                    .placeholder(R.drawable.ic_pdf)
                    .error(R.drawable.ic_pdf)
                    .into(imageView);
        }
    }

    private static class PreviewImageDiffCallback extends DiffUtil.ItemCallback<PreviewResponse.PreviewImage> {
        @Override
        public boolean areItemsTheSame(@NonNull PreviewResponse.PreviewImage oldItem, 
                                       @NonNull PreviewResponse.PreviewImage newItem) {
            return oldItem.getPageNumber() == newItem.getPageNumber();
        }

        @Override
        public boolean areContentsTheSame(@NonNull PreviewResponse.PreviewImage oldItem, 
                                         @NonNull PreviewResponse.PreviewImage newItem) {
            return oldItem.getImageUrl().equals(newItem.getImageUrl());
        }
    }
}

