package com.stirante.instaprefs.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Environment;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * Created by stirante
 */
public class FileUtils {

    public static final File INSTAPREFS_DIR = new File(Environment.getExternalStorageDirectory() + "/Instaprefs/");

    public static void download(String src, File dst, Context context) {
        if (context != null) Toast.makeText(context, "Starting download...", Toast.LENGTH_SHORT).show();
        new DownloadFileFromURL(src, dst, context).execute();
    }

    static class DownloadFileFromURL extends AsyncTask<Void, Void, Boolean> {
        private String src;
        private File dst;
        private Context context;

        public DownloadFileFromURL(String src, File dst, Context context) {
            this.src = src;
            this.dst = dst;
            this.context = context;
        }

        @Override
        protected Boolean doInBackground(Void... f_url) {
            int count;
            try {
                dst.getParentFile().mkdirs();
                URL url = new URL(src);
                URLConnection connection = url.openConnection();
                connection.connect();
                InputStream input = url.openStream();
                OutputStream output = new FileOutputStream(dst);
                byte buffer[] = new byte[1024];
                while ((count = input.read(buffer)) != -1) {
                    output.write(buffer, 0, count);
                }
                output.flush();
                output.close();
                input.close();
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (result) {
                if (context != null) Toast.makeText(context, "Downloaded!", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
