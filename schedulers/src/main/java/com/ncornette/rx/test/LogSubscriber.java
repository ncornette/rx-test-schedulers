package com.ncornette.rx.test;

import rx.Subscriber;

public class LogSubscriber extends Subscriber<Object> {

    private RxTestSchedulers.Logger logger;
    private Subscriber<Object> subscriber;

    public LogSubscriber(RxTestSchedulers.Logger logger, Subscriber<Object> subscriber) {
        this.logger = logger;
        if (subscriber == null) {
            this.subscriber = new Subscriber<Object>() {
                @Override public void onCompleted() {}
                @Override public void onError(Throwable e) {}
                @Override public void onNext(Object o) {}
            };
        } else {
            this.subscriber = subscriber;
        }
    }

    @Override
        public void onCompleted() {
            logger.i("--> onCompleted.");
            subscriber.onCompleted();
        }

        @Override
        public void onError(Throwable e) {
            logger.i("--> onError: " + e);
            subscriber.onError(e);
        }

        @Override
        public void onNext(Object o) {
            logger.i("--> onNext: " + o);
            subscriber.onNext(o);
        }
    }

