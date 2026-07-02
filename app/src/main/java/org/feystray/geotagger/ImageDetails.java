package org.feystray.geotagger;

import androidx.annotation.NonNull;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class ImageDetails {
    public String displayName;

    public double latitude;

    public double longitude;

    public LocalDateTime time = LocalDateTime.now();

    public byte[] mapSnapPath;

    public float textSize = 1.2f;

    public String shortAddr;

    public String getTime(){
        return time.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy @ hh:mm a"));
    }

//    public String getShortAddress(){
//        return String.format("%s, %s, %s", area, state, country);
//    }

    @NonNull
    @Override
    public String toString() {
        return String.format("shortAddress=%s;longAddress=%s;time=%s;textSize=%f;long=%f;lat=%f", shortAddr, displayName, getTime(), textSize, longitude,latitude);
    }
}
