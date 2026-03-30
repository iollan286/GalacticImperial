package com.galacticimperial;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class TutorialMenuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutorial_menu);

        // BACK → return to main menu
        TextView btnBack = findViewById(R.id.btn_back);
        btnBack.setOnClickListener(v -> finish());

        // Tutorial 1 → open game settings for Tutorial 1
        TextView btnTutorial1 = findViewById(R.id.btn_tutorial1);
        btnTutorial1.setOnClickListener(v -> {
            Intent intent = new Intent(this, GameSettingsActivity.class);
            intent.putExtra("tutorial_title", "TUTORIAL 1: Getting Things Running");
            startActivity(intent);
        });
    }
}
