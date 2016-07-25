package com.ncornette.rx.test;


import com.ncornette.rx.test.service.SpamRXService;

import java.text.MessageFormat;
import java.util.List;

import rx.Observable;
import rx.functions.Action0;
import rx.functions.Action1;
import rx.functions.Func1;
import rx.schedulers.TestScheduler;
import rx.subjects.PublishSubject;

/**
 * Created by nic on 11/07/16.
 */
public class MockSpamService implements SpamRXService {

    public static final int MAX_RESULTS = 120;
    private final TestScheduler backgroundTestScheduler;
    private final TestScheduler foregroundTestScheduler;
    private final RxTestSchedulers.Logger logger;
    private int requestCount;

    public MockSpamService(TestScheduler backgroundTestScheduler, TestScheduler foregroundTestScheduler) {
        this(backgroundTestScheduler, foregroundTestScheduler, new RxTestSchedulers.Logger());
    }

    public MockSpamService(TestScheduler backgroundTestScheduler, TestScheduler foregroundTestScheduler, RxTestSchedulers.Logger logger) {
        this.backgroundTestScheduler = backgroundTestScheduler;
        this.foregroundTestScheduler = foregroundTestScheduler;
        this.logger = logger;
    }

    @Override
    public Observable<List<Spam>> searchSpams(String query, final int limit, PublishSubject<Integer> pagePublishSubject) {
        return pagePublishSubject
                .distinctUntilChanged()
                .cache()
                .concatMap(new Func1<Integer, Observable<? extends List<Spam>>>() {
                    @Override
                    public Observable<? extends List<Spam>> call(Integer pageNumber) {
                        if (pageNumber <= 3) {
                            return MockSpamService.this.listOfSpams(limit).subscribeOn(backgroundTestScheduler);
                        } else {
                            return MockSpamService.this.listOfSpams(0).subscribeOn(backgroundTestScheduler);
                        }
                    }
                }).takeWhile(new Func1<List<Spam>, Boolean>() {
                    @Override
                    public Boolean call(List<Spam> spams) {
                        return !spams.isEmpty();
                    }
                })
                .cache()
                .observeOn(foregroundTestScheduler);
    }

    @Override
    public Observable<List<Spam>> latestSpams(int count) {
        Observable<List<Spam>> spams = listOfSpams(count);
        return spams.subscribeOn(backgroundTestScheduler)
                .observeOn(foregroundTestScheduler);
    }

    private Observable<List<Spam>> listOfSpams(final int count) {
        return Observable
                .range(0, count)
                .doOnSubscribe(new Action0() {
                    @Override
                    public void call() {
                        if (count > MAX_RESULTS) {
                            throw new IllegalArgumentException("max results: " + MAX_RESULTS);
                        }
                    }
                })
                .map(new Func1<Integer, Spam>() {
                    @Override
                    public Spam call(Integer i) {
                        return new Spam(MessageFormat.format("{0}", i));
                    }
                })
                .doOnNext(new Action1<Spam>() {
                    @Override
                    public void call(Spam spam) {
                        logger.i("<-- {0}", spam);
                    }
                })
                .toList()
                .doOnNext(new Action1<List<Spam>>() {
                    @Override
                    public void call(List<Spam> spams) {
                        requestCount++;
                    }
                });
    }

    public int getRequestCount() {
        return requestCount;
    }

}
