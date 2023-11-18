/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.mlkit.vision.demo.java;

import static java.lang.Math.max;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import androidx.appcompat.app.AppCompatActivity;

import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;
import android.util.Pair;
import android.view.MenuInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Spinner;
import android.widget.Toast;
import com.google.android.gms.common.annotation.KeepName;
import com.google.mlkit.vision.demo.BitmapUtils;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.R;
import com.google.mlkit.vision.demo.VisionImageProcessor;
import com.google.mlkit.vision.demo.java.barcodescanner.BarcodeScannerProcessor;

import com.google.mlkit.vision.demo.java.textdetector.TextRecognitionProcessor;

import com.google.mlkit.vision.demo.preference.SettingsActivity;


import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Activity demonstrating different image detector features with a still image from camera. */
@KeepName
public final class StillImageActivity extends AppCompatActivity {

  private static final String TAG = "StillImageActivity";

  private static final String TEXT_RECOGNITION_LATIN = "Text Recognition Latin";

  private static final String SIZE_SCREEN = "w:screen"; // Match screen width
  private static final String SIZE_1024_768 = "w:1024"; // ~1024*768 in a normal ratio
  private static final String SIZE_640_480 = "w:640"; // ~640*480 in a normal ratio
  private static final String SIZE_ORIGINAL = "w:original"; // Original image size

  private static final String KEY_IMAGE_URI = "com.google.mlkit.vision.demo.KEY_IMAGE_URI";
  private static final String KEY_SELECTED_SIZE = "com.google.mlkit.vision.demo.KEY_SELECTED_SIZE";

  private static final int REQUEST_IMAGE_CAPTURE = 1001;
  private static final int REQUEST_CHOOSE_IMAGE = 1002;

  private ImageView preview;
  private GraphicOverlay graphicOverlay;
  private String selectedMode = TEXT_RECOGNITION_LATIN;
  private String selectedSize = SIZE_SCREEN;

  boolean isLandScape;

  private Uri imageUri;
  private int imageMaxWidth;
  private int imageMaxHeight;
  private VisionImageProcessor imageProcessor;

  private float startX, startY, endX, endY;
  private boolean isDrawing = false;
  private Bitmap mutableBitmap; // Declare mutableBitmap as a class-level variable

  private RectF drawnRect;


  @SuppressLint("ClickableViewAccessibility")
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_still_image);

    findViewById(R.id.select_image_button)
            .setOnClickListener(
                    view -> {
                      // Menu for selecting either: a) take new photo b) select from existing
                      PopupMenu popup = new PopupMenu(StillImageActivity.this, view);
                      popup.setOnMenuItemClickListener(
                              menuItem -> {
                                int itemId = menuItem.getItemId();
                                if (itemId == R.id.select_images_from_local) {
                                  startChooseImageIntentForResult();
                                  return true;
                                } else if (itemId == R.id.take_photo_using_camera) {
                                  startCameraIntentForResult();
                                  return true;
                                }
                                return false;
                              });
                      MenuInflater inflater = popup.getMenuInflater();
                      inflater.inflate(R.menu.camera_button_menu, popup.getMenu());
                      popup.show();
                    });
    preview = findViewById(R.id.preview);

    preview.setOnTouchListener((v, event) -> {
      float eventX = event.getX();
      float eventY = event.getY();

      switch (event.getAction()) {
        case MotionEvent.ACTION_DOWN:
          // Clear the graphic overlay when a new selection begins
          graphicOverlay.clear();

          // Initialize a new rectangle when the touch event starts
          drawnRect = new RectF(eventX, eventY, eventX, eventY);
          isDrawing = true;
          break;

        case MotionEvent.ACTION_UP:
          if (isDrawing) {
            // Finalize the rectangle and process the image inside the defined rectangle area
            drawnRect.right = eventX;
            drawnRect.bottom = eventY;
            isDrawing = false;
            drawRectangle(); // Draw the finalized rectangle
            processImageInRectangle(drawnRect.left, drawnRect.top, drawnRect.right, drawnRect.bottom);
          }
          break;
      }
      return true;
    });




    graphicOverlay = findViewById(R.id.graphic_overlay);


    isLandScape =
            (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE);

    if (savedInstanceState != null) {
      imageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI);
      selectedSize = savedInstanceState.getString(KEY_SELECTED_SIZE);
    }

    View rootView = findViewById(R.id.root);
    rootView
            .getViewTreeObserver()
            .addOnGlobalLayoutListener(
                    new OnGlobalLayoutListener() {
                      @Override
                      public void onGlobalLayout() {
                        rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        imageMaxWidth = rootView.getWidth();
                        imageMaxHeight = rootView.getHeight() - findViewById(R.id.control).getHeight();
                        if (SIZE_SCREEN.equals(selectedSize)) {
                          tryReloadAndDetectInImage();
                        }
                      }
                    });

    ImageView settingsButton = findViewById(R.id.settings_button);
    settingsButton.setOnClickListener(
            v -> {
              Intent intent = new Intent(getApplicationContext(), SettingsActivity.class);
              intent.putExtra(
                      SettingsActivity.EXTRA_LAUNCH_SOURCE, SettingsActivity.LaunchSource.STILL_IMAGE);
              startActivity(intent);
            });
  }

  @Override
  public void onResume() {
    super.onResume();
    Log.d(TAG, "onResume");
    createImageProcessor();
    tryReloadAndDetectInImage();
  }

  @Override
  public void onPause() {
    super.onPause();
    if (imageProcessor != null) {
      imageProcessor.stop();
    }
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    if (imageProcessor != null) {
      imageProcessor.stop();
    }
  }


  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelable(KEY_IMAGE_URI, imageUri);
    outState.putString(KEY_SELECTED_SIZE, selectedSize);
  }

  private void startCameraIntentForResult() {
    // Clean up last time's image
    imageUri = null;
    preview.setImageBitmap(null);

    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      ContentValues values = new ContentValues();
      values.put(MediaStore.Images.Media.TITLE, "New Picture");
      values.put(MediaStore.Images.Media.DESCRIPTION, "From Camera");
      imageUri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
      takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }
  }

  private void startChooseImageIntentForResult() {
    Intent intent = new Intent();
    intent.setType("image/*");
    intent.setAction(Intent.ACTION_GET_CONTENT);
    startActivityForResult(Intent.createChooser(intent, "Select Picture"), REQUEST_CHOOSE_IMAGE);
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      tryReloadAndDetectInImage();
    } else if (requestCode == REQUEST_CHOOSE_IMAGE && resultCode == RESULT_OK) {
      // In this case, imageUri is returned by the chooser, save it.
      imageUri = data.getData();
      tryReloadAndDetectInImage();
    } else {
      super.onActivityResult(requestCode, resultCode, data);
    }
  }

  private void tryReloadAndDetectInImage() {
    Log.d(TAG, "Try reload and detect image");
    try {
      if (imageUri == null) {
        return;
      }

      if (SIZE_SCREEN.equals(selectedSize) && imageMaxWidth == 0) {
        // UI layout has not finished yet, will reload once it's ready.
        return;
      }

      Bitmap imageBitmap = BitmapUtils.getBitmapFromContentUri(getContentResolver(), imageUri);
      if (imageBitmap == null) {
        return;
      }

      // Preprocess the image: grayscale conversion and contrast adjustment
      float contrastLevel = 150.0f; // Adjust this value to change the contrast
      //Bitmap processedBitmap = convertToGrayscaleWithContrast(imageBitmap, contrastLevel);

      Bitmap processedBitmap = convertToGrayscale(imageBitmap);


      // Apply blur effect to the processed image
      Bitmap blurredBitmap = applyBlur(getApplicationContext(), processedBitmap, 17.0f); // Adjust blur radius

      // Clear the overlay first
      graphicOverlay.clear();

      Bitmap resizedBitmap;
      if (selectedSize.equals(SIZE_ORIGINAL)) {
        resizedBitmap = blurredBitmap; // Use the blurred bitmap for original size
      } else {
        // Get the dimensions of the image view
        Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

        // Determine how much to scale down the image
        float scaleFactor =
                max(
                        (float) blurredBitmap.getWidth() / (float) targetedSize.first,
                        (float) blurredBitmap.getHeight() / (float) targetedSize.second);

        resizedBitmap =
                Bitmap.createScaledBitmap(
                        blurredBitmap,
                        (int) (blurredBitmap.getWidth() / scaleFactor),
                        (int) (blurredBitmap.getHeight() / scaleFactor),
                        true);
      }

      preview.setImageBitmap(resizedBitmap);

      if (imageProcessor != null) {
        graphicOverlay.setImageSourceInfo(
                resizedBitmap.getWidth(), resizedBitmap.getHeight(), /* isFlipped= */ false);
        imageProcessor.processBitmap(resizedBitmap, graphicOverlay);
      } else {
        Log.e(TAG, "Null imageProcessor, please check adb logs for imageProcessor creation error");
      }
    } catch (IOException e) {
      Log.e(TAG, "Error retrieving saved image");
      imageUri = null;
    }
  }
  private Pair<Integer, Integer> getTargetedWidthHeight() {
    int targetWidth;
    int targetHeight;

    switch (selectedSize) {
      case SIZE_SCREEN:
        targetWidth = imageMaxWidth;
        targetHeight = imageMaxHeight;
        break;
      case SIZE_640_480:
        targetWidth = isLandScape ? 640 : 480;
        targetHeight = isLandScape ? 480 : 640;
        break;
      case SIZE_1024_768:
        targetWidth = isLandScape ? 1024 : 768;
        targetHeight = isLandScape ? 768 : 1024;
        break;
      default:
        throw new IllegalStateException("Unknown size");
    }

    return new Pair<>(targetWidth, targetHeight);
  }

  private void createImageProcessor() {
    if (imageProcessor != null) {
      imageProcessor.stop();
    }
    try {
      switch (selectedMode) {


        case TEXT_RECOGNITION_LATIN:
          if (imageProcessor != null) {
            imageProcessor.stop();
          }
          imageProcessor =
                  new TextRecognitionProcessor(this, new TextRecognizerOptions.Builder().build());
          break;


        default:
          Log.e(TAG, "Unknown selectedMode: " + selectedMode);
      }
    } catch (Exception e) {
      Log.e(TAG, "Can not create image processor: " + selectedMode, e);
      Toast.makeText(
                      getApplicationContext(),
                      "Can not create image processor: " + e.getMessage(),
                      Toast.LENGTH_LONG)
              .show();
    }
  }

  private void drawRectangle() {
    // Get the original bitmap from the preview ImageView
    Bitmap originalBitmap = ((BitmapDrawable) preview.getDrawable()).getBitmap().copy(Bitmap.Config.ARGB_8888, true);

    // Create a Canvas to draw on the mutable bitmap
    Canvas canvas = new Canvas(originalBitmap);

    // Draw the bitmap on the canvas
    canvas.drawBitmap(originalBitmap, 0, 0, null);

    // Define the paint properties for drawing the rectangle
    Paint paint = new Paint();
    paint.setColor(Color.RED);
    paint.setStyle(Paint.Style.STROKE);
    paint.setStrokeWidth(5);

    // Draw the rectangle on the canvas
    canvas.drawRect(drawnRect, paint);

    // Set the modified bitmap to the preview ImageView
    preview.setImageBitmap(originalBitmap);
  }
  private void processImageInRectangle(float startX, float startY, float endX, float endY) {
    // Get the original bitmap from the preview ImageView
    Bitmap originalBitmap = ((BitmapDrawable) preview.getDrawable()).getBitmap();

    // Calculate the rectangle coordinates in terms of the original image
    int imageWidth = originalBitmap.getWidth();
    int imageHeight = originalBitmap.getHeight();

    float scaleFactorX = (float) imageWidth / preview.getWidth();
    float scaleFactorY = (float) imageHeight / preview.getHeight();

    int croppedStartX = (int) (startX * scaleFactorX);
    int croppedStartY = (int) (startY * scaleFactorY);
    int croppedEndX = (int) (endX * scaleFactorX);
    int croppedEndY = (int) (endY * scaleFactorY);

    // Ensure that the rectangle coordinates are within bounds
    croppedStartX = Math.max(0, croppedStartX);
    croppedStartY = Math.max(0, croppedStartY);
    croppedEndX = Math.min(imageWidth, croppedEndX);
    croppedEndY = Math.min(imageHeight, croppedEndY);

    // Crop the image within the defined rectangle
    Bitmap croppedBitmap = Bitmap.createBitmap(originalBitmap, croppedStartX, croppedStartY,
            croppedEndX - croppedStartX, croppedEndY - croppedStartY);

    // Process the cropped image for text recognition using the image processor
    if (imageProcessor != null) {
      graphicOverlay.clear(); // Clear the overlay first
      graphicOverlay.setImageSourceInfo(croppedBitmap.getWidth(), croppedBitmap.getHeight(), false);
      imageProcessor.processBitmap(croppedBitmap, graphicOverlay);
    } else {
      Log.e(TAG, "Null imageProcessor, please check adb logs for imageProcessor creation error");
    }
  }

  public static Bitmap applyBlur(Context context, Bitmap originalBitmap, float blurRadius) {
    Bitmap blurredBitmap = Bitmap.createBitmap(originalBitmap.getWidth(), originalBitmap.getHeight(), Bitmap.Config.ARGB_8888);

    RenderScript rs = RenderScript.create(context);
    Allocation input = Allocation.createFromBitmap(rs, originalBitmap);
    Allocation output = Allocation.createFromBitmap(rs, blurredBitmap);

    ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
    blur.setInput(input);
    blur.setRadius(blurRadius);
    blur.forEach(output);

    output.copyTo(blurredBitmap);

    rs.destroy();

    return blurredBitmap;
  }

  public static Bitmap convertToGrayscaleWithContrast(Bitmap originalBitmap, float contrast) {
    int width = originalBitmap.getWidth();
    int height = originalBitmap.getHeight();

    Bitmap grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

    int[] pixels = new int[width * height];
    originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

    float contrastFactor = (259f * (contrast + 255f)) / (255f * (259f - contrast));

    for (int i = 0; i < width * height; i++) {
      int pixel = pixels[i];
      int red = Color.red(pixel);
      int green = Color.green(pixel);
      int blue = Color.blue(pixel);

      int luminance = (int) (0.299 * red + 0.587 * green + 0.114 * blue);

      int newRed = truncateColor(contrastFactor * (red - 128) + 128 + luminance);
      int newGreen = truncateColor(contrastFactor * (green - 128) + 128 + luminance);
      int newBlue = truncateColor(contrastFactor * (blue - 128) + 128 + luminance);

      pixels[i] = Color.rgb(newRed, newGreen, newBlue);
    }

    grayscaleBitmap.setPixels(pixels, 0, width, 0, 0, width, height);

    return grayscaleBitmap;
  }

  private static int truncateColor(float color) {
    if (color < 0) {
      return 0;
    } else if (color > 255) {
      return 255;
    }
    return (int) color;
  }

  public static Bitmap convertToGrayscale(Bitmap originalBitmap) {
    int width = originalBitmap.getWidth();
    int height = originalBitmap.getHeight();

    Bitmap grayscaleBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);

    int[] pixels = new int[width * height];
    originalBitmap.getPixels(pixels, 0, width, 0, 0, width, height);

    for (int i = 0; i < width * height; i++) {
      int pixel = pixels[i];
      int red = Color.red(pixel);
      int green = Color.green(pixel);
      int blue = Color.blue(pixel);

      int luminance = (int) (0.299 * red + 0.587 * green + 0.114 * blue);
      pixels[i] = Color.rgb(luminance, luminance, luminance);
    }

    grayscaleBitmap.setPixels(pixels, 0, width, 0, 0, width, height);

    return grayscaleBitmap;
  }


}

