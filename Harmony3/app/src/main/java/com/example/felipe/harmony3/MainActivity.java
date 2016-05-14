package com.example.felipe.harmony3;


import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//************************************* Smartphone Button ******************************************

        Button phone_button = (Button) findViewById(R.id.smartphone_button);

        assert phone_button != null;
        phone_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, PhoneActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });


//************************************* Client Button **********************************************
        Button tablet_button = (Button) findViewById(R.id.tablet_button);
        assert tablet_button != null;
        tablet_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(MainActivity.this, TabletActivity.class);
                MainActivity.this.startActivity(myIntent);
            }
        });
//**************************************************************************************************

    }
}
