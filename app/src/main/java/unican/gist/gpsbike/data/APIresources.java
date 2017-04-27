package unican.gist.gpsbike.data;

import retrofit2.http.GET;
import retrofit2.http.Headers;
import rx.Observable;
import unican.gist.gpsbike.model.VersionData;

/**
 * Created by andres on 25/04/2017.
 */

public interface APIresources {
    @Headers("Content-Type: application/json")
    @GET("dgt/version")
    Observable<VersionData> getVersion();
}
