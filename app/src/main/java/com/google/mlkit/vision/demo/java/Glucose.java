package com.google.mlkit.vision.demo.java;

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



public class Glucose extends AppCompatActivity {

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        String detectedText = getIntent().getStringExtra("DETECTED_NUM");
        float detected = Float.parseFloat(detectedText);
        detected = detected/10.0f;

        Log.d("Detected", String.valueOf(detected));
        setContentView(R.layout.glucose);

        textView = findViewById(R.id.observationTextView); // Replace with your TextView ID

        // Create a JSON object for the HL7 FHIR Observation
        JSONObject hl7FhirObservation = createHL7FhirObservation();
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
        }    }



    // Function to create HL7 FHIR Observation JSON
    private JSONObject createHL7FhirObservation() {
        try {
            JSONObject hl7FhirObservation = new JSONObject();

            // Populate the HL7 FHIR Observation JSON with the specified fields
            hl7FhirObservation.put("resourceType", "Observation");
            hl7FhirObservation.put("id", "blood-glucose");

            // Add the 'meta' field
            JSONObject meta = new JSONObject();
            meta.put("versionId", "4");
            meta.put("lastUpdated", "2020-08-04T08:03:31.384Z");
            JSONArray profileArray = new JSONArray();
            profileArray.put("http://hl7.org/fhir/us/core/StructureDefinition/us-core-observation-lab");
            meta.put("profile", profileArray);
            hl7FhirObservation.put("meta", meta);

            // Add the 'text' field
            JSONObject text = new JSONObject();
            text.put("status", "generated");
            text.put("div", "<div xmlns=\"http://www.w3.org/1999/xhtml\"><p><b>Generated Narrative: Observation</b>...</div>");
            hl7FhirObservation.put("text", text);

            // Add other fields similarly

            // Add 'status' field
            hl7FhirObservation.put("status", "final");

            // Add 'category' field
            JSONObject categoryObj = new JSONObject();
            JSONArray categoryArray = new JSONArray();
            JSONObject coding = new JSONObject();
            coding.put("system", "http://terminology.hl7.org/CodeSystem/observation-category");
            coding.put("code", "laboratory");
            coding.put("display", "Laboratory");
            categoryArray.put(coding);
            categoryObj.put("coding", categoryArray);
            categoryObj.put("text", "Laboratory");
            hl7FhirObservation.put("category", categoryArray);

            // Add 'code' field
            JSONObject codeObj = new JSONObject();
            JSONArray codingArray = new JSONArray();
            JSONObject codeCoding = new JSONObject();
            codeCoding.put("system", "http://loinc.org");
            codeCoding.put("code", "2339-0");
            codeCoding.put("display", "Glucose Bld-mCnc");
            codingArray.put(codeCoding);
            codeObj.put("coding", codingArray);
            codeObj.put("text", "Glucose Bld-mCnc");
            hl7FhirObservation.put("code", codeObj);

            // Add 'subject' field
            JSONObject subjectObj = new JSONObject();
            subjectObj.put("reference", "Patient/example1");
            subjectObj.put("display", "Amy Shaw");
            hl7FhirObservation.put("subject", subjectObj);

            // Add 'effectiveDateTime' field
            hl7FhirObservation.put("effectiveDateTime", "2005-07-05");

            // Add 'valueQuantity' field
            JSONObject valueQuantityObj = new JSONObject();
            valueQuantityObj.put("value", 76);
            valueQuantityObj.put("unit", "mg/dL");
            valueQuantityObj.put("system", "http://unitsofmeasure.org");
            hl7FhirObservation.put("valueQuantity", valueQuantityObj);

            // Add 'referenceRange' field
            JSONObject referenceRangeObj = new JSONObject();
            JSONObject lowObj = new JSONObject();
            lowObj.put("value", 40);
            lowObj.put("unit", "mg/dL");
            lowObj.put("system", "http://unitsofmeasure.org");
            lowObj.put("code", "mg/dL");
            JSONObject highObj = new JSONObject();
            highObj.put("value", 109);
            highObj.put("unit", "mg/dL");
            highObj.put("system", "http://unitsofmeasure.org");
            highObj.put("code", "mg/dL");
            JSONArray appliesToArray = new JSONArray();
            JSONObject appliesToObj = new JSONObject();
            JSONArray codingArrayAppliesTo = new JSONArray();
            JSONObject codingAppliesTo = new JSONObject();
            codingAppliesTo.put("system", "http://terminology.hl7.org/CodeSystem/referencerange-meaning");
            codingAppliesTo.put("code", "normal");
            codingAppliesTo.put("display", "Normal Range");
            codingArrayAppliesTo.put(codingAppliesTo);
            appliesToObj.put("coding", codingArrayAppliesTo);
            appliesToObj.put("text", "Normal Range");
            appliesToArray.put(appliesToObj);
            referenceRangeObj.put("low", lowObj);
            referenceRangeObj.put("high", highObj);
            referenceRangeObj.put("appliesTo", appliesToArray);
            JSONArray referenceRangeArray = new JSONArray();
            referenceRangeArray.put(referenceRangeObj);
            hl7FhirObservation.put("referenceRange", referenceRangeArray);

            // Return the JSON object
            return hl7FhirObservation;
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
