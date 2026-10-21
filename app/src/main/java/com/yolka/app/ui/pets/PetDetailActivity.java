package com.yolka.app.ui.pets;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class PetDetailActivity extends AppCompatActivity {
    public static final String EXTRA_PET = "extra_pet";
    public static final String EXTRA_INITIAL_TAB = "extra_initial_tab";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Expediente Clínico");
        setContentView(tv);
    }
}
