package unican.gist.gpsbike.data;

import rx.Observable;
import unican.gist.gpsbike.model.VersionData;

/**
 * Created by andres on 25/04/2017.
 */

public interface DataRepositoryInterface {
    Observable<VersionData> getVersion();
}
