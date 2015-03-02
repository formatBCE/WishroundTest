package ua.pp.formatbce.wishroundtest;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.facebook.Request;
import com.facebook.Session;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import butterknife.ButterKnife;
import butterknife.InjectView;

/**
 * Created by format on 27.02.2015
 */
public class FBPostFragment extends DialogFragment {

    private String mCurrentPhotoPath;
    private Bitmap toPost;

    @InjectView(R.id.imageView)
    ImageView iv;
    @InjectView(R.id.editTextPost)
    EditText post;
    @InjectView(R.id.editTextLink)
    EditText link;
    @InjectView(R.id.btnCancelPost)
    Button cancelPost;
    @InjectView(R.id.btnDoPost)
    Button doPost;
    private static final int REQUEST_PHOTO = 23;
    private static ProgressDialog progress;


    public FBPostFragment() {
    }


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_fb_post, null);
        ButterKnife.inject(this, root);
        getDialog().setTitle("Post to Facebook");
        cancelPost.setOnClickListener((v) -> dismiss());
        doPost.setOnClickListener((v) -> initiatePosting());
        iv.setOnClickListener((v) -> dispatchTakePictureIntent());
        post.setText("Wishround - некоторые вещи лучше делать вместе");
        link.setText("http://wishround.com");
        progress = new ProgressDialog(getActivity());
        progress.setTitle("Posting");
        progress.setCancelable(false);
        progress.setIndeterminate(true);
        progress.setMessage("Posting to Facebook in progress");
        return root;
    }

    private void initiatePosting() {
        if (toPost == null) {
            Toast.makeText(getActivity(), "Make picture first!", Toast.LENGTH_SHORT).show();
            return;
        }
        if (!LoginFragment.checkPermissions()) {
            Toast.makeText(getActivity(), "Don't have permissions for posting, re-login please", Toast.LENGTH_SHORT).show();
            return;
        }
        if(!progress.isShowing()){
            progress.show();
        }
        Request r = Request.newUploadPhotoRequest(Session.getActiveSession(), toPost, response -> {
            progress.dismiss();
            this.dismiss();
            Toast.makeText(getActivity(), "Posted successfully", Toast.LENGTH_SHORT).show();
        });
        Bundle bundle = r.getParameters();
        bundle.putString("caption", post.getText().toString() + "\n" + link.getText().toString());
        r.executeAsync();
    }

    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getActivity().getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                Toast.makeText(getActivity(), "SD card missing, or app cannot write to it", Toast.LENGTH_SHORT).show();
            }
            if (photoFile != null) {
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(takePictureIntent, REQUEST_PHOTO);
            } else {
                Toast.makeText(getActivity(), "Failed to create temp photo file", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,
                ".jpg",
                storageDir
        );
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    private Bitmap decodeBitmap() {
        BitmapFactory.Options bmOptions = new BitmapFactory.Options();
        bmOptions.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
        int photoW = bmOptions.outWidth;
        int photoH = bmOptions.outHeight;
        int scaleFactor;
        if(photoW > photoH){
            //1200x600
            scaleFactor = Math.max(photoW / 1200, photoH / 600);
        } else {
            //600x1200
            scaleFactor = Math.max(photoW / 600, photoH / 1200);
        }
        bmOptions.inJustDecodeBounds = false;
        bmOptions.inSampleSize = scaleFactor;
        Log.e("SCALE", "" + scaleFactor);
        return BitmapFactory.decodeFile(mCurrentPhotoPath, bmOptions);
    }


    @Override
    public synchronized void onActivityResult(final int requestCode,
                                              int resultCode, final Intent data) {
        if (requestCode == REQUEST_PHOTO && resultCode == Activity.RESULT_OK) {
            toPost = decodeBitmap();
            Toast.makeText(getActivity(), toPost.getWidth() + "x" + toPost.getHeight(), Toast.LENGTH_SHORT).show();
            iv.setImageBitmap(toPost);
        }
    }
}
