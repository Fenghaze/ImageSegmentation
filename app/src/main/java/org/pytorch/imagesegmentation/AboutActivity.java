package org.pytorch.imagesegmentation;

import android.os.Bundle;

public class AboutActivity extends AppCompatPreferenceActivity{

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.about);
    }
}
