package com.vsaery.marsplayassignment;

import android.Manifest;
import android.app.Dialog;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;

import java.io.File;
import java.security.Permission;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;

public class GalleryActivity extends AppCompatActivity implements GalleryViewAdapter.OnThumbnailClickListener {

    @BindView(R.id.rv_gallery)
    RecyclerView galleryView;
    @BindView(R.id.pb_load_gallery)
    ProgressBar galleryProgress;

    private String[] filesPaths;
    private String[] filesNames;

    private static final int READ_STORAGE_CODE = 2001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gallery);
        ButterKnife.bind(this);

        galleryView.setVisibility(View.INVISIBLE);
        galleryProgress.setVisibility(View.INVISIBLE);

        checkExternalStoragePermission();
    }

    private void checkExternalStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestReadExternalStoragePermission();
            } else {
                getImageList();
            }
        } else getImageList();
    }

    private void requestReadExternalStoragePermission() {
        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                READ_STORAGE_CODE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == READ_STORAGE_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            getImageList();
        }
    }

    private void getImageList() {
        galleryProgress.setVisibility(View.VISIBLE);
        File dir = new File(Environment.getExternalStorageDirectory().getAbsoluteFile() + "/Marsplay/.data");
        if (!dir.isDirectory()) {
            galleryProgress.setVisibility(View.INVISIBLE);
            Toast.makeText(this, getString(R.string.no_image_found), Toast.LENGTH_LONG).show();
            return;
        }
        File[] files = dir.listFiles();
        filesPaths = new String[files.length];
        filesNames = new String[files.length];

        if (files.length == 0) {
            Toast.makeText(this, getString(R.string.no_image_found), Toast.LENGTH_LONG).show();
            galleryProgress.setVisibility(View.INVISIBLE);
            return;
        }
        for (int i = 0; i < files.length; i++) {
            Log.e("File_mars", files[i].getAbsolutePath());
            filesPaths[i] = files[i].getAbsolutePath();
            filesNames[i] = files[i].getName();
        }

        setRecyclerView();
    }

    private void setRecyclerView() {
        galleryView.setLayoutManager(new GridLayoutManager(this, getGridSpanCount()));
        galleryView.setHasFixedSize(true);
        GalleryViewAdapter galleryViewAdapter = new GalleryViewAdapter(filesPaths, this);
        galleryProgress.setVisibility(View.INVISIBLE);
        galleryView.setVisibility(View.VISIBLE);
        galleryView.setAdapter(galleryViewAdapter);
    }

    private int getGridSpanCount() {
        Configuration configuration = getResources().getConfiguration();
        int screenWidthDp = configuration.screenWidthDp;
        if (screenWidthDp > getResources().getInteger(R.integer.smallestWidth700)) {
            return getResources().getInteger(R.integer.columnCount5);
        } else if (screenWidthDp > getResources().getInteger(R.integer.smallestWidth600)) {
            return getResources().getInteger(R.integer.columnCount4);
        } else {
            return getResources().getInteger(R.integer.columnCount3);
        }
    }

    @Override
    public void onThumbnailClick(int position) {
        showImageDialog(position);
    }

    private void showImageDialog(int position) {
        final Dialog dialog = new Dialog(this, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_full_image);
        ImageButton cancelDialog = dialog.findViewById(R.id.imageDialogCancel);
        PhotoView fullImage = dialog.findViewById(R.id.iv_full_image);
        final ProgressBar viewProgress = dialog.findViewById(R.id.pb_load_full_image);
        cancelDialog.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        PhotoViewAttacher attacher = new PhotoViewAttacher(fullImage);
        String url = getString(R.string.download_url_path) + filesNames[position];
        Log.e("IMAGE_URL", url);
        Glide.with(this)
                .load(url)
                .listener(new RequestListener<Drawable>() {
                    @Override
                    public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
                        viewProgress.setVisibility(View.GONE);
                        Toast.makeText(GalleryActivity.this, getString(R.string.error_msg), Toast.LENGTH_SHORT).show();
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
                        viewProgress.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(fullImage);
        attacher.update();
        dialog.show();
    }

}
