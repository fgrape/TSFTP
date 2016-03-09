package acme.bnss.tsftp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class UploadActivity extends AppCompatActivity {

    private UploadHandler handler = new UploadHandler();
    ProgressDialog progressDialog;

    public UploadActivity() {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        Button uploadButton = (Button) findViewById(R.id.fab);
        uploadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // execute this when the downloader must be fired
                final UploadTask uploadTask = new UploadTask(UploadActivity.this);
                uploadTask.execute("email", "File");

                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        uploadTask.cancel(true);
                    }
                });
            }
        });

        progressDialog = new ProgressDialog(UploadActivity.this);
        progressDialog.setMessage("A message");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(true);
    }

    private class UploadTask extends AsyncTask<String, Integer, Void> {

        private Context context;

        public UploadTask(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(String... params) {
            String email = params[0];
            String file  = params[1];
            UploadResult result = handler.uploadFile(email, file, progressDialog);

            return null;
        }

    }

}
