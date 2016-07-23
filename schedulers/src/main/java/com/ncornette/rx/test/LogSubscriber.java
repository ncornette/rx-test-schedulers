package com.ncornette.rx.test;

import rx.Subscriber;

public class LogSubscriber<T> extends Subscriber<T> {

    private RxTestSchedulers.Logger logger;
    private Subscriber<T> subscriber;

    public LogSubscriber(RxTestSchedulers.Logger logger, Subscriber<T> subscriber) {
        this.logger = logger;
        if (subscriber == null) {
            this.subscriber = new Subscriber<T>() {
                @Override public void onCompleted() {/* NO-OP */}
                @Override public void onError(Throwable e) {/* NO-OP */}
                @Override public void onNext(T o) {/* NO-OP */}
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
        public void onNext(T o) {
            logger.i("--> onNext: " + o);
            subscriber.onNext(o);
        }

    public static <T> Subscriber<T> create(RxTestSchedulers.Logger logger, Subscriber<T> subscriber) {
        return new LogSubscriber<>(logger, subscriber);
    }
}

