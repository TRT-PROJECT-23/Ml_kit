package com.google.mlkit.vision.demo.java;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.mlkit.vision.demo.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class Glucose extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


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

        setContentView(R.layout.glucose);




        textView = findViewById(R.id.observationTextView); // Replace with your TextView ID

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
}
