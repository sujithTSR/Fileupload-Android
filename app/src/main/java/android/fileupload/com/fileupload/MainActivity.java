package android.fileupload.com.fileupload;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.CursorLoader;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Time;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import pub.devrel.easypermissions.EasyPermissions;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, EasyPermissions.PermissionCallbacks{
//    private String BASE_URL = "https://jfxvvmorvy.localtunnel.me/pages/create/";
    private String BASE_URL = "http://192.168.1.100:9292";

    private static final int PICK_FILE_REQUEST = 1;
    private static final String TAG = MainActivity.class.getSimpleName();
    private String selectedFilePath;
    ProgressDialog dialog;
    PowerManager.WakeLock wakeLock;
    Button bUpload;
    TextView tvFileName;
    ImageView ivAttachment;
    private Uri uri;

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
//                            upload_retrofit();
//                            uploadFile(selectedFilePath);
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        EasyPermissions.onRequestPermissionsResult(requestCode, permissions, grantResults, MainActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK){
            if (requestCode == PICK_FILE_REQUEST){
                if (data == null){
                    return ;
                }
                uri = data.getData();
                Log.v("Here", "upload_retrofit");
                if(EasyPermissions.hasPermissions(this, Manifest.permission.READ_EXTERNAL_STORAGE)){
                    Log.v("Here", "permissions already there");

                    String filePath = getPath(uri);
                    File file = new File(filePath);
                    Log.v("Here", "file "+ file.getName());
                    OkHttpClient client = new OkHttpClient.Builder()
                            .connectTimeout(120, TimeUnit.SECONDS)
                            .retryOnConnectionFailure(true)
                            .readTimeout(120,TimeUnit.SECONDS).build();
                    Log.d(TAG, "Filename " + file.getName());
//                    image/*
//                    multipart/form-data
                    RequestBody mFile = RequestBody.create(MediaType.parse("image/*"), file);
                    MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("file", file.getName(), mFile);
                    RequestBody filename = RequestBody.create(MediaType.parse("text/plain"), file.getName());
                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .client(client)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    Log.v("Here", retrofit.baseUrl().toString());

                    UploadImageInterface uploadImage = retrofit.create(UploadImageInterface.class);
                    Call<UploadObject> fileUpload = uploadImage.uploadFile(fileToUpload, filename);

                    fileUpload.enqueue(new Callback<UploadObject>() {
                        @Override
                        public void onResponse(Call<UploadObject> call, Response<UploadObject> response) {
                            if (response != null){
                                Log.v("response", response.raw().toString());
//                                Toast.makeText(MainActivity.this, "Response " + response.raw().message(), Toast.LENGTH_LONG).show();
//                                Toast.makeText(MainActivity.this, "Success " + response.body().getSuccess(), Toast.LENGTH_LONG).show();
                            }
                        }
                        @Override
                        public void onFailure(Call<UploadObject> call, Throwable t) {
                            Log.d(TAG, "Error " + t.getMessage());
                        }
                    });
                }
                else {
                    EasyPermissions.requestPermissions(this, getString(R.string.app_name), 300, Manifest.permission.READ_EXTERNAL_STORAGE);
                }



//                PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
//                wakeLock = powerManager.newWakeLock(PowerManager.SCREEN_BRIGHT_WAKE_LOCK, TAG);
//                wakeLock.acquire();
//                // ================================================
//                Uri uri = data.getData();
//                selectedFilePath = getPath(uri);
//                // ================================================
////                selectedFilePath = FilePath.
////                        uri.getPath();
//                Log.i(TAG, "Selected File Path:" + selectedFilePath);
//
//                if (selectedFilePath != null && !selectedFilePath.equals("")) {
//                    tvFileName.setText(selectedFilePath);
//                } else {
//                    Toast.makeText(this, "Cannot upload file to server", Toast.LENGTH_SHORT).show();
//                }

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

    public void upload_retrofit(){

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

//            try {
//                String uploadId = UUID.randomUUID().toString();
//                Log.v("Log", BASE_URL);
//
//                //Creating a multi part request
//                MultipartUploadRequest muf = new MultipartUploadRequest(this, uploadId, BASE_URL)
//                        .addFileToUpload(selectedFilePath, "uploaded_file") //Adding file
//                        .addParameter("name", "here") //Adding text parameter to the request
//                        .setNotificationConfig(new UploadNotificationConfig())
//                        .setMaxRetries(2);
//                         //Starting the upload
//                muf.setAutoDeleteFilesAfterSuccessfulUpload(false);
//                muf.setMethod("POST");
//                muf.startUpload();
//
//            } catch (Exception exc) {
//                Toast.makeText(this, exc.getMessage(), Toast.LENGTH_SHORT).show();
//            }


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

                // creating new data output stream
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

    @Override
    public void onPermissionsGranted(int requestCode, List<String> perms) {
        if(uri != null){
            String filePath = getPath(uri);
            File file = new File(filePath);
            RequestBody mFile = RequestBody.create(MediaType.parse("image/*"), file);
            MultipartBody.Part fileToUpload = MultipartBody.Part.createFormData("file", file.getName(), mFile);
            RequestBody filename = RequestBody.create(MediaType.parse("text/plain"), file.getName());
            Retrofit retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
            UploadImageInterface uploadImage = retrofit.create(UploadImageInterface.class);
            Call<UploadObject> fileUpload = uploadImage.uploadFile(fileToUpload, filename);
            fileUpload.enqueue(new Callback<UploadObject>() {
                @Override
                public void onResponse(Call<UploadObject> call, Response<UploadObject> response) {
                    Toast.makeText(MainActivity.this, "Success " + response.message(), Toast.LENGTH_LONG).show();
                    Toast.makeText(MainActivity.this, "Success " + response.body().toString(), Toast.LENGTH_LONG).show();
                }
                @Override
                public void onFailure(Call<UploadObject> call, Throwable t) {
                    Log.d(TAG, "Error " + t.getMessage());
                }
            });
        }
    }

    @Override
    public void onPermissionsDenied(int requestCode, List<String> perms) {
        Log.d(TAG, "Permission has been denied");

    }
}
