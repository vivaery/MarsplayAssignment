package com.vsaery.marsplayassignment;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

public class GalleryViewAdapter extends RecyclerView.Adapter<GalleryViewAdapter.GalleryViewHolder> {

    private String[] mFilesPaths;
    private OnThumbnailClickListener mClickListener;

    public GalleryViewAdapter(String [] filePaths, OnThumbnailClickListener clickListener) {
        this.mFilesPaths = filePaths;
        this.mClickListener = clickListener;
    }

    public interface OnThumbnailClickListener {
        void onThumbnailClick(int position);
    }


    @NonNull
    @Override
    public GalleryViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.gallery_card, viewGroup, false);
        return new GalleryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GalleryViewHolder galleryViewHolder, int i) {
        galleryViewHolder.bind(i);
    }

    @Override
    public int getItemCount() {
        return mFilesPaths.length;
    }

    public class GalleryViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView thumbnail;

        public GalleryViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnail = itemView.findViewById(R.id.iv_thumbnail);
            itemView.setOnClickListener(this);
        }

        public void bind(int i) {
            Bitmap bmp = BitmapFactory.decodeFile(mFilesPaths[i]);
            thumbnail.setImageBitmap(bmp);
        }

        @Override
        public void onClick(View view) {
            if (view == itemView) {
                mClickListener.onThumbnailClick(getAdapterPosition());
            }
        }
    }
}
