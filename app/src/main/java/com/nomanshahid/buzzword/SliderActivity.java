package com.nomanshahid.buzzword;

import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Objects;

public class SliderActivity extends AppCompatActivity {
    private TextView phoneTitle;
    private TextView definition;
    private Button tryNewNumber;
    private String wordName;
    private String wordDefinition;
    private String phoneNumber;

    public SliderActivity() {
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slider);
        phoneTitle = (TextView) findViewById(R.id.phoneTitle);
        definition = (TextView) findViewById(R.id.definition);
        tryNewNumber = (Button) findViewById(R.id.tryNewNumberBtn);
        wordName = getIntent().getExtras().getString("WORD");
        wordDefinition = getIntent().getExtras().getString("DEFINITION");
        phoneNumber = getIntent().getExtras().getString("PHONE_NUMBER");
        if(Objects.equals(wordName, "Nothing found!")) {
            phoneTitle.setText(wordName);
            definition.setText(wordDefinition);
        } else {
            phoneTitle.setText(prettyPhoneNumber(phoneNumber));
            definition.setText(wordDefinition);
        }
        tryNewNumber.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(SliderActivity.this, MainActivity.class));
            }
        });

    }

    public String prettyPhoneNumber (String stringPhoneNumber) {
        String formattedNumber = "(";
        for (int i = 0; i <= 6; ++i) {
            if (i == 6) {
                formattedNumber += "-" + wordName.toUpperCase();

            } else {
                if (i == 3) formattedNumber += ") ";
                formattedNumber += stringPhoneNumber.charAt(i);
            }
        }
        return formattedNumber;
    }

    //TODO: Multiple defintions
}
