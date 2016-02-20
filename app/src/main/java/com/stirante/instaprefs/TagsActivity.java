package com.stirante.instaprefs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class TagsActivity extends AppCompatActivity {

    private EditText text;
    private ArrayAdapter<String> adapter;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tags);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Button addButton = (Button) findViewById(R.id.add_button);
        text = ((EditText) findViewById(R.id.tag_text));
        ListView list = ((ListView) findViewById(R.id.tag_list));
        ArrayList<String> objects = new ArrayList<>();
        String[] ignored = prefs.getString("ignored_tags", "").split(";");
        if (ignored.length != 1 || !ignored[0].isEmpty())
            Collections.addAll(objects, ignored);
        adapter = new MyAdapter(this, objects);
        addButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (text.getText().toString().isEmpty()) return;
                adapter.add(text.getText().toString());
                prefs.edit().putString("ignored_tags", prefs.getString("ignored_tags", "") + text.getText().toString() + ";").apply();
                text.getText().clear();
            }
        });
        list.setAdapter(adapter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        File prefsDir = new File(getApplicationInfo().dataDir, "shared_prefs");
        File prefsFile = new File(prefsDir, getPackageName() + "_preferences.xml");
        if (prefsFile.exists()) {
            prefsFile.setReadable(true, false);
        }
    }

    class MyAdapter extends ArrayAdapter<String> {

        public MyAdapter(Context context, ArrayList<String> objects) {
            super(context, R.layout.tag_list_item, R.id.tag_name, objects);
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            v.findViewById(R.id.remove_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    remove(getItem(position));
                    String s = "";
                    for (int i = 0; i < getCount(); i++) {
                        s += getItem(i) + ";";
                    }
                    prefs.edit().putString("ignored_tags", s).apply();
                }
            });

            return v;
        }
    }

}
