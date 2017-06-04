package android.fileupload.com.fileupload;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.internal.http.multipart.FilePart;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

public class MainActivity extends AppCompatActivity implements View.OnClickListener{
    private String BASE_URL = "http://192.168.43.91:9292/pages/create/";
    private static final int PICK_FILE_REQUEST = 1;
    private static final String TAG = MainActivity.class.getSimpleName();
    private String selectedFilePath;
    ProgressDialog dialog;
    PowerManager.WakeLock wakeLock;
    Button bUpload;
    TextView tvFileName;
    ImageView ivAttachment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        isStoragePermissionGranted();
        ivAttachment = (ImageView) findViewById(R.id.ivAttachment);
        bUpload = (Button) findViewById(R.id.b_upload);
        tvFileName = (TextView) findViewById(R.id.tv_file_name);
        ivAttachment.setOnClickListener(this);
        bUpload.setOnClickListener(this);

    }

    public  boolean isStoragePermissionGranted() {

        if (Build.VERSION.SDK_INT >= 23) {
            requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 1);
            requestPermissions(new String[]{Manifest.permission.CAMERA},1);
            requestPermissions(new String[]{Manifest.permission.ACCESS_NETWORK_STATE},1);
            requestPermissions(new String[]{Manifest.permission.INTERNET},1);
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        if (view == ivAttachment){
            showFileChooser();
        }

        if (view == bUpload){
            //on upload button Click
            if (selectedFilePath != null){
                dialog = ProgressDialog.show(MainActivity.this, "","Uploading File ...", true);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try{
                            uploadFile(selectedFilePath);
                        }
                        catch (OutOfMemoryError o){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(MainActivity.this, "Insufficient Memory!", Toast.LENGTH_SHORT).show();
                                }
                            });
                            dialog.dismiss();
                        }
                    }
                }).start();
            }
            else{
                Toast.makeText(this, "Please choose a file first", Toast.LENGTH_SHORT);
            }

        }

    }

    private void showFileChooser(){
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        galleryIntent.setType("file/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(galleryIntent, "Choose File to Upload .. "), PICK_FILE_REQUEST);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == PICK_FILE_REQUEST){
                if (data == null){
                    return ;
                }

                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
                wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
                wakeLock.acquire();
                // ================================================
                Uri uri = data.getData();
                selectedFilePath = getPath(uri);
                // ================================================
//                selectedFilePath = FilePath.
//                        uri.getPath();
                Log.i(TAG, "Selected File Path:" + selectedFilePath);

                if (selectedFilePath != null && !selectedFilePath.equals("")) {
                    tvFileName.setText(selectedFilePath);
                } else {
                    Toast.makeText(this, "Cannot upload file to server", Toast.LENGTH_SHORT).show();
                }

            }
        }
    }

    public String getPath(Uri contentUri) {
        String[] proj = { MediaStore.Images.Media.DATA };
        Log.v("Check", contentUri.toString());
        CursorLoader loader = new CursorLoader(getApplicationContext(), contentUri, proj, null, null, null);
        Cursor cursor = loader.loadInBackground();
        int column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        cursor.moveToFirst();
        String result = cursor.getString(column_index);
        cursor.close();
        return result;
    }

    public int uploadFile(final String selectedFilePath){
        int serverResponseCode = 0;
        HttpURLConnection connection;
        DataOutputStream dataOutputStream;
        String lineEnd = "\r\n";
        String twoHyphens = "--";
        String boundary = "*****";


        int bytesRead, bytesAvailable, bufferSize;
        byte[] buffer;
        int maxBufferSize = 1 * 1024 * 1024;
        File selectedFile = new File(selectedFilePath);
        String[] parts = selectedFilePath.split("/");
        final String fileName = parts[parts.length - 1];

        if (!selectedFile.isFile()){
            dialog.dismiss();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    tvFileName.setText("Source File Doesn't exist : " + selectedFilePath);
                }
            });
            return 0;
        }
        else{
            try{
                Log.v("Log", BASE_URL);
                Log.v("File", selectedFilePath);
                FileInputStream fileInputStream = new FileInputStream(selectedFile);
                URL url = new URL(BASE_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setDoInput(true);
                connection.setDoOutput(true);
                connection.setUseCaches(false);
                connection.setRequestMethod("POST");
                connection.setRequestProperty("Connection", "Keep-Alive");
                connection.setRequestProperty("ENCTYPE", "multipart/form-data");
                connection.setRequestProperty(
                        "Content-Type", "multipart/form-data;boundary=" + boundary);
                connection.setRequestProperty("uploaded_file",selectedFilePath);

                // creating new data output strem
                dataOutputStream = new DataOutputStream(connection.getOutputStream());
                dataOutputStream.writeBytes("Content-Disposition: form-data; name=\"uploaded_file\";filename=\""
                        + selectedFilePath + "\"" + lineEnd);
                dataOutputStream.writeBytes(lineEnd);
                //returns no. of bytes present in fileInputStream
                bytesAvailable = fileInputStream.available();
                //selecting the buffer size as minimum of available bytes or 1 MB
                bufferSize = Math.min(bytesAvailable, maxBufferSize);
                //setting the buffer as byte array of size of bufferSize
                buffer = new byte[bufferSize];
                //reads bytes from FileInputStream(from 0th index of buffer to buffersize)
                bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                //loop repeats till bytesRead = -1, i.e., no bytes are left to read
                while (bytesRead > 0) {

                    try {

                        //write the bytes read from inputstream
                        dataOutputStream.write(buffer, 0, bufferSize);
                    } catch (OutOfMemoryError e) {
                        Toast.makeText(MainActivity.this, "Insufficient Memory!", Toast.LENGTH_SHORT).show();
                    }
                    bytesAvailable = fileInputStream.available();
                    bufferSize = Math.min(bytesAvailable, maxBufferSize);
                    bytesRead = fileInputStream.read(buffer, 0, bufferSize);
                }

                dataOutputStream.writeBytes(lineEnd);
                dataOutputStream.writeBytes(twoHyphens + boundary + twoHyphens + lineEnd);

                try{
                    serverResponseCode = connection.getResponseCode();
                }catch (OutOfMemoryError e){
                    Toast.makeText(MainActivity.this, "Memory Insufficient!", Toast.LENGTH_SHORT).show();
                }
                String serverResponseMessage = connection.getResponseMessage();

                Log.i(TAG, "Server Response is: " + serverResponseMessage + ": " + serverResponseCode);

                //response code of 200 indicates the server status OK
                if (serverResponseCode == 200) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            tvFileName.setText("File Upload completed.\n\n You can see the uploaded file here: \n\n" + "http://coderefer.com/extras/uploads/" + fileName);
                        }
                    });
                }

                //closing the input and output streams
                fileInputStream.close();
                dataOutputStream.flush();
                dataOutputStream.close();

                if (wakeLock.isHeld()) {

                    wakeLock.release();
                }

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            } catch (MalformedURLException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
            dialog.dismiss();
            return serverResponseCode;
        }







    }
}
