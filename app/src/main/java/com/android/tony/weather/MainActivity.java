package com.android.tony.weather;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import org.litepal.LitePal;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        LitePal.getDatabase();
    }
}
