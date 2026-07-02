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
import android.net.Uri;
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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicReference;

public class PreviewActivity extends AppCompatActivity {

    ImageDetails imgData = null;
    ImageView imgV;

    Bitmap editedImg;

    Boolean EditMode = false;

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
        String imageURI = getIntent().getStringExtra("URI");
        if(imageURI==null) {
            Toast.makeText(this, "Invalid Image", Toast.LENGTH_SHORT).show();
            finish();
        }

        showGeoTaggedImg(imageURI);
        imgV = findViewById(R.id.imageView);
        Button editButton = findViewById(R.id.editButton);
        Button moreOptions = findViewById(R.id.moreOptions);
        Slider textSizeSlide = findViewById(R.id.textSizeSlider);
        FloatingActionButton saveButton = (FloatingActionButton) findViewById(R.id.saveButton);
        imgV.setImageBitmap(RoatateImageIFExif(imageURI));
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
            OpenLocationDialog();
        });
        textSizeSlide.addOnSliderTouchListener(new Slider.OnSliderTouchListener() {
            @Override
            public void onStartTrackingTouch(@NonNull Slider slider) {
                Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                vibrator.cancel();
                vibrator.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK));
            }

            @Override
            public void onStopTrackingTouch(@NonNull Slider slider) {
                if (imgData!=null ) imgData.textSize = slider.getValue();
                showGeoTaggedImg(imageURI);
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

    @SuppressLint("ClickableViewAccessibility")
    private void OpenLocationDialog(){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        View v = getLayoutInflater().inflate(R.layout.more_edit_dialog, null);
        Button updateValues = v.findViewById(R.id.button2);
        EditText lat = v.findViewById(R.id.latitude);
        EditText lon = v.findViewById(R.id.longitude);
        EditText date = v.findViewById(R.id.date);
        EditText time = v.findViewById(R.id.time);
        EditText shortAddr = v.findViewById(R.id.short_addr);
        MaterialCheckBox checkBox = v.findViewById(R.id.showadvanced);
        if (imgData != null){
            lat.getEditableText().append(String.valueOf(imgData.latitude));
            lon.getEditableText().append(String.valueOf(imgData.longitude));
            date.getEditableText().append(imgData.time.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            shortAddr.getEditableText().append(imgData.shortAddr);
            time.getEditableText().append(imgData.time.format(DateTimeFormatter.ofPattern("HH:mm")));
        }
        builder.setView(v);
        Dialog dialog = builder.create();
        ((Button) v.findViewById(R.id.closebutton)).setOnClickListener(v1->dialog.dismiss());
        updateValues.setOnClickListener(v1->{
            if (shortAddr.getText().toString().isEmpty()){
                autoFillDetails(Float.parseFloat(lat.getText().toString()),Float.parseFloat(lon.getText().toString()));
            }else{
                autoFillDetails(Float.parseFloat(lat.getText().toString()),Float.parseFloat(lon.getText().toString()), shortAddr.getText().toString());
            }
            if (imgData!=null && !date.getText().toString().isEmpty() && !time.getText().toString().isEmpty()) {
                imgData.time = LocalDateTime.parse(date.getText().toString() + " @ " + time.getText().toString() , DateTimeFormatter.ofPattern("dd/MM/yyyy @ HH:mm"));
            }
            dialog.dismiss();
        });
        checkBox.addOnCheckedStateChangedListener((materialCheckBox, i) -> {
            if (i == MaterialCheckBox.STATE_CHECKED){
                ((TextInputLayout) v.findViewById(R.id.short_addr_layout)).setVisibility(VISIBLE);
            }else{
                ((TextInputLayout) v.findViewById(R.id.short_addr_layout)).setVisibility(GONE);

            }
        });
        time.setOnTouchListener((v2, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP){
                MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                        .setTitleText("Choose the Time")
                        .setTimeFormat(TimeFormat.CLOCK_12H)
                        .setHour(LocalDateTime.now().getHour())
                        .setMinute(LocalDateTime.now().getMinute())
                        .build();
                timePicker.addOnPositiveButtonClickListener(dialog1 -> {
                    time.setText(String.format("%2s:%2s", timePicker.getHour(), timePicker.getMinute()).replace(" ", "0"));
                });
                timePicker.show(getSupportFragmentManager(), "Time_Picker");
            }
            return true;
        });
        date.setOnTouchListener((v2,event)->{
            if (event.getAction() == MotionEvent.ACTION_UP){
                MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Choose Date")
                        .build();
                datePicker.show(getSupportFragmentManager(), "DATE_PICKER");
                datePicker.addOnPositiveButtonClickListener(dialog1 -> {
                    date.setText(Instant.ofEpochMilli(datePicker.getSelection()).atZone(ZoneId.of("UTC")).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                });
            }
            return true;
        });
        dialog.show();
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
    }


    private void checkSharedPrefs(){
        SharedPreferences sharedPreferences = getSharedPreferences("overlay", MODE_PRIVATE);
        String shortAddr = sharedPreferences.getString("shortAddr", null);
        String longAddr = sharedPreferences.getString("longAddr", null);
        Float lat = sharedPreferences.getFloat("lat", 0.0f);
        Float lon = sharedPreferences.getFloat("lon", 0.0f);
        Float textSize = sharedPreferences.getFloat("textSize", 0.0f);
        if (shortAddr != null && longAddr!=null && lat!= 0.0f && lon != 0.0f){
            imgData = new ImageDetails();
            imgData.shortAddr = shortAddr;
            imgData.displayName = longAddr;
            imgData.latitude = lat;
            imgData.longitude = lon;
            if (textSize != 0.0f){
                imgData.textSize = textSize;
            }
        }
    }

    public void saveSharedPrefs(ImageDetails imgData){
        SharedPreferences sharedPreferences = getSharedPreferences("overlay", MODE_PRIVATE);
        sharedPreferences.edit().putString("shortAddr", imgData.shortAddr).apply();
        sharedPreferences.edit().putString("longAddr", imgData.displayName).apply();
        sharedPreferences.edit().putFloat("lat", (float) imgData.latitude).apply();
        sharedPreferences.edit().putFloat("lon", (float) imgData.longitude).apply();
    }

    public void autoFillDetails(float lat, float lon){
        OverlayHandler overlayHandler = new OverlayHandler();
        new Thread(() -> {
            ImageDetails x = overlayHandler.fetchDetails(lat, lon);;
            x.mapSnapPath = overlayHandler.FetchImage(lat, lon);
            runOnUiThread(()->{
                if (x.mapSnapPath == null) {
                    Toast.makeText(this, "Unable to Reach Google Maps API", Toast.LENGTH_SHORT).show();
                    finish();
                }
                imgData= x;
                saveSharedPrefs(x);
            });
        }).start();
    }
    public void autoFillDetails(float lat, float lon, String short_addr){
        OverlayHandler overlayHandler = new OverlayHandler();
        new Thread(() -> {
            ImageDetails x = overlayHandler.fetchDetails(lat, lon);
            x.mapSnapPath = overlayHandler.FetchImage(lat, lon);
            runOnUiThread(()->{
                if (x.mapSnapPath == null) {
                    Toast.makeText(this, "Unable to Reach Google Maps API", Toast.LENGTH_SHORT).show();
                    finish();
                }
                x.shortAddr = short_addr;
                imgData= x;
                saveSharedPrefs(x);
            });
        }).start();
    }

    public void saveImage(){
        SharedPreferences sharedPreferences = getSharedPreferences("overlay", MODE_PRIVATE);
        sharedPreferences.edit().putFloat("textSize", imgData.textSize).apply();

        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, "GeoTagged_"+ LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".jpg");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, "Pictures/GeoTagged_Capture");
//        contentValues.put(MediaStore.Images.Media.IS_PENDING, true);
        Uri uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
        if (uri != null){
            try {
                OutputStream os = getContentResolver().openOutputStream(uri);
                editedImg.compress(Bitmap.CompressFormat.JPEG, 100, os);
                os.flush();
                os.close();
            } catch (IOException e) {
                Toast.makeText(this, "Couldn't Save Image due to invalid URI", Toast.LENGTH_SHORT).show();
                finish();
            } finally {
//                contentValues.put(MediaStore.Images.Media.IS_PENDING, false);
                Intent intent = new Intent(PreviewActivity.this, MainActivity.class);
                startActivity(intent);
            }

        }
    }

    public void showGeoTaggedImg(String uri){
            OverlayHandler overlayHandler = new OverlayHandler();

            if (imgData == null ){
                checkSharedPrefs();
                if (imgData == null) {return;}
            }
            AtomicReference<ImageDetails> details = new AtomicReference<>(new ImageDetails());
            new Thread(() -> {
                ImageDetails x = imgData;
                x.mapSnapPath = overlayHandler.FetchImage(x.latitude, x.longitude);
                runOnUiThread(()->{
                    if (x.mapSnapPath == null) {
                        Toast.makeText(this, "Unable to Reach Google Maps API", Toast.LENGTH_SHORT).show();
                        finish();
                    }
                    details.set(x);
                    Bitmap img = null;
                    img = overlayHandler.AddOverlay(RoatateImageIFExif(uri), details.get());
                    editedImg = img;
                    imgV.setImageBitmap(img);
                });
            }).start();
    }
}