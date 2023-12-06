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

package com.google.mlkit.vision.demo.java.textdetector;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.util.Log;
import android.widget.TextView;

import com.google.mlkit.vision.demo.R;

import androidx.annotation.NonNull;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.demo.GraphicOverlay;
import com.google.mlkit.vision.demo.java.VisionProcessorBase;
import com.google.mlkit.vision.demo.preference.PreferenceUtils;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.Text.Element;
import com.google.mlkit.vision.text.Text.Line;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.TextRecognizerOptionsInterface;

import java.util.HashMap;
import java.util.List;



/** Processor for the text detector demo. */
public class TextRecognitionProcessor extends VisionProcessorBase<Text> {

  private static final String TAG = "TextRecProcessor";

  private final TextRecognizer textRecognizer;
  private final Boolean shouldGroupRecognizedTextInBlocks;
  private final Boolean showLanguageTag;
  private final boolean showConfidence;
  private TextView result0;
  private Context context;

  public TextRecognitionProcessor(
      Context context, TextRecognizerOptionsInterface textRecognizerOptions) {
    super(context);
    this.context = context;
    shouldGroupRecognizedTextInBlocks = PreferenceUtils.shouldGroupRecognizedTextInBlocks(context);
    showLanguageTag = PreferenceUtils.showLanguageTag(context);
    showConfidence = PreferenceUtils.shouldShowTextConfidence(context);
    textRecognizer = TextRecognition.getClient(textRecognizerOptions);
  }

  @Override
  public void stop() {
    super.stop();
    textRecognizer.close();
  }

  @Override
  protected Task<Text> detectInImage(InputImage image) {
    return textRecognizer.process(image);
  }

  @Override
  protected void onSuccess(@NonNull Text text, @NonNull GraphicOverlay graphicOverlay) {
    Log.d(TAG, "On-device Text detection successful");

    // Access the TextView by its ID
    result0 = ((Activity) context).findViewById(R.id.text0);

    if (result0 != null) {
      // Update the TextView with the recognized text
      result0.setText(extractDigits(text));
    }

    logExtrasForTesting(text);
    graphicOverlay.add(
            new TextGraphic(
                    graphicOverlay,
                    text,
                    shouldGroupRecognizedTextInBlocks,
                    showLanguageTag,
                    showConfidence));
  }
  // Method to extract only the digits from the recognized text
  private String extractDigits(Text text) {
    // Define character replacements using a HashMap
    HashMap<Character, Character> replacements = new HashMap<>();
    replacements.put('I', '1');
    replacements.put('i', '1');
    replacements.put('S', '5');
    replacements.put('s', '5');
    replacements.put('O', '0');
    replacements.put('o', '0');
    replacements.put('z', '2');
    replacements.put('Z', '2');
    // Add more replacements as needed

    StringBuilder digits = new StringBuilder();
    for (Text.TextBlock textBlock : text.getTextBlocks()) {
      for (Text.Line line : textBlock.getLines()) {
        for (Text.Element element : line.getElements()) {
          String elementText = element.getText();

          // Replace characters based on the defined replacements
          StringBuilder replacedText = new StringBuilder();
          for (char c : elementText.toCharArray()) {
            if (Character.isDigit(c)) {
              replacedText.append(c);
            } else {
              char replacement = replacements.getOrDefault(c, c);
              replacedText.append(replacement);
            }
          }

          // Check if the modified text contains only digits
          if (replacedText.toString().matches("\\d+")) {
            digits.append(replacedText);
          }
        }
      }
    }
    return digits.toString();
  }


  private static void logExtrasForTesting(Text text) {
    if (text != null) {

      //luodaan muuttuja tunnistetusta tekstistä
      String muuttuja = text.getText();
      //Kirjoitetaan muuttuja lokiin (Logcat)
      Log.i(MANUAL_TESTING_LOG,"TaTATATTATATATATTATATATATTATATATAT\n"+ muuttuja);

      Log.v(MANUAL_TESTING_LOG, "Detected text has : " + text.getTextBlocks().size() + " blocks");
      for (int i = 0; i < text.getTextBlocks().size(); ++i) {
        List<Line> lines = text.getTextBlocks().get(i).getLines();
        Log.v(
            MANUAL_TESTING_LOG,
            String.format("Detected text block %d has %d lines", i, lines.size()));
        for (int j = 0; j < lines.size(); ++j) {
          List<Element> elements = lines.get(j).getElements();
          Log.v(
              MANUAL_TESTING_LOG,
              String.format("Detected text line %d has %d elements", j, elements.size()));
          for (int k = 0; k < elements.size(); ++k) {
            Element element = elements.get(k);
            Log.v(
                MANUAL_TESTING_LOG,
                String.format("Detected text element %d says: %s", k, element.getText()));
            Log.v(
                MANUAL_TESTING_LOG,
                String.format(
                    "Detected text element %d has a bounding box: %s",
                    k, element.getBoundingBox().flattenToString()));
            Log.v(
                MANUAL_TESTING_LOG,
                String.format(
                    "Expected corner point size is 4, get %d", element.getCornerPoints().length));
            for (Point point : element.getCornerPoints()) {
              Log.v(
                  MANUAL_TESTING_LOG,
                  String.format(
                      "Corner point for element %d is located at: x - %d, y = %d",
                      k, point.x, point.y));
            }
          }
        }
      }
    }
  }

  @Override
  protected void onFailure(@NonNull Exception e) {
    Log.w(TAG, "Text detection failed." + e);
  }
}
