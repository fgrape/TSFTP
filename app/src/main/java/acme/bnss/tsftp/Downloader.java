package acme.bnss.tsftp;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

import java.net.URL;

import javax.crypto.*;

/**
 * Created by Felix on 2016-03-04.
 */
public class Downloader extends Service {

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {

        return null;
    }

    public void main(String[] args){
        DownloadFilesTask dlTask = new DownloadFilesTask();
        try {
            dlTask.execute(new URL("http://172.31.212.116/index.html"));
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    private class DownloadFilesTask extends AsyncTask<URL, Integer, Long> {
        protected Long doInBackground(URL... urls) {
            int count = urls.length;
            long totalSize = 0;
            for (int i = 0; i < count; i++) {
                totalSize += Downloader.downloadFile(urls[i]);
                publishProgress((int) ((i / (float) count) * 100));
                // Escape early if cancel() is called
                if (isCancelled()) break;
            }
            return totalSize;
        }

        protected void onProgressUpdate(Integer... progress) {
            // TODO: Om vi ska ha...
        }

        protected void onPostExecute(Long result) {
            //showDialog("Downloaded " + result + " bytes");
        }
    }

    private class UploadTask extends AsyncTask<String, Integer, String> {
        protected String doInBackground(String... strs) {

            return "error";
        }
        protected void onPostExecute(String result) { // result was returned from doInBackground()

        }
    }

    private static long downloadFile(URL url){
        return 0L;
    }
}
