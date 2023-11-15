package com.google.mlkit.vision.demo;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
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

    private void navigateToChooserActivity() {
        Intent intent = new Intent(this, CodeScanner.class);
        startActivity(intent);
    }
}
