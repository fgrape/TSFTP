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

public class DownloadActivity extends AppCompatActivity {

    private DownloadHandler handler = new DownloadHandler();
    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_download);

        Intent intent = getIntent();
        String action = intent.getAction();
        Uri data = intent.getData();
        System.out.print(data);

        Button downLoadButton = (Button) findViewById(R.id.downloadButton);
        downLoadButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                // execute this when the downloader must be fired
                final DownloadTask downloadTask = new DownloadTask(DownloadActivity.this);
                downloadTask.execute("fileID");

                progressDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        downloadTask.cancel(true);
                    }
                });
            }
        });

        progressDialog = new ProgressDialog(DownloadActivity.this);
        progressDialog.setMessage("Downloading cat gifs!");
        progressDialog.setIndeterminate(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setCancelable(true);
    }

    private class DownloadTask extends AsyncTask<String, Integer, Void> {

        private Context context;
        private PowerManager.WakeLock mWakeLock;

        public DownloadTask(Context context) {
            this.context = context;
        }

        protected Void doInBackground(String... params) {
            InputStream input = null;
            FileOutputStream output = null;
            HttpURLConnection connection = null;
            String fileID  = params[0];
            DownloadResult result = handler.downloadFile(fileID);
            try {
                //TODO: Move to HTTPSConnectionHandler
                URL url = new URL("http://172.31.212.116/tsftp.php/" + fileID);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                // expect HTTP 200 OK, so we don't mistakenly save error report
                // instead of the file
                /*if (connection.getResponseCode() != HttpURLConnection.HTTP_OK) {
                    return "Server returned HTTP " + connection.getResponseCode()
                            + " " + connection.getResponseMessage();
                }*/

                // this will be useful to display download percentage
                // might be -1: server did not report the length
                int fileLength = connection.getContentLength();

                // download the file
                input = connection.getInputStream();
                if (isExternalStorageWritable()) {
                    File file = new File(Environment.getExternalStoragePublicDirectory("TSFTP/Downloads"), fileID);
                    output = new FileOutputStream(file);
                }
                byte data[] = new byte[4096];
                long total = 0;
                int count;
                while ((count = input.read(data)) != -1) {
                    // allow canceling with back button
                    if (isCancelled()) {
                        input.close();
                        return null;
                    }
                    total += count;
                    // publishing the progress....
                    if (fileLength > 0) // only if total length is known
                        publishProgress((int) (total * 100 / fileLength));
                    output.write(data, 0, count);
                }
            } catch (Exception e) {
                //return e.toString();
                //TODO: Do something
            } finally {
                try {
                    if (output != null)
                        output.close();
                    if (input != null)
                        input.close();
                } catch (IOException ignored) {
                }

                if (connection != null)
                    connection.disconnect();
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

        public boolean isExternalStorageWritable() {
            String state = Environment.getExternalStorageState();
            if (Environment.MEDIA_MOUNTED.equals(state)) {
                return true;
            }
            return false;
        }

        @Override
        protected void onProgressUpdate(Integer... progress) {
            super.onProgressUpdate(progress);
            // if we get here, length is known, now set indeterminate to false
            progressDialog.setIndeterminate(false);
            progressDialog.setMax(100);
            progressDialog.setProgress(progress[0]);
        }

        //@Override
        protected void onPostExecute(String result) {
            mWakeLock.release();
            progressDialog.dismiss();
            Scanner in = null;
            if (result != null) {
                Toast.makeText(context, "Download error: " + result, Toast.LENGTH_LONG).show();
            }else {
                Toast.makeText(context, "File downloaded", Toast.LENGTH_LONG).show();
                try {
                    in = new Scanner(new BufferedInputStream(openFileInput("index.html")));
                    StringBuilder sb = new StringBuilder();

                    while (in.hasNext()){
                        sb.append(in.next());
                    }

                    Toast.makeText(context, sb.toString(), Toast.LENGTH_SHORT).show();
                } catch (Exception e){
                    e.printStackTrace();
                }
            }
        }
    }

}
