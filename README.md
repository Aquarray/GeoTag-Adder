# GeoTagger

GeoTagger is an Android application that enables users to add rich geotag overlays to their photos. With a simple, user-friendly interface, GeoTagger automatically fetches your current location details—including latitude, longitude, and an embedded map snapshot—and overlays them onto pictures captured directly from the app or chosen from your gallery.

## Features

- **In-App Camera Capture:** Take pictures directly within the app using the Android CameraX API.
- **Gallery Import:** Select images directly from your device's gallery to add geotags.
- **Automatic Geotagging:** Automatically fetches location coordinates and a Google Maps snapshot.
- **Customizable Overlays:** Edit the location details, adjust the date/time, and customize the text size of the overlay text to fit your needs perfectly.
- **Easy Saving:** Saves the finalized, geotagged photos directly into your device's Pictures folder (`Pictures/GeoTagged_Capture`).

## Prerequisites & Technologies

- **Android SDK:** Targeting modern Android devices.
- **CameraX:** For a robust, lifecycle-aware camera implementation.
- **Google Maps API:** Used to fetch the location snapshot in the overlay.
- **Java:** Written entirely in Java.

## Getting Started

1. Clone the repository to your local machine:
   ```bash
   git clone <your-repository-url>
   ```
2. Open the project using **Android Studio**.
3. Ensure you have your Google Maps API setup correctly if needed (check your `local.properties` or resource files where the Maps API key might be required for the `OverlayHandler` to function).
4. Build and run the app on a physical Android device or an emulator with a configured camera.

## Usage

1. Open the **GeoTagger** app. Accept the required Camera and Storage permissions.
2. Tap the **Capture** button to take a photo, or choose the **Gallery** icon to pick an existing image.
3. Once in the preview mode, you will see a preview of the geotag overlay on your image.
4. Tap **Edit** to modify location parameters manually or adjust the text scale.
5. Tap **Save** to generate the final image and store it on your device.

## Contributing

Contributions are welcome! If you find a bug or have an idea for a new feature, please open an issue or submit a pull request.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.
