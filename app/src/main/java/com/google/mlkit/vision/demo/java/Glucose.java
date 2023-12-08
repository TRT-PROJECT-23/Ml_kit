package com.google.mlkit.vision.demo.java;

import android.content.SharedPreferences;
import android.os.Bundle;
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
            sendGlucoseDataToServer(id, detected, savedUsername);
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

    private void sendGlucoseDataToServer(int pId, double detected, String savedUsername) {
        new Thread(() -> {
            try {
                URL url = new URL("http://ServerAddress/" + pId); //Replace with actual address
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setRequestProperty("Accept", "application/json");
                conn.setDoOutput(true);

                // Create JSON object for the request body
                JSONObject requestBody = new JSONObject();
                requestBody.put("practitioner", savedUsername);
                requestBody.put("result", detected);

                // Convert request body to string and send it
                OutputStream os = conn.getOutputStream();
                os.write(requestBody.toString().getBytes("UTF-8"));
                os.flush();
                os.close();

                int responseCode = conn.getResponseCode();

                if (responseCode == HttpURLConnection.HTTP_CREATED) {
                    // Glucose data added successfully
                    Log.d("GlucoseAdded", "Glucose data added for person with ID " + pId);
                    // Handle success or perform any action on successful addition

                    // Fetch and display person details after adding glucose data
                    fetchPersonDetails(pId);
                } else {
                    // Failed to add glucose data
                    Log.d("GlucoseNotAdded", "Failed to add glucose data");
                    // Handle failure or perform any action on failed addition
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Method to fetch and display person details after adding glucose data
    private void fetchPersonDetails(int personId) {
        new Thread(() -> {
            try {
                URL url = new URL("http://ServerAddress/" + personId); //Replace with actual address
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("Accept", "application/json");

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String inputLine;
                    StringBuilder response = new StringBuilder();
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();

                    // Handle the response (stored in 'response' StringBuilder) here
                    Log.d("Response", response.toString());

                    // Update UI or perform actions based on the response data
                    // For example, parse the JSON response and display in TextView
                    JSONObject jsonResponse = new JSONObject(response.toString());

                    // Format the JSON for display in TextView
                    String formattedJson = formatJson(jsonResponse);

                    // Update UI with formatted JSON data
                    runOnUiThread(() -> {
                        TextView personDetailsTextView = findViewById(R.id.observationTextView); // Replace with your TextView ID
                        personDetailsTextView.setText(formattedJson);
                    });
                } else {
                    // Handle unsuccessful response
                    Log.d("FetchFailed", "Failed to fetch person details. Response code: " + responseCode);
                }

                conn.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Helper method to format JSON for display
    private String formatJson(JSONObject jsonObject) {
        try {
            return jsonObject.toString(4); // Indentation of 4 spaces for better readability
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return "";
    }

}
