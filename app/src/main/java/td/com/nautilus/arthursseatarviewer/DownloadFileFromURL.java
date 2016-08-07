package td.com.nautilus.arthursseatarviewer;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

//Defines DownloadFileFromURL as an asynchronous task
class DownloadFileFromURL extends AsyncTask<String, String, String> {

    String key = null;
    //Constructor takes and stores an inKey to a local key variable
    public DownloadFileFromURL(String inKey) {
        super();
        key = inKey;
    }

    @Override
    //On PreExecute print to console that download has started
    protected void onPreExecute() {
        super.onPreExecute();
        System.out.println("Starting download");
    }

    @Override
    //Downloads file from a URL and stores it to an external storage directory
    //using the key passed in in the constructor
    //This function is called on .execute in MainActivity
    protected String doInBackground(String... f_url) {
        int count;
        try {
            //Get storage location and store as root
            String root = Environment.getExternalStorageDirectory().toString();

            URL url = new URL(f_url[0]);

            //Open URL connection
            URLConnection connection = url.openConnection();
            connection.connect();

            //Get file length
            int lengthOfFile = connection.getContentLength();

            //Read file with 8k buffer
            InputStream input = new BufferedInputStream(url.openStream(), 8192);

            //Output stream to write file
            OutputStream output = new FileOutputStream(root+"/"+key+".png");
            byte data[] = new byte[1024];

            long total = 0;
            while ((count = input.read(data)) != -1) {
                total += count;
                // Write data to file
                output.write(data, 0, count);
            }

            //Flush output
            output.flush();

            //Close streams
            output.close();
            input.close();

        } catch (Exception e) {
            Log.e("Error: ", e.getMessage());
        }
        return null;
    }
    @Override
    //On PostExecute print to console that download has finished
    protected void onPostExecute(String file_url) {
        System.out.println("Downloaded");
    }
}