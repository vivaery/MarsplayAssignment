package com.vsaery.marsplayassignment;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.StrictMode;
import android.provider.MediaStore;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.services.s3.AmazonS3Client;
import com.github.chrisbanes.photoview.PhotoView;
import com.github.chrisbanes.photoview.PhotoViewAttacher;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.theartofdev.edmodo.cropper.CropImage;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import butterknife.BindView;
import butterknife.ButterKnife;

public class MainActivity extends AppCompatActivity {

    @BindView(R.id.btn_select_image)
    Button selectionButton;
    @BindView(R.id.btn_upload)
    Button uploadButton;
    @BindView(R.id.iv_image)
    PhotoView imageView;
    @BindView(R.id.pb_upload_progress)
    ProgressBar uploadProgress;
    @BindView(R.id.linearLayout)
    LinearLayout buttonLayout;
    @BindView(R.id.fab_gallery)
    FloatingActionButton openGalleryButton;

    private Bitmap image;
    private String picturePath;
    private Uri imageUri;
    private boolean fromCamera = true;
    private PhotoViewAttacher attacher;
    private boolean canEditImage = false;
    private String camImageName;


    public static final int CAMERA_PERM_REQUEST_CODE = 1001;
    public static final int CAMERA_PIC_REQUEST_CODE = 1002;
    public static final int READ_STORAGE_CODE = 1003;
    public static final int GALLERY_PIC_REQUEST_CODE = 1005;

    private static final String AWS_KEY = "AKIAIR4OXJDS54L66L6A";
    private static final String SECRET_KEY = "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";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
        StrictMode.setVmPolicy(builder.build());


        attacher = new PhotoViewAttacher(imageView);
        uploadProgress.setVisibility(View.INVISIBLE);

        selectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                openDialog();
            }
        });

        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                uploadImage();
            }
        });

        openGalleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, GalleryActivity.class);
                startActivity(intent);
            }
        });
        initiateAWS();


    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    private void openDialog() {
        AlertDialog.Builder selectionDialog = new AlertDialog.Builder(this);
        selectionDialog.setTitle("Upload Picture options");
        selectionDialog.setPositiveButton("Gallery", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                requestExternalStoragePermission();
            }
        });

        selectionDialog.setNegativeButton("Camera", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                checkCameraPermission();
            }
        });

        selectionDialog.show();
    }

    private void initiateAWS() {
        AWSMobileClient.getInstance().initialize(this).execute();
    }

    private void hideEditButton() {
        canEditImage = false;
        invalidateOptionsMenu();
    }

    private void showEditButton() {
        canEditImage = true;
        invalidateOptionsMenu();
    }

    private void uploadImage() {
        hideEditButton();
        showProgress();
        if (!isNetworkAvailable()) {
            Toast.makeText(this, "Please connect to internet.", Toast.LENGTH_SHORT).show();
        }
        BasicAWSCredentials credentials = new BasicAWSCredentials(AWS_KEY, SECRET_KEY);
        AmazonS3Client s3Client = new AmazonS3Client(credentials);

        TransferUtility transferUtility = TransferUtility.builder()
                .context(getApplicationContext())
                .awsConfiguration(AWSMobileClient.getInstance().getConfiguration())
                .s3Client(s3Client)
                .build();

        File file = new File(picturePath);
        final String fileName = picturePath.substring(picturePath.lastIndexOf("/") + 1);
        TransferObserver observer = transferUtility.upload("mobileUploads/" + fileName, file);

        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                if (state == TransferState.COMPLETED) {
                    Toast.makeText(MainActivity.this, "Image has been uploaded", Toast.LENGTH_SHORT).show();
                    uploadButton.setVisibility(View.GONE);
                    saveThumbnail(fileName);
                    hideProgress();
                    showEditButton();
                }
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                float percentDonef = ((float) bytesCurrent / (float) bytesTotal) * 100;
                int percentDone = (int) percentDonef;
                uploadProgress.setProgress(percentDone);
            }

            @Override
            public void onError(int id, Exception ex) {
                hideProgress();
                showEditButton();
                Toast.makeText(MainActivity.this, "Image upload failed, please try again", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showProgress() {
        buttonLayout.setVisibility(View.INVISIBLE);
        uploadProgress.setVisibility(View.VISIBLE);
    }

    private void hideProgress() {
        uploadProgress.setVisibility(View.INVISIBLE);
        buttonLayout.setVisibility(View.VISIBLE);
    }

    private void saveThumbnail(String fileName) {
        File dir = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                + "/Marsplay/.data/");
        checkIfDirExists(dir);
        if (fromCamera) {
            File file = new File(Environment.getExternalStorageDirectory().getAbsolutePath() + "/Marsplay/",camImageName );
            boolean deleted = file.delete();
        }

        int thumbnailSize = 150;
        Bitmap thumbnail = ThumbnailUtils.extractThumbnail(image, thumbnailSize, thumbnailSize);

        File thumbnailPath = new File(dir, fileName);

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(thumbnailPath);
            thumbnail.compress(Bitmap.CompressFormat.PNG, 100, fos);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void checkIfDirExists(File dir) {
        if (!dir.isDirectory())
            dir.mkdirs();
    }


    private void requestExternalStoragePermission() {

        String[] PERMISSIONS = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, READ_STORAGE_CODE);
        } else openGallery();
    }

    private void checkCameraPermission() {
        String[] PERMISSIONS = {
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA
        };

        if (!hasPermissions(this, PERMISSIONS)) {
            ActivityCompat.requestPermissions(this, PERMISSIONS, CAMERA_PERM_REQUEST_CODE);
        } else initiateCamera();
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == READ_STORAGE_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            openGallery();

        else if (requestCode == CAMERA_PERM_REQUEST_CODE) {
            for (int i = 0; i < grantResults.length; i++) {
                if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                    return;
            }
            initiateCamera();
        }
    }

    private void openGallery() {
        Intent i = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(i, GALLERY_PIC_REQUEST_CODE);
    }

    private void initiateCamera() {
        Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        imageUri = getImageUri();
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
        startActivityForResult(cameraIntent, CAMERA_PIC_REQUEST_CODE);
    }

    private Uri getImageUri() {
        Uri retUri = null;
        File mFile;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss");
            mFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath()
                    + "/Marsplay/");
            checkIfDirExists(mFile);
            File imageFile = new File(mFile, sdf.format(new Date()) + ".jpg");
            camImageName = sdf.format(new Date()) + ".jpg";
            retUri = Uri.fromFile(imageFile);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return retUri;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (image != null)
            image = null;

        if (requestCode == CAMERA_PIC_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            fromCamera = true;
            CropImage.activity(imageUri).start(this);
        }
        if (requestCode == GALLERY_PIC_REQUEST_CODE && resultCode == Activity.RESULT_OK && data != null) {
            imageUri = data.getData();
            fromCamera = false;
            CropImage.activity(imageUri).start(this);
        }

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {
                imageUri = result.getUri();
                showSelectedImage();
                uploadButton.setVisibility(View.VISIBLE);
            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                error.printStackTrace();
            }
        }
    }

    private void showSelectedImage() {
        picturePath = imageUri.getPath();
        image = BitmapFactory.decodeFile(picturePath);
        imageView.setImageBitmap(image);
        attacher.update();
        showEditButton();
        Toast.makeText(this, "Pinch to zoom in/out.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        if (canEditImage)
            menu.findItem(R.id.edit_image).setVisible(true);
        else
            menu.findItem(R.id.edit_image).setVisible(false);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.edit_image:
                CropImage.activity(imageUri).start(this);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}
