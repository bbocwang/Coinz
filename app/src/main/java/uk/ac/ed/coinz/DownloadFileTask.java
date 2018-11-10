package uk.ac.ed.coinz;

import android.os.AsyncTask;
import android.support.annotation.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public class DownloadFileTask extends AsyncTask<String, Void, String> {
    @Override
    protected  String doInBackground(String... urls){
        try{
            return loadFileFromNetwork(urls[0]);
        }catch (IOException e){
            return "Unable to load data. Please check your network connection";
        }
    }

    private String loadFileFromNetwork(String urlString) throws IOException{
        return  readStream(downloadUrl(new URL(urlString)));
    }

    private InputStream downloadUrl(URL url) throws IOException{
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        conn.setReadTimeout(10000);
        conn.setConnectTimeout(15000);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        conn.connect();
        return conn.getInputStream();
    }

    @NonNull
    private String readStream(InputStream stream)throws IOException{
        Scanner s = new Scanner(stream).useDelimiter("\\A");
        String result = s.hasNext() ? s.next() : "";
        return result;
    }

    @Override
    protected void onPostExecute(String result){
        super.onPostExecute(result);
        DownloadCompleteRunner.downloadComplete(result);
    }
}
