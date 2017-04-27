package unican.gist.gpsbike.model;

import rx.Observable;
import unican.gist.gpsbike.data.DataRepository;
import unican.gist.gpsbike.data.DataRepositoryInterface;

/**
 * Created by andres on 25/04/2017.
 */

public class GetVersionUseCase extends UseCase {
    @Override
    protected Observable buildUseCaseObservable() {
        DataRepositoryInterface repository = DataRepository.getInstance();
        return repository.getVersion();
    }
}
