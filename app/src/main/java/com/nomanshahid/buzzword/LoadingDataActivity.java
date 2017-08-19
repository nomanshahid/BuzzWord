package com.nomanshahid.buzzword;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ProgressBar;

public class LoadingDataActivity extends AppCompatActivity {

    ProgressBar loadingData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_loading_data);
        loadingData = (ProgressBar) findViewById(R.id.progressBar);
    }
}
