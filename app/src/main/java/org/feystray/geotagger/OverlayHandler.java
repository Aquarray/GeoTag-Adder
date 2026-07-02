package org.feystray.geotagger;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class OverlayHandler {
    public ImageDetails fetchDetails(double lat, double lon) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(String.format("https://nominatim.openstreetmap.org/reverse?format=json&lat=%f&lon=%f", lat, lon)).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept", "application/json");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36");
            BufferedReader stream = new BufferedReader(new InputStreamReader(conn.getInputStream()));
//            StringBuilder stbuiler = new StringBuilder();
//            for(int t = stream.read(); t != -1; t = stream.read()){
//                stbuiler.append((char) t);
//            }
//            String resp = stbuiler.toString();
            String resp = stream.readLine();
            final String regex = "display_name\":\"(.*?)\",.*(?:city|town|village|county)\":\"(\\w+)\",.*(?:state)\":\"(\\w+)\",.*(?:country)\":\"(\\w+)\"";
            final Pattern pattern = Pattern.compile(regex, Pattern.MULTILINE);
            final Matcher matcher = pattern.matcher(resp);
            ImageDetails de= new ImageDetails();
            while (matcher.find()) {
                de.displayName = matcher.group(1);
                de.shortAddr = String.format("%s, %s, %s", matcher.group(2), matcher.group(3), matcher.group(4));
            }
            de.latitude = lat;
            de.longitude = lon;
            return de;
        } catch (Exception e) {
           return null;
        }
    }

    public byte[] FetchImage(double lat , double lon){
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(String.format("https://maps.googleapis.com/maps/api/staticmap?center=%f,%f&zoom=15&size=300x300&key=%s", lat, lon, BuildConfig.GAPIKEY)).openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36");
            InputStream stream = conn.getInputStream();
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buff = new byte[1024];
            int bytesRead;
            while((bytesRead = stream.read(buff))!=-1){
                outputStream.write(buff, 0, bytesRead);
            }
            return outputStream.toByteArray();
//            File f = File.createTempFile("map", ".png");
//            HttpResponse resp = cli.send(req, HttpResponse.BodyHandlers.ofFile(f.toPath()));
//            return f.getPath();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return null;
    }


    public Bitmap AddOverlay(Bitmap imgPath, ImageDetails details){
        Bitmap mutablebitmap = imgPath.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutablebitmap);
        drawGeoTag(canvas, canvas.getWidth(), canvas.getHeight(), details);
        return mutablebitmap;
    }

    public void drawGeoTag(Canvas canvas, int maxWidth, int maxHeight, ImageDetails detail){
        Paint bgPaint = new Paint();
        bgPaint.setColor(Color.argb(191, 0,0,0));
        canvas.drawRect(0, (int)(maxHeight*0.8), maxWidth, maxHeight, bgPaint);

        Bitmap bitmap = BitmapFactory.decodeByteArray(detail.mapSnapPath,0 , detail.mapSnapPath.length);
        canvas.drawBitmap(bitmap,null ,new Rect((int)(maxWidth*0.05), (int)(maxHeight*0.82), (int)(maxWidth*0.29), (int)(maxHeight*0.98)), null);
        TextPaint myTextPaint = new TextPaint();
        myTextPaint.setAntiAlias(true);
        myTextPaint.setTextSize(detail.textSize *14);
        myTextPaint.setColor(Color.WHITE);

        StringBuilder metadata = new StringBuilder();
        metadata.append(detail.shortAddr);
        metadata.append("\n");
        metadata.append(detail.displayName);
        metadata.append("\n");
        metadata.append(String.format("Lat: %.6f Long: %.6f", detail.latitude, detail.longitude));
        metadata.append("\n");
        metadata.append(detail.getTime());
        metadata.append("\n");
        StaticLayout layout = new StaticLayout(metadata.toString(), myTextPaint,(int)(maxWidth *0.68), Layout.Alignment.ALIGN_NORMAL, 1.3f , 0 , false);
        canvas.translate((int)(maxWidth*0.3), (int)(maxHeight*0.82));
        layout.draw(canvas);
    }
}
