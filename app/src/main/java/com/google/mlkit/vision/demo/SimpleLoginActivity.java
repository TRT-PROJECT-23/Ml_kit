package com.google.mlkit.vision.demo;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.mlkit.vision.demo.R;

public class SimpleLoginActivity extends AppCompatActivity {
    private Button buttonLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.valid_ui);

        buttonLogin = findViewById(R.id.buttonLogin); // Find your button by its ID

        buttonLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // Handle the button click event here
                // You can navigate to another activity or perform any other action.
                navigateToChooserActivity(); // Call the method to navigate to ChooserActivity
            }
        });
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
