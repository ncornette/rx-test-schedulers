package com.ncornette.rx.test;

import com.ncornette.rx.test.service.SpamRXService;

import java.util.List;

import rx.Observable;
import rx.Subscriber;
import rx.functions.Func1;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

/**
 * Created by nic on 11/07/16.
 */
public class RetrofitSpamServiceImpl implements SpamRXService {

    private final TestScheduler backgroundTestScheduler;
    private final TestScheduler foregroundTestScheduler;
    private RetrofitSpamService retrofitService;

    public RetrofitSpamServiceImpl(TestScheduler backgroundTestScheduler,
                                   TestScheduler foregroundTestScheduler,
                                   RetrofitSpamService retrofitService) {
        this.backgroundTestScheduler = backgroundTestScheduler;
        this.foregroundTestScheduler = foregroundTestScheduler;

        this.retrofitService = retrofitService;
    }

    @Override
    public Observable<List<Spam>> searchSpams(final String query, final int limit, PublishSubject<Integer> pagePublishSubject) {
        return pagePublishSubject
                .distinctUntilChanged()
                .cache()
                .concatMap(new Func1<Integer, Observable<? extends List<Spam>>>() {

                    @Override
                    public Observable<? extends List<Spam>> call(Integer pageNumber) {
                        return retrofitService.searchSpams(query, limit, pageNumber)
                                .subscribeOn(backgroundTestScheduler);
                    }
                }).takeWhile(new Func1<List<Spam>, Boolean>() {
                    @Override
                    public Boolean call(List<Spam> spams) {
                        return !spams.isEmpty();
                    }
                })
                .cache()
                .subscribeOn(backgroundTestScheduler)
                .observeOn(foregroundTestScheduler);
    }

    @Override
    public Observable<List<Spam>> latestSpams(int count) {
        return retrofitService.latestSpams(count)
                .subscribeOn(backgroundTestScheduler)
                .observeOn(foregroundTestScheduler);
    }

    public static class LogSpamSubscriber extends Subscriber<Object> {
        @Override
        public void onCompleted() {
            System.out.println("--> onCompleted.");
        }

        @Override
        public void onError(Throwable e) {
            System.out.println("--> onError: " + e);
        }

        @Override
        public void onNext(Object o) {
            System.out.println("--> onNext: " + o);
        }
    }


}
