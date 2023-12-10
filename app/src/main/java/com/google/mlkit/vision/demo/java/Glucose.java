package com.google.mlkit.vision.demo.java;

import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.mlkit.vision.demo.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Glucose extends AppCompatActivity {

    private TextView textView;
    private Button sendDataButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.glucose);

        String detectedText = getIntent().getStringExtra("DETECTED_NUM");
        double detected;

        if (detectedText != null && !detectedText.isEmpty()) {
            if (!detectedText.contains(".")) {
                detected = Double.parseDouble(detectedText) / 10.0; // Parse as double
            } else {
                detected = Double.parseDouble(detectedText); // Parse as double
            }
        } else {
            detected = 5.6; // Set the value to 5.6 if detectedText is null or empty
        }

        Log.d("Detected", String.valueOf(detected));


        textView = findViewById(R.id.observationTextView); // Replace with your TextView ID
        sendDataButton = findViewById(R.id.sendDataButton);

        SharedPreferences sharedPref = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE);
        String storedResult = sharedPref.getString("barcodeResult", "");
        int id = Integer.parseInt(storedResult);

        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        String savedUsername = sharedPreferences.getString("USERNAME_KEY", "");
        sendDataButton.setOnClickListener(v -> {
            sendGlucoseDataToServer(detected);
            sendDataButton.setVisibility(View.GONE); // Hide the button after sending data
        });
        // Create a JSON object for the HL7 FHIR Observation
        JSONObject hl7FhirObservation = createHL7FhirObservation(detected);
        if (hl7FhirObservation != null) {
            // Convert the JSON object to a string for display or further use
            String observationJson = hl7FhirObservation.toString();

            // Create a Gson instance with pretty-print settings
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            // Format the JSON string using Gson
            String prettyJson = gson.toJson(hl7FhirObservation);

            // Display the formatted JSON string in the TextView
            textView.setText(prettyJson);

            // Log the generated JSON
            Log.d("Generated JSON", observationJson);
            Log.d("Pretty", prettyJson);
        }    }



    // Function to create HL7 FHIR Observation JSON
    private JSONObject createHL7FhirObservation(double detected) {
        try {
            JSONObject hl7FhirObservation = new JSONObject();

            // Populate the HL7 FHIR Observation JSON with the specified fields
            hl7FhirObservation.put("resourceType", "Observation");
            hl7FhirObservation.put("id", "blood-glucose");

            JSONObject text = new JSONObject();
            text.put("status", "generated");
            text.put("div", "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p><b>Generated Narrative: Observation</b>...</div>");


            JSONObject identifier = new JSONObject();
            identifier.put("use", "official");
            identifier.put("system", "http://www.bmc.nl/zorgportal/identifiers/observations");
            identifier.put("value", "6323");


            hl7FhirObservation.put("status", "final");


            JSONObject code = new JSONObject();
            JSONArray codingArray = new JSONArray();
            JSONObject coding = new JSONObject();
            coding.put("system", "http://loinc.org");
            coding.put("code", "15074-8");
            coding.put("display", "Glucose [Moles/volume] in Blood");


            SharedPreferences sharedPref = getSharedPreferences("MY_PREFS_NAME", MODE_PRIVATE);
            String storedResult = sharedPref.getString("barcodeResult", "");

            JSONObject subject = new JSONObject();
            subject.put("reference", "Patient/f001");
            subject.put("display", storedResult);

            hl7FhirObservation.put("subject", subject);


            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX", Locale.getDefault());
            String formattedTime = sdf.format(new Date()); // Get the current time in the specified format

            Log.d("Current Time", formattedTime); // Log the current time (optional)

            hl7FhirObservation.put("effectiveDateTime", formattedTime);
            hl7FhirObservation.put("issued", formattedTime);



            JSONObject device = new JSONObject();

            JSONArray extensionArray = new JSONArray();

            JSONObject deviceVersion = new JSONObject();
            deviceVersion.put("url", "DeviceVersion");
            deviceVersion.put("valueString", "TUAMK-2023");
            extensionArray.put(deviceVersion);

            SimpleDateFormat ymd = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            String formattedYMD = ymd.format(new Date()); // Get the current date in the specified format (yyyy-MM-dd)


            JSONObject lastSyncTime = new JSONObject();
            lastSyncTime.put("url", "lastSyncTime");
            lastSyncTime.put("valueDateTime", formattedYMD);
            extensionArray.put(lastSyncTime);

            device.put("extension", extensionArray);

            hl7FhirObservation.put("device", device);


            // Retrieve the username from SharedPreferences
            SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
            String savedUsername = sharedPreferences.getString("USERNAME_KEY", "");


            JSONArray performerArray = new JSONArray();
            JSONObject performer = new JSONObject();
            performer.put("reference", "Practitioner/f005");
            performer.put("display", savedUsername);
            performerArray.put(performer);

            hl7FhirObservation.put("performer", performerArray);


            JSONObject valueQuantity = new JSONObject();
            valueQuantity.put("value", detected);
            valueQuantity.put("unit", "mmol/l");
            valueQuantity.put("system", "http://unitsofmeasure.org");
            valueQuantity.put("code", "mmol/L");

            hl7FhirObservation.put("valueQuantity", valueQuantity);


            JSONArray interpretationArray = new JSONArray();
            JSONObject interpretation = new JSONObject();
            JSONArray codingInterpretationArray = new JSONArray();
            JSONObject codingInterpretation = new JSONObject();
            codingInterpretation.put("system", "http://terminology.hl7.org/CodeSystem/v3-ObservationInterpretation");
            codingInterpretation.put("code", "H");
            codingInterpretation.put("display", "High");
            codingInterpretationArray.put(codingInterpretation);
            interpretation.put("coding", codingInterpretationArray);
            interpretationArray.put(interpretation);

            hl7FhirObservation.put("interpretation", interpretationArray);


            JSONArray referenceRangeArray = new JSONArray();
            JSONObject referenceRange = new JSONObject();
            JSONObject low = new JSONObject();
            low.put("value", 3.1);
            low.put("unit", "mmol/l");
            low.put("system", "http://unitsofmeasure.org");
            low.put("code", "mmol/L");

            JSONObject high = new JSONObject();
            high.put("value", 6.2);
            high.put("unit", "mmol/l");
            high.put("system", "http://unitsofmeasure.org");
            high.put("code", "mmol/L");

            referenceRange.put("low", low);
            referenceRange.put("high", high);

            referenceRangeArray.put(referenceRange);

            hl7FhirObservation.put("referenceRange", referenceRangeArray);

            // Return the JSON object
            return hl7FhirObservation;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void sendGlucoseDataToServer(double detected) {
        new Thread(() -> {
            try {
                // URL of the server endpoint
                URL url = new URL("https://setalat.fi/FHIR-srv.php");

                // Open connection
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                // Construct JSON data similar to the PHP example
                JSONObject data = new JSONObject();
                data.put("resourceType", "Observation");

                JSONObject code = new JSONObject();
                JSONArray coding = new JSONArray();
                JSONObject codingObj = new JSONObject();
                codingObj.put("system", "http://loinc.org");
                codingObj.put("code", "15074-8");
                codingObj.put("display", "Glucose [Moles/volume] in Blood");
                coding.put(codingObj);
                code.put("coding", coding);
                data.put("code", code);

                JSONObject valueQuantity = new JSONObject();
                valueQuantity.put("value", detected);
                valueQuantity.put("unit", "mmol/l");
                data.put("valueQuantity", valueQuantity);

                // Convert JSON data to string and send it
                OutputStream os = conn.getOutputStream();
                os.write(data.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    // Read and handle the response from the server
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // Hide the JSON TextView
                    runOnUiThread(() -> {
                        TextView jsonTextView = findViewById(R.id.observationTextView);
                        jsonTextView.setVisibility(View.GONE);
                    });

                    // Handle the response here
                    String serverResponse = response.toString();
                    Log.d("ServerResponse", serverResponse);

                    // Parse HTML response to extract necessary data
                    String parsedResponse = parseHTMLResponse(serverResponse);

                    // Determine value status (low, normal, high) based on the detected value
                    String valueStatus;
                    String statusText;
                    int color;
                    if (detected < 3.9) {
                        valueStatus = "Low";
                        statusText = "Status: Low";
                        color = Color.BLUE;
                    } else if (detected >= 3.9 && detected <= 5.6) {
                        valueStatus = "Normal";
                        statusText = "Status: Normal";
                        color = Color.GREEN;
                    } else {
                        valueStatus = "High";
                        statusText = "Status: High";
                        color = Color.RED;
                    }

                    // Display parsed response along with value status in the responseTextView
                    String displayText = parsedResponse + "\n" + statusText;
                    SpannableString spannableString = new SpannableString(displayText);
                    ForegroundColorSpan colorSpan = new ForegroundColorSpan(color);
                    int startIndex = displayText.indexOf(statusText);
                    int endIndex = startIndex + statusText.length();
                    spannableString.setSpan(colorSpan, startIndex, endIndex, Spannable.SPAN_INCLUSIVE_INCLUSIVE);
                    runOnUiThread(() -> {
                        TextView responseTextView = findViewById(R.id.responseTextView); // Replace with your TextView ID
                        responseTextView.setText(spannableString);
                        responseTextView.setVisibility(View.VISIBLE);
                    });

                    // Process the server response as needed
                } else {
                    // Handle failure or non-OK response code
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private String parseHTMLResponse(String htmlResponse) {
        String extractedData = "";

        // Extracting required data using regular expressions (modify this based on your HTML structure)
        Pattern pattern = Pattern.compile("<td>(.*?)</td><td>(.*?)</td>");
        Matcher matcher = pattern.matcher(htmlResponse);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);
            extractedData += key + ": " + value + "\n";
        }

        return extractedData;
    }


}
