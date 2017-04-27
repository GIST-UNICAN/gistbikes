package unican.gist.gpsbike.data;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory;
import retrofit2.converter.gson.GsonConverterFactory;
import rx.Observable;
import unican.gist.gpsbike.model.VersionData;

/**
 * Created by andres on 25/04/2017.
 */

public class DataRepository implements DataRepositoryInterface {
    APIresources resources;
    private static final DataRepository INSTANCE = new DataRepository();

    public static DataRepository getInstance() {
        return INSTANCE;
    }

    private DataRepository() {

        HttpLoggingInterceptor interceptor = new HttpLoggingInterceptor();
        interceptor.setLevel(HttpLoggingInterceptor.Level.BODY);
        interceptor.setLevel(HttpLoggingInterceptor.Level.HEADERS);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(60, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)

                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(interceptor).build();

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("http://193.144.208.142:8080/")
                .addConverterFactory(GsonConverterFactory.create())
                .addCallAdapterFactory(RxJavaCallAdapterFactory.create())
                .client(okHttpClient)
                .build();
        resources = retrofit.create(APIresources.class);
    }

    @Override
    public Observable<VersionData> getVersion() {
        return resources.getVersion();
    }
}
