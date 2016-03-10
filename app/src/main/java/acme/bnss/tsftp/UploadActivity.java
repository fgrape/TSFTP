package acme.bnss.tsftp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
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

        Button uploadButton = (Button) findViewById(R.id.uploadButton);
        uploadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // execute this when the downloader must be fired
                final UploadTask uploadTask = new UploadTask(UploadActivity.this);
                EditText emailET = (EditText)findViewById(R.id.receiverEmailField);
                String email = emailET.getText().toString();

                EditText fileET = (EditText)findViewById(R.id.fileField);
                String fileString = fileET.getText().toString();
                if (isExternalStorageWritable()) {
                    File file = new File(Environment.getExternalStoragePublicDirectory("TSFTP"), fileString);
                    uploadTask.execute(new EmailFileTuple(email, file));
                }
                else {
                    Context context = getApplicationContext();
                    CharSequence text = "Cannot access SD Card internal memory";
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }



                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        uploadTask.cancel(true);
                    }
                });
            }
        });

        progressDialog = new ProgressDialog(UploadActivity.this);
        progressDialog.setMessage("Uploading cat gifs!");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(true);
    }

    public void downloadView(View view){
        Intent intent = new Intent(this, DownloadActivity.class);
        //Add trivial code
        startActivity(intent);
    }

    private class UploadTask extends AsyncTask<EmailFileTuple, Integer, Void> {

        private Context context;


        public UploadTask(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(EmailFileTuple... params) {
            String email = params[0].getEmail();
            File file  = params[0].getFile();
            final UploadResult result = handler.uploadFile(email, file, progressDialog);
            if (!result.wasSuccessful()){
                runOnUiThread(new Runnable() {
                    public void run() {
                        CharSequence text = "Didn't work yo check this error: " + result.getMessage();
                        int duration = Toast.LENGTH_LONG;
                        Toast toast = Toast.makeText(UploadActivity.this, text, duration);
                        toast.show();
                    }
                });
            }
            final String fileLink = result.getFileDescriptor().getFileLink();
            String server = result.getFileDescriptor().getServer();
            String hash = result.getFileDescriptor().getHash();
            String fileName = result.getFileDescriptor().getFileName();

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView resultText = (TextView)findViewById(R.id.fileLinkText);
                    resultText.setText(fileLink);
                }
            });


            return null;
        }

    }
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

}
