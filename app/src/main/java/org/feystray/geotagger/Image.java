package org.feystray.geotagger;

import android.graphics.Bitmap;

import java.time.LocalDateTime;

public class Image {
    public Bitmap originalImage;
    public Bitmap editedImage;
    public LocalDateTime time;

    public Image(){
    }

    public Image(Bitmap originalImage, LocalDateTime time){
        this.originalImage = originalImage;
        this.time = time;
    }
}
