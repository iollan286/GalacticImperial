package com.galacticimperial;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // EXIT — closes the app
        TextView btnExit = findViewById(R.id.btn_exit);
        btnExit.setOnClickListener(v -> finishAndRemoveTask());

        // NEW TUTORIAL GAME — opens the tutorial selection menu
        TextView btnTutorial = findViewById(R.id.btn_new_tutorial);
        btnTutorial.setOnClickListener(v ->
            startActivity(new Intent(this, TutorialMenuActivity.class))
        );
    }
}
