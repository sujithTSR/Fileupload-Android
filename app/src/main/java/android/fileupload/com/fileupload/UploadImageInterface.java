package android.fileupload.com.fileupload;

/**
 * Created by ichigo on 5/6/17.
 */
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;
public interface UploadImageInterface {
    @Multipart
    @POST("/pages/create/")

    Call<UploadObject> uploadFile(@Part MultipartBody.Part file, @Part("name") RequestBody name);
}