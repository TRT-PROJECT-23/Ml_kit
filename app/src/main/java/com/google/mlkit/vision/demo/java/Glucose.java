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
        }
        sendDataButton.setOnClickListener(v -> {
            sendGlucoseDataToServer(hl7FhirObservation);
            sendDataButton.setVisibility(View.GONE); // Hide the button after sending data
        });
    }



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

            String interpretationCode;
            String interpretationDisplay;

            if (detected < 3.1) {
                interpretationCode = "L"; // Interpretation code for Low
                interpretationDisplay = "Low";
            } else if (detected >= 3.1 && detected <= 6.2) {
                interpretationCode = "N"; // Interpretation code for Normal
                interpretationDisplay = "Normal";
            } else {
                interpretationCode = "H"; // Interpretation code for High
                interpretationDisplay = "High";
            }

            codingInterpretation.put("code", interpretationCode);
            codingInterpretation.put("display", interpretationDisplay);
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

    private void sendGlucoseDataToServer(JSONObject hl7FhirObservation) {
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

                //JSON to String
                String formattedJson = hl7FhirObservation.toString();

                // Write the JSON string to the output stream
                OutputStream os = conn.getOutputStream();
                os.write(formattedJson.getBytes("UTF-8"));
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


                    // Display parsed response along with value status in the responseTextView

                    runOnUiThread(() -> {
                        TextView responseTextView = findViewById(R.id.responseTextView); // Replace with your TextView ID
                        responseTextView.setText(parsedResponse);
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

    private String parseHTMLResponse(String htmlResponse) throws JSONException {
        StringBuilder extractedData = new StringBuilder();

        // Extracting specific data using a custom regular expression pattern
        Pattern pattern = Pattern.compile("<td>(resourceType|id|status|subject|effectiveDateTime|device|performer|valueQuantity|interpretation)</td><td>(.*?)</td>");
        Matcher matcher = pattern.matcher(htmlResponse);

        while (matcher.find()) {
            String key = matcher.group(1);
            String value = matcher.group(2);

            // Format key-value pairs
            switch (key) {
                case "resourceType":
                    extractedData.append("Resource Type: ").append(value).append("\n");
                    break;
                case "id":
                    extractedData.append("ID: ").append(value).append("\n");
                    break;
                case "status":
                    extractedData.append("Status: ").append(value).append("\n");
                    break;
                case "effectiveDateTime":
                    extractedData.append("Effective Date Time: ").append(value).append("\n");
                    break;
                case "subject":
                    JSONObject subjectJson = new JSONObject(value);
                    String reference = subjectJson.optString("reference", "");
                    String display = subjectJson.optString("display", "");
                    extractedData.append("Subject Reference: ").append(reference).append("\n");
                    extractedData.append("Subject Display: ").append(display).append("\n");
                    break;
                case "device":
                    JSONObject deviceJson = new JSONObject(value);
                    JSONArray extensions = deviceJson.optJSONArray("extension");
                    if (extensions != null && extensions.length() > 0) {
                        JSONObject firstExtension = extensions.optJSONObject(0);
                        if (firstExtension != null) {
                            String url = firstExtension.optString("url", "");
                            String valueString = firstExtension.optString("valueString", "");
                            extractedData.append("Device Extension - ").append(url).append(": ").append(valueString).append("\n");
                        }
                    }
                    break;
                case "performer":
                    JSONArray performers = new JSONArray(value);
                    for (int i = 0; i < performers.length(); i++) {
                        JSONObject performer = performers.optJSONObject(i);
                        if (performer != null) {
                            String referenceValue = performer.optString("reference", "");
                            String displayValue = performer.optString("display", "");
                            extractedData.append("Performer ").append(i + 1).append(" Reference: ").append(referenceValue).append("\n");
                            extractedData.append("Performer ").append(i + 1).append(" Display: ").append(displayValue).append("\n");
                        }
                    }
                    break;
                case "valueQuantity":
                    JSONObject valueQuantityJson = new JSONObject(value);
                    double quantityValue = valueQuantityJson.optDouble("value", 0.0);
                    String unit = valueQuantityJson.optString("unit", "");
                    extractedData.append("Value Quantity: ").append(quantityValue).append(" ").append(unit).append("\n");
                    break;
                case "interpretation":
                    try {
                        JSONArray interpretationArray = new JSONArray(value);
                        for (int i = 0; i < interpretationArray.length(); i++) {
                            JSONObject interpretationObj = interpretationArray.getJSONObject(i);
                            JSONArray codingArray = interpretationObj.getJSONArray("coding");
                            for (int j = 0; j < codingArray.length(); j++) {
                                JSONObject codingObj = codingArray.getJSONObject(j);
                                String interpretationCode = codingObj.optString("code", "");
                                String interpretationDisplay = codingObj.optString("display", "");
                                extractedData.append("Interpretation Code: ").append(interpretationCode).append("\n");
                                extractedData.append("Interpretation Display: ").append(interpretationDisplay).append("\n");
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    break;
                default:
                    extractedData.append(key).append(": ").append(value).append("\n");
                    break;
            }
        }

        return extractedData.toString();
    }






}
