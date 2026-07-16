package org.feystray.geotagger;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.fragment.app.FragmentManager;

import com.google.android.material.checkbox.MaterialCheckBox;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.timepicker.MaterialTimePicker;
import com.google.android.material.timepicker.TimeFormat;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DialogsHandler {
    Context context;
    DialogsHandler(Context context){
        this.context = context;
    }

    public interface LocationandTimeDialogEvent{
        void onSave(float lat, float lon, String short_Addr , LocalDateTime datenTime);
        void onSave(float lat, float lon, String short_Addr , LocalDateTime fromTime, LocalDateTime toTime);
        void onCancel();
    }

    @SuppressLint("ClickableViewAccessibility")
    public void showLocationAndTimeDialog(ImageDetails initalDetails, FragmentManager fm, boolean isMultiMode, LocalDateTime defaultTime ,LocationandTimeDialogEvent callback){
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(context, R.style.FullscreenDialog);
        View v = LayoutInflater.from(context).inflate(R.layout.more_edit_dialog, null);
        Button updateValues = v.findViewById(R.id.button2);
        EditText lat = v.findViewById(R.id.latitude);
        EditText lon = v.findViewById(R.id.longitude);
        EditText date = v.findViewById(R.id.date);
        EditText time = v.findViewById(R.id.time);
        EditText shortAddr = v.findViewById(R.id.short_addr);
        MaterialCheckBox checkBox = v.findViewById(R.id.showadvanced);
        EditText fromTime = v.findViewById(R.id.fromTime);
        EditText toTime = v.findViewById(R.id.endTime);
        //LoadInital Details
        if (initalDetails != null){
            lat.getEditableText().append(String.valueOf(initalDetails.latitude));
            lon.getEditableText().append(String.valueOf(initalDetails.longitude));
            date.getEditableText().append(defaultTime != null ? defaultTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
            fromTime.getEditableText().append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            toTime.getEditableText().append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm")));
            shortAddr.getEditableText().append(initalDetails.shortAddr);
        }
        ((TextInputLayout) v.findViewById(R.id.timeLayout)).setVisibility(isMultiMode ? GONE : VISIBLE);
        ((TextInputLayout) v.findViewById(R.id.fromTimeLayout)).setVisibility(isMultiMode ? VISIBLE: GONE);
        ((TextInputLayout) v.findViewById(R.id.endTimeLayout)).setVisibility(isMultiMode ? VISIBLE: GONE);
        //
        builder.setView(v);
        Dialog dialog = builder.create();
        ((Button) v.findViewById(R.id.closebutton)).setOnClickListener(v1-> {
            callback.onCancel();
            dialog.dismiss();
        });
        updateValues.setOnClickListener(v1->{
            if (isMultiMode){
                callback.onSave(
                        Float.parseFloat(lat.getText().toString()),
                        Float.parseFloat(lon.getText().toString()),
                        !shortAddr.getText().toString().isEmpty() ? shortAddr.getText().toString() : null,
                        LocalDateTime.parse(date.getText().toString() + " @ " + fromTime.getText().toString() , DateTimeFormatter.ofPattern("dd/MM/yyyy @ HH:mm")),
                        LocalDateTime.parse(date.getText().toString() + " @ " + toTime.getText().toString() , DateTimeFormatter.ofPattern("dd/MM/yyyy @ HH:mm"))
                );
            }else{
                callback.onSave(
                        Float.parseFloat(lat.getText().toString()),
                        Float.parseFloat(lon.getText().toString()),
                        !shortAddr.getText().toString().isEmpty() ? shortAddr.getText().toString() : null,
                        !date.getText().toString().isEmpty() && !time.getText().toString().isEmpty()
                                ? LocalDateTime.parse(date.getText().toString() + " @ " + time.getText().toString() , DateTimeFormatter.ofPattern("dd/MM/yyyy @ HH:mm"))
                                : null
                );
            }
//            if (shortAddr.getText().toString().isEmpty()){
//                autoFillDetails(Float.parseFloat(lat.getText().toString()),Float.parseFloat(lon.getText().toString()), null);
//            }else{
//                autoFillDetails(Float.parseFloat(lat.getText().toString()),Float.parseFloat(lon.getText().toString()), shortAddr.getText().toString());
//            }
//            if (imgData!=null && !date.getText().toString().isEmpty() && !time.getText().toString().isEmpty()) {
//                imgData.time = LocalDateTime.parse(date.getText().toString() + " @ " + time.getText().toString() , DateTimeFormatter.ofPattern("dd/MM/yyyy @ HH:mm"));
//            }
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
                timePicker.show(fm, "Time_Picker");
            }
            return true;
        });
        fromTime.setOnTouchListener((v2, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP){
                MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                        .setTitleText("Choose the Start Time")
                        .setTimeFormat(TimeFormat.CLOCK_12H)
                        .setHour(LocalDateTime.now().getHour())
                        .setMinute(LocalDateTime.now().getMinute())
                        .build();
                timePicker.addOnPositiveButtonClickListener(dialog1 -> {
                    fromTime.setText(String.format("%2s:%2s", timePicker.getHour(), timePicker.getMinute()).replace(" ", "0"));
                });
                timePicker.show(fm, "Time_Picker");
            }
            return true;
        });
        toTime.setOnTouchListener((v2, event) -> {
            if (event.getAction() == MotionEvent.ACTION_UP){
                MaterialTimePicker timePicker = new MaterialTimePicker.Builder()
                        .setTitleText("Choose the End Time")
                        .setTimeFormat(TimeFormat.CLOCK_12H)
                        .setHour(LocalDateTime.now().getHour())
                        .setMinute(LocalDateTime.now().getMinute())
                        .build();
                timePicker.addOnPositiveButtonClickListener(dialog1 -> {
                    toTime.setText(String.format("%2s:%2s", timePicker.getHour(), timePicker.getMinute()).replace(" ", "0"));
                });
                timePicker.show(fm, "Time_Picker");
            }
            return true;
        });
        date.setOnTouchListener((v2,event)->{
            if (event.getAction() == MotionEvent.ACTION_UP){
                MaterialDatePicker<Long> datePicker = MaterialDatePicker.Builder.datePicker()
                        .setTitleText("Choose Date")
                        .build();
                datePicker.show(fm, "DATE_PICKER");
                datePicker.addOnPositiveButtonClickListener(dialog1 -> {
                    date.setText(Instant.ofEpochMilli(datePicker.getSelection()).atZone(ZoneId.of("UTC")).toLocalDateTime().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
                });
            }
            return true;
        });
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT);
        }
        dialog.show();
    }

    public  interface LoadingDialogEvent{
        void show();
        void dismiss();
    }

    public LoadingDialogEvent LoadingDialog(){
        Dialog loadingDialog  = new MaterialAlertDialogBuilder(context)
                .setView(
                        LayoutInflater.from(context).inflate(R.layout.spinner, null)
                )
                .create();
        return new LoadingDialogEvent() {
            @Override
            public void show() {
                loadingDialog.show();
            }

            @Override
            public void dismiss() {
                loadingDialog.dismiss();
            }
        };
    }


}
