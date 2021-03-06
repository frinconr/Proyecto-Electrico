package com.example.felipe.harmony3;


import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;



/**
 * This Activity appears when starting the application.
 * It is the welcome screen where the user can select
 * between using the smartphone or the tablet configuration.
 */

public class MainActivity extends AppCompatActivity{



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        // Smartphone button configuration, launches smartphone activity with a slide in from left
        // animation.

        Button phone_button = (Button) findViewById(R.id.smartphone_button);

        assert phone_button != null;
        phone_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, PhoneActivity.class);
                myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                MainActivity.this.startActivity(myIntent);
                overridePendingTransition(R.anim.slide_in_from_left, R.anim.fade_out);
            }
        });


        // Tablet button configuration, launches smartphone activity with a slide in from right
        // animation.

        Button tablet_button = (Button) findViewById(R.id.tablet_button);
        assert tablet_button != null;
        tablet_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, TabletActivity.class);
                myIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
                myIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                MainActivity.this.startActivity(myIntent);
                overridePendingTransition(R.anim.slide_in_from_right, R.anim.fade_out);
            }
        });
    }


}
