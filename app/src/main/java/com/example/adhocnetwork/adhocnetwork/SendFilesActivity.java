package com.example.adhocnetwork.adhocnetwork;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import android.content.ContentResolver;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.Toast;

import com.google.android.gms.nearby.connection.Payload;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

public class SendFilesActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int TAKEPHOTO_REQUEST_CODE = 0;
    private static final int PICKIMAGE_REQUEST_CODE = 1;
    private static final int PICKFILE_REQUEST_CODE = 2;
    private ConnectionService mCon;
    private String currentPhotoPath;
    private Uri photourii;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_send_files);
        mCon = ConnectionService.getInstance();

        Button mClickButton1 = (Button)findViewById(R.id.btnTakePhoto);
        mClickButton1.setOnClickListener(this);
        Button mClickButton2 = (Button)findViewById(R.id.btnChooseGallery);
        mClickButton2.setOnClickListener(this);
        Button mClickButton3 = (Button)findViewById(R.id.btnChooseFolder);
        mClickButton3.setOnClickListener(this);
        Button mClickButton4 = (Button)findViewById(R.id.btnCloseTransfer);
        mClickButton4.setOnClickListener(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case  R.id.btnTakePhoto: {

                dispatchTakePictureIntent();
                break;
            }
            case R.id.btnChooseGallery: { //FROM GALLERY
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto , PICKIMAGE_REQUEST_CODE);
                break;
            }
            case R.id.btnChooseFolder: {//FROM FOLDER
                 Intent pickFile = new Intent(Intent.ACTION_GET_CONTENT);
                pickFile.setType("*/*");
                startActivityForResult(pickFile, PICKFILE_REQUEST_CODE);
                break;
            }
            case R.id.btnCloseTransfer: {
                finish();
                break;
            }
        }
    }
    //Methods to create image file outside the app, when shot with camera
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = SimpleDateFormat.getDateTimeInstance().format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.e("MyApp", "Failed to create file");
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "com.example.adhocnetwork.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                photourii = photoURI;
                startActivityForResult(takePictureIntent, TAKEPHOTO_REQUEST_CODE);
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK && data != null) {
            String filenameMessage;
            Payload filenameBytesPayload;
            switch (requestCode) {
                case TAKEPHOTO_REQUEST_CODE:
                    Payload imageFilePayload;
                    try {
                        // Open the ParcelFileDescriptor for this URI with read access.
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(photourii, "r");
                        imageFilePayload = Payload.fromFile(pfd);
                    } catch (FileNotFoundException | NullPointerException e) {
                        Log.e("MyApp", "Media File not found", e);
                        Toast.makeText(this, "IT FAILED", Toast.LENGTH_LONG).show();
                        return;
                    }
                    String extension0 = getExtensionType(photourii);
                    // Construct a simple message mapping the ID of the file payload to the desired filename.
                    filenameMessage = imageFilePayload.getId() + ":" + photourii.getLastPathSegment();
                    // Send the filename message as a bytes payload.
                    filenameBytesPayload = Payload.fromBytes(filenameMessage.getBytes(StandardCharsets.UTF_8));

                    // Finally, send the filename and file payload.
                    mCon.send(filenameBytesPayload);
                    mCon.send(imageFilePayload);
                    Toast.makeText(this, "IMG:"+filenameMessage, Toast.LENGTH_LONG).show();
                    break;
                case PICKIMAGE_REQUEST_CODE:
                    Uri uri_media = data.getData();

                    Payload mediaFilePayload;
                    try {
                        // Open the ParcelFileDescriptor for this URI with read access.
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri_media, "r");
                        mediaFilePayload = Payload.fromFile(pfd);
                    } catch (FileNotFoundException | NullPointerException e) {
                        Log.e("MyApp", "Media File not found", e);
                        return;
                    }
                    String extension1 = getExtensionType(uri_media);
                    filenameMessage = mediaFilePayload.getId() + ":" + uri_media.getLastPathSegment() +"."+ extension1;
                    filenameBytesPayload = Payload.fromBytes(filenameMessage.getBytes(StandardCharsets.UTF_8));

                    mCon.send(filenameBytesPayload);
                    mCon.send(mediaFilePayload);
                    Toast.makeText(this, "IMG:"+filenameMessage, Toast.LENGTH_LONG).show();
                    break;
                case PICKFILE_REQUEST_CODE:
                    // The URI of the file selected by the user.
                    Uri uri_file = data.getData();
                    Payload filePayload;
                    try {
                        // Open the ParcelFileDescriptor for this URI with read access.
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(uri_file, "r");
                        filePayload = Payload.fromFile(pfd);
                    } catch (FileNotFoundException | NullPointerException e) {
                        Log.e("MyApp", "File not found", e);
                        return;
                    }
                    String extension2 = getExtensionType(uri_file);
                    filenameMessage = filePayload.getId() + ":" + uri_file.getLastPathSegment() +"."+ extension2;
                    filenameBytesPayload = Payload.fromBytes(filenameMessage.getBytes(StandardCharsets.UTF_8));

                    mCon.send(filenameBytesPayload);
                    mCon.send(filePayload);
                    Toast.makeText(this, "FILE:"+filenameMessage, Toast.LENGTH_LONG).show();
                    break;
            }
        }
        if (resultCode == RESULT_OK && data == null) { // extra case when using media_output
            String filenameMessage;
            Payload filenameBytesPayload;
            Payload imageFilePayload;
            try {
                // Open the ParcelFileDescriptor for this URI with read access.
                ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(photourii, "r");
                imageFilePayload = Payload.fromFile(pfd);
            } catch (FileNotFoundException | NullPointerException e) {
                Log.e("MyApp", "Media File not found", e);
                Toast.makeText(this, "IT FAILED", Toast.LENGTH_LONG).show();
                return;
            }
            //String extension0 = getExtensionType(photourii);
            // Construct a simple message mapping the ID of the file payload to the desired filename.
            filenameMessage = imageFilePayload.getId() + ":" + photourii.getLastPathSegment();
            // Send the filename message as a bytes payload.
            filenameBytesPayload = Payload.fromBytes(filenameMessage.getBytes(StandardCharsets.UTF_8));

            // Finally, send the filename and file payload.
            mCon.send(filenameBytesPayload);
            mCon.send(imageFilePayload);
            Toast.makeText(this, "IMG:"+filenameMessage, Toast.LENGTH_LONG).show();
        }
    }

    public String getExtensionType(Uri uri) {
        String extension;

        if (uri.getScheme().equals(ContentResolver.SCHEME_CONTENT)) {
            final MimeTypeMap mime = MimeTypeMap.getSingleton();
            extension = mime.getExtensionFromMimeType(this.getContentResolver().getType(uri));
        } else {
            extension = MimeTypeMap.getFileExtensionFromUrl(Uri.fromFile(new File(uri.getPath())).toString());
        }
        return extension;
    }

}