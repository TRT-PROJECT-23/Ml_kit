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

package com.google.mlkit.vision.demo;

import static java.lang.Math.max;

import android.content.ContentValues;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.util.Pair;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;

import android.widget.ImageView;
import android.widget.PopupMenu;

import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;

import com.google.android.gms.common.annotation.KeepName;
import com.google.mlkit.vision.demo.java.ChooserActivity;
import com.google.mlkit.vision.demo.java.barcodescanner.BarcodeScannerProcessor;

import com.google.mlkit.vision.demo.preference.SettingsActivity;


import java.io.IOException;



import android.content.SharedPreferences;

/** Activity demonstrating different image detector features with a still image from camera. */
@KeepName
public final class CodeScanner extends AppCompatActivity {

  private static final String TAG = "StillImageActivity";

  private static final String BARCODE_SCANNING = "Barcode Scanning";

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
  private String selectedMode = BARCODE_SCANNING;
  private String selectedSize = SIZE_SCREEN;

  boolean isLandScape;

  private Uri imageUri;
  private int imageMaxWidth;
  private int imageMaxHeight;
  private VisionImageProcessor imageProcessor;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.code_scanner);

    findViewById(R.id.select_image_button)
        .setOnClickListener(
            view -> {
              // Menu for selecting either: a) take new photo b) select from existing
              PopupMenu popup = new PopupMenu(CodeScanner.this, view);
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

      // Clear the overlay first
      graphicOverlay.clear();

      Bitmap resizedBitmap;
      if (selectedSize.equals(SIZE_ORIGINAL)) {
        resizedBitmap = imageBitmap;
      } else {
        // Get the dimensions of the image view
        Pair<Integer, Integer> targetedSize = getTargetedWidthHeight();

        // Determine how much to scale down the image
        float scaleFactor =
            max(
                (float) imageBitmap.getWidth() / (float) targetedSize.first,
                (float) imageBitmap.getHeight() / (float) targetedSize.second);

        resizedBitmap =
            Bitmap.createScaledBitmap(
                imageBitmap,
                (int) (imageBitmap.getWidth() / scaleFactor),
                (int) (imageBitmap.getHeight() / scaleFactor),
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

      preview.setVisibility(View.VISIBLE);

      ConstraintSet constraintSet = new ConstraintSet();
      constraintSet.clone((ConstraintLayout) findViewById(R.id.root));
      constraintSet.connect(R.id.preview, ConstraintSet.BOTTOM, R.id.control, ConstraintSet.TOP);
      constraintSet.applyTo((ConstraintLayout) findViewById(R.id.root));

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


        case BARCODE_SCANNING:
          imageProcessor = new BarcodeScannerProcessor(this, /* zoomCallback= */ null);
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

  public void onNextButtonClick(View view) {
    // Get the TextView that holds the result
    TextView resultTextView = findViewById(R.id.text1);

    // Get the text from the TextView
    String result = resultTextView.getText().toString().trim();

    // Saving the result to SharedPreferences
    SharedPreferences sharedPref = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE);
    SharedPreferences.Editor editor = sharedPref.edit();

    // Check if the result is empty or contains the default text
    if (result.isEmpty() || result.equals("Result will appear here")) {
      // Set the text to "123456789" if the result is empty or contains the default text
      result = "123456789";
    }

    // Save the retrieved text to SharedPreferences regardless of whether it's default
    editor.putString("barcodeResult", result);
    editor.apply();

    // Now 'result' holds the retrieved text from the TextView or "123456789" if empty/default

    // Navigate to the next activity
    Intent intent = new Intent(this, ChooserActivity.class);
    startActivity(intent);
  }






}
