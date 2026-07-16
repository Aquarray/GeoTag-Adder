package org.feystray.geotagger;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.provider.MediaStore;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.slider.Slider;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;
import com.google.common.collect.BiMap;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicReference;

public class PreviewActivity extends AppCompatActivity {

    ImageView imgV;

    Boolean EditMode = false;

    DialogsHandler dialogsHandler;
    SharedPreferences sharedPreferences;

    OverlayHandler overlayHandler;

    List<Image> images = new ArrayList<>();

    ImageDetails imgData;



    @RequiresApi(api = Build.VERSION_CODES.VANILLA_ICE_CREAM)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_preview);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        dialogsHandler = new DialogsHandler(this);
        sharedPreferences = getSharedPreferences("overlay", MODE_PRIVATE);
        overlayHandler = new OverlayHandler();
        imgV = findViewById(R.id.imageView);

        String data = getIntent().getStringExtra("URI");
        if(data==null) {
            Toast.makeText(this, "Invalid Image", Toast.LENGTH_SHORT).show();
            finish();
        }
        String[] imageURI = data.split(";");
        for (String x : imageURI){
            images.add(new Image(RoatateImageIFExif(x) , LocalDateTime.now()));
        }
        if (hasMetaSaved()){
            loadFromSharedPrefs();
            PreviewNewImage();
        }else{
            imgV.setImageBitmap(images.getLast().originalImage);
        }

        Button editButton = findViewById(R.id.editButton);
        Button moreOptions = findViewById(R.id.moreOptions);
        Slider textSizeSlide = findViewById(R.id.textSizeSlider);
        FloatingActionButton saveButton = (FloatingActionButton) findViewById(R.id.saveButton);

        editButton.setOnClickListener(v->{
            if (!EditMode){
                editButton.setText("Save");
                moreOptions.setVisibility(VISIBLE);
                ((LinearLayout) findViewById(R.id.textEditLayout)).setVisibility(VISIBLE);
                if (imgData != null){
                    ((Slider) findViewById(R.id.textSizeSlider)).setValue(imgData.textSize);
                }
                saveButton.setVisibility(GONE);
            }else{
                saveImage();
            }
            EditMode = !EditMode;
        });
        ((FloatingActionButton) findViewById(R.id.saveButton)).setOnClickListener(v->saveImage());
        moreOptions.setOnClickListener(v->{
            dialogsHandler.showLocationAndTimeDialog(imgData, getSupportFragmentManager(),
                    images.size() > 1,
                    images.get(0).time
                    ,new DialogsHandler.LocationandTimeDialogEvent() {
                @Override
                public void onSave(float lat, float lon, String short_Addr, LocalDateTime datenTime) {
                    autoFillDetails(lat, lon, short_Addr, new autFillDetailsEvents() {
                        @Override
                        public void onSuccess(ImageDetails img) {
                            if (imgData != null){
                                img.textSize = imgData.textSize;
                            }
                            saveSharedPrefs(img);
                            imgData = img;
                            if (datenTime != null){
                                images.get(0).time = datenTime;
                            }
                            PreviewNewImage();
                        }

                        @Override
                        public void onError(String API) {
                            Toast.makeText(PreviewActivity.this, "Unable to Reach "+API+" API", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }

                @Override
                public void onSave(float lat, float lon, String short_Addr, LocalDateTime fromTime, LocalDateTime toTime) {
                    autoFillDetails(lat, lon, short_Addr, new autFillDetailsEvents() {
                        @Override
                        public void onSuccess(ImageDetails img) {
                            if (imgData != null){
                                img.textSize = textSizeSlide.getValue();
                            }
                            imgData = img;
                            saveSharedPrefs(img);
                            long maxminutes = Duration.between(fromTime, toTime).toMinutes();
                            for(int i=0; i< images.size(); i++){
                                Image im = images.get(i);
                                im.time= fromTime.plusMinutes(maxminutes- new Random().nextLong(maxminutes));
                                images.set(i, im);
                            }
                            PreviewNewImage();

                        }

                        @Override
                        public void onError(String API) {
                            Toast.makeText(PreviewActivity.this, "Unable to Reach "+API+" API", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    });
                }

                @Override
                public void onCancel() {

                }
            });
        });
        textSizeSlide.addOnChangeListener((slider, v, b) -> {
            Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            vibrator.cancel();
            vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
        });
        textSizeSlide.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {

            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                if (imgData!=null ) imgData.textSize = slider.getValue();
                PreviewNewImage();
            }
        });
        saveButton.setOnClickListener(v->saveImage());
    }

    private Bitmap RoatateImageIFExif(String uri){
        Uri imageUri = Uri.parse(uri);
        try {
            java.io.InputStream inputStream = getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
            if (inputStream != null) inputStream.close();

            java.io.InputStream exifStream = getContentResolver().openInputStream(imageUri);
            int rotationDegrees = 0;
            if (exifStream != null) {
                // Requires androidx.exifinterface:exifinterface dependency
                // Or android.media.ExifInterface if targeting API 24+ directly from stream
                androidx.exifinterface. media.ExifInterface exif = new androidx.exifinterface.media.ExifInterface(exifStream);
                int orientation = exif.getAttributeInt(
                        androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION,
                        androidx.exifinterface.media.ExifInterface.ORIENTATION_NORMAL
                );

                if (orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_90) rotationDegrees = 90;
                else if (orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_180) rotationDegrees = 180;
                else if (orientation == androidx.exifinterface.media.ExifInterface.ORIENTATION_ROTATE_270) rotationDegrees = 270;

                exifStream.close();
            }

            // 4. Rotate the image if it is detected as sideways landscape
            if (rotationDegrees != 0) {
                Matrix matrix = new Matrix();
                matrix.postRotate(rotationDegrees);
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);

                // Display the corrected portrait image
                return rotatedBitmap;
            } else {
                // Display directly if it is already oriented correctly
                return bitmap;
            }

        } catch (Exception e) {
            Log.e("PreviewActivity", "Error loading or rotating image: " + e.getMessage());
        }
        return null;
    }


    private boolean hasMetaSaved(){
        String shortAddr = sharedPreferences.getString("shortAddr", null);
        String longAddr = sharedPreferences.getString("longAddr", null);
        Float lat = sharedPreferences.getFloat("lat", 0.0f);
        Float lon = sharedPreferences.getFloat("lon", 0.0f);
        if (lat == 0.0f || lon == 0.0f ) return false;
        else if (longAddr == null || shortAddr == null){
            autoFillDetails(lat, lon, null, new autFillDetailsEvents() {
                @Override
                public void onSuccess(ImageDetails img) {

                }

                @Override
                public void onError(String API) {

                }
            });
            return false;
        }
        return true;
    }
    private void loadFromSharedPrefs(){
        String shortAddr = sharedPreferences.getString("shortAddr", null);
        String longAddr = sharedPreferences.getString("longAddr", null);
        float lat = sharedPreferences.getFloat("lat", 0.0f);
        float lon = sharedPreferences.getFloat("lon", 0.0f);
        float textSize = sharedPreferences.getFloat("textSize", 1.2f);
        imgData = new ImageDetails();
        imgData.shortAddr = shortAddr;
        imgData.displayName = longAddr;
        imgData.latitude = lat;
        imgData.longitude = lon;
        imgData.textSize = textSize;
    }

    public void saveSharedPrefs(ImageDetails imgData){
        sharedPreferences.edit().putString("shortAddr", imgData.shortAddr).apply();
        sharedPreferences.edit().putString("longAddr", imgData.displayName).apply();
        sharedPreferences.edit().putFloat("lat", (float) imgData.latitude).apply();
        sharedPreferences.edit().putFloat("lon", (float) imgData.longitude).apply();
    }

    private interface autFillDetailsEvents{
        void onSuccess(ImageDetails img);
        void onError(String API);
    }
    private void autoFillDetails(float lat, float lon, String short_addr, autFillDetailsEvents callback){
        OverlayHandler overlayHandler = new OverlayHandler();
        DialogsHandler.LoadingDialogEvent loadingDialog = dialogsHandler.LoadingDialog();
        loadingDialog.show();
        new Thread(() -> {
            ImageDetails x = overlayHandler.fetchDetails(lat, lon);;
            x.mapSnapPath = overlayHandler.FetchImage(lat, lon);
            runOnUiThread(()->{
                if (x.displayName == null) {
                    callback.onError("OpenStreet");
                }
                if (x.mapSnapPath == null) {
//                    Toast.makeText(this, "Unable to Reach Google Maps API", Toast.LENGTH_SHORT).show();
//                    finish();
                    callback.onError("Google Maps");
                }
                if (short_addr != null) x.shortAddr = short_addr;
//                imgData= x;
//                saveSharedPrefs(x);
                callback.onSuccess(x);
                loadingDialog.dismiss();
            });
        }).start();
    }
    public void saveImage(){
        sharedPreferences.edit().putFloat("textSize", imgData.textSize).apply();
        DrawOverlaysOnAll(new DrawOverALLEvents() {
            @Override
            public void onSuccess() {
                for (Image i : images){
                    ContentValues contentValues = new ContentValues();
                    contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "GeoTagged_"+ System.currentTimeMillis() + ".jpg");
                    contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
                    contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/GeoTagged_Capture");
//        contentValues.put(MediaStore.Images.Media.IS_PENDING, true);
                    Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
                    if (uri != null){
                        try {
                            OutputStream os = getContentResolver().openOutputStream(uri);
                            i.editedImage.compress(Bitmap.CompressFormat.JPEG, 100, os);
                            os.flush();
                            os.close();
                            //Add Exif Data
                            ExifInterface exifInterface = new ExifInterface(Objects.requireNonNull(getContentResolver().openFileDescriptor(uri, "rw")).getFileDescriptor());
                            String latRef = imgData.latitude >= 0 ? "N" : "S";
                            String lonRef = imgData.longitude >= 0 ? "E" : "W";
                            String latExif = convertToDmsString(Math.abs(imgData.latitude));
                            String lonExif = convertToDmsString(Math.abs(imgData.longitude));
                            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE, latExif);
                            exifInterface.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, latRef);
                            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, lonExif);
                            exifInterface.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, lonRef);
                            exifInterface.saveAttributes();
                        } catch (IOException e) {
                            Toast.makeText(PreviewActivity.this, "Couldn't Save Image due to invalid URI", Toast.LENGTH_SHORT).show();
                            finish();
                        } finally {
                            finish();
                        }

                    }
                }
            }
        });

    }

    private String convertToDmsString(double coordinate) {
        int degrees = (int) coordinate;
        coordinate = (coordinate - degrees) * 60;
        int minutes = (int) coordinate;
        coordinate = (coordinate - minutes) * 60;
        int seconds = (int) (coordinate * 1000);
        return degrees + "/1," + minutes + "/1," + seconds + "/1000";
    }

    public void PreviewNewImage(){
        new Thread(() -> {
            if (imgData.mapSnapPath == null ){
                imgData.mapSnapPath = overlayHandler.FetchImage(imgData.latitude, imgData.longitude);
            }
            runOnUiThread(()->{
                if (imgData.mapSnapPath == null) {
                    Toast.makeText(this, "Unable to Reach Google Maps API", Toast.LENGTH_SHORT).show();
                    finish();
                }
                Bitmap img = null;
                overlayHandler.AddOverlay(imgData, images.get(0));
                imgV.setImageBitmap(images.get(0).editedImage);
            });
        }).start();
    }

    private interface DrawOverALLEvents{
        void onSuccess();
    }

    public void DrawOverlaysOnAll(DrawOverALLEvents callback){
        DialogsHandler.LoadingDialogEvent loadingDialog = dialogsHandler.LoadingDialog();
        loadingDialog.show();
            new Thread(() -> {
                if (imgData.mapSnapPath == null ){
                    imgData.mapSnapPath =overlayHandler.FetchImage(imgData.latitude, imgData.longitude);

                }
                runOnUiThread(()->{
                    if (imgData.mapSnapPath == null) {
                        Toast.makeText(this, "Unable to Reach Google Maps API", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    for (Image i: images){
                        overlayHandler.AddOverlay(imgData, i);
                    }
                    callback.onSuccess();
                    loadingDialog.dismiss();
                });
            }).start();
    }
}