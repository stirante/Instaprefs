package com.stirante.instaprefs;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;

import com.alexvasilkov.gestures.views.GestureImageView;

import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ZoomActivity extends AppCompatActivity {

    private GestureImageView picture;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        try {
            getSupportActionBar().hide();
            getActionBar().hide();
        } catch (Throwable t) {
            //shhhhh...
        }
        picture = (GestureImageView) findViewById(R.id.picture);
        picture.getController().getSettings().setMaxZoom(5f);
        if (getIntent().hasExtra("url")) {
            new GetImageFromURL(getIntent().getStringExtra("url")).execute();
        }
        else {
            finish();
        }
    }

    class GetImageFromURL extends AsyncTask<Void, Void, Bitmap> {
        private String src;

        public GetImageFromURL(String src) {
            this.src = src;
        }

        @Override
        protected Bitmap doInBackground(Void... f_url) {
            try {
                URL url = new URL(src);
                URLConnection connection = url.openConnection();
                connection.connect();
                InputStream input = url.openStream();
                Bitmap bitmap = BitmapFactory.decodeStream(input);
                input.close();
                return bitmap;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) picture.setImageBitmap(result);
        }
    }
}
