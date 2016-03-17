package acme.bnss.tsftp;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.os.PowerManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Scanner;

public class DownloadActivity extends AppCompatActivity {

    private DownloadHandler2 handler = new DownloadHandler2();
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        Button getSenderButton = (Button) findViewById(R.id.getSenderIDButton);
        getSenderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // execute this when the downloader must be fired
                final GetSenderTask getSenderTask = new GetSenderTask(DownloadActivity.this);
                EditText fileIDET = (EditText)findViewById(R.id.fileIDField);
                String fileID = fileIDET.getText().toString();
                getSenderTask.execute(fileID);
            }
        });

        Button downloadButton = (Button) findViewById(R.id.downloadButton);
        downloadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // execute this when the downloader must be fired
                final DownloadTask downloadTask = new DownloadTask(DownloadActivity.this);
                EditText fileIDET = (EditText)findViewById(R.id.fileIDField);
                String fileID = fileIDET.getText().toString();
                if (isExternalStorageWritable()) {
                    downloadTask.execute(fileID);
                }
                else {
                    Context context = getApplicationContext();
                    CharSequence text = "Cannot access SD Card memory";
                    int duration = Toast.LENGTH_LONG;
                    Toast toast = Toast.makeText(context, text, duration);
                    toast.show();
                }

                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        downloadTask.cancel(true);
                    }
                });
            }
        });

        progressDialog = new ProgressDialog(DownloadActivity.this);
        progressDialog.setMessage("Downloading file...");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(true);
    }

    public void downloadView(View view){
        Intent intent = new Intent(this, DownloadActivity.class);
        //Add trivial code
        startActivity(intent);
    }

    private class DownloadTask extends AsyncTask<String, Integer, Void> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;


        public DownloadTask(Context context) {
            this.context = context;
        }

        @Override
        protected Void doInBackground(String... params) {
            String fileID = params[0];
            final DownloadResult result = handler.downloadFile(fileID);
            if (!result.wasSuccessfull()){
                runOnUiThread(new Runnable() {
                    public void run() {
                        CharSequence text = "File not downloaded: " + result.getMessage();
                        int duration = Toast.LENGTH_LONG;
                        Toast toast = Toast.makeText(DownloadActivity.this, text, duration);
                        toast.show();
                    }
                });
            } else {
                //Toast some stuff
                runOnUiThread(new Runnable() {
                    public void run() {
                        CharSequence text = "File successfully downloaded:\n" + result.getFileName();
                        int duration = Toast.LENGTH_LONG;
                        Toast toast = Toast.makeText(DownloadActivity.this, text, duration);
                        toast.show();
                    }
                });
            }



            return null;
        }
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // take CPU lock to prevent CPU from going off if the user
            // presses the power button during download
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    getClass().getName());
            mWakeLock.acquire();
            progressDialog.show();
        }
        @Override
        protected void onPostExecute(Void v) {
            mWakeLock.release();
            progressDialog.dismiss();
        }

    }
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    private class GetSenderTask extends AsyncTask<String, Integer, Void> {

        public GetSenderTask(Context context) {

        }

        @Override
        protected Void doInBackground(String... params) {
            String fileID = params[0];
            final String senderName = handler.getSenderName(fileID);
            //SenderIDText
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    TextView resultText = (TextView)findViewById(R.id.senderIDText);
                    resultText.setText(senderName);
                }
            });
            return null;
        }
    }



}
