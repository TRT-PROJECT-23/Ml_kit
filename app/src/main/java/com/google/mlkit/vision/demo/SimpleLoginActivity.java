package com.google.mlkit.vision.demo;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.mlkit.vision.demo.R;

public class SimpleLoginActivity extends AppCompatActivity {
    private static final int CAMERA_PERMISSION_REQUEST_CODE = 100;
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.valid_ui);

        buttonLogin = findViewById(R.id.buttonLogin); // Find your button by its ID

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                checkAndRequestPermissions();
            }
        });
    }

    private void checkAndRequestPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

            // Permissions not granted, request them
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                    CAMERA_PERMISSION_REQUEST_CODE);
        } else {
            // Permissions already granted, proceed with navigation
            navigateToChooserActivity();
        }
    }


    // Handle permission request results
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CAMERA_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED &&
                    grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                // Both Camera and Storage permissions granted
                navigateToChooserActivity();
            } else {
                // Handle permission denial scenario (show a message, etc.)
            }
        }
    }

    // Save the username to SharedPreferences
    private void saveUsername(String username) {
        SharedPreferences sharedPreferences = getSharedPreferences("MyPrefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString("USERNAME_KEY", username);
        editor.apply();
    }

    // Call this method to save the username
    private void navigateToChooserActivity() {
        EditText usernameEditText = findViewById(R.id.editTextUsername); // Replace with your EditText ID
        String username = usernameEditText.getText().toString().trim();

        if (!username.isEmpty()) {
            // Username retrieved successfully, proceed to the next activity
            saveUsername(username); // Save username using SharedPreferences or any other method
            Intent intent = new Intent(this, CodeScanner.class);
            startActivity(intent);
        } else {
            // Username is empty, set a default username (e.g., "K.Kotihoitaja")
            username = "K.Kotihoitaja";
            saveUsername(username); // Save the default username
            // Proceed to the next activity or handle this case as needed
            Intent intent = new Intent(this, CodeScanner.class);
            startActivity(intent);
        }
    }


}
