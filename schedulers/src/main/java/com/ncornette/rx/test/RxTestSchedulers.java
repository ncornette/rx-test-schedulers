package com.ncornette.rx.test;

import java.text.MessageFormat;
import java.util.concurrent.TimeUnit;


import rx.Subscriber;
import rx.functions.Func0;
import rx.observers.TestSubscriber;
import rx.schedulers.TestScheduler;

/**
 * Created by nic on 11/07/16.
 */
public class RxTestSchedulers {

    private final TestScheduler foregroundScheduler;
    private final TestScheduler backgroundScheduler;
    private final TestSubscriberWrapper subscriberWrapper;
    private final Func0<Integer> backgroundEventsCount;

    public RxTestSchedulers() {
        this(builder().build().newBuilder());
    }

    private RxTestSchedulers(Builder builder) {
        foregroundScheduler = builder.foregroundScheduler;
        backgroundScheduler = builder.backgroundScheduler;
        subscriberWrapper = builder.subscriberWrapper;
        backgroundEventsCount = builder.backgroundEventsCount;
    }

    public static Builder builder() {
        return new Builder();
    }

    public Builder newBuilder() {
        return newBuilder(this);
    }

    public static Builder newBuilder(RxTestSchedulers copy) {
        if (copy == null) return builder();
        Builder builder = new Builder();
        builder.foregroundScheduler = copy.foregroundScheduler;
        builder.backgroundScheduler = copy.backgroundScheduler;
        builder.subscriberWrapper = copy.subscriberWrapper;
        builder.backgroundEventsCount = copy.backgroundEventsCount;
        builder.delegateSubscriber = copy.subscriberWrapper.delegateSubscriber;
        return builder;
    }

    private void triggerBackgroundActions(String title) {
        System.out.println("┌┄┄┄┄┄┄┄┄┄┄");
        System.out.println("┆ BACKGROUND: " + title);
        System.out.println("├┄┄┄┄┄┄┄┄┄┄");
        System.out.println("┆  ");
        long startTime = System.nanoTime();
        testBackgroundScheduler().triggerActions();
        long elapsed = System.nanoTime() - startTime;
        testBackgroundScheduler().advanceTimeBy(elapsed, TimeUnit.NANOSECONDS);
        System.out.println("┆  ");
        System.out.println(MessageFormat.format("┆  current: {0}ms", TimeUnit.NANOSECONDS.toMillis(elapsed)));
        System.out.println(MessageFormat.format("┆  total  : {0}ms", testBackgroundScheduler().now()));
        System.out.println("└┄┄┄┄┄┄┄┄┄┄");
    }

    private void triggerForegroundActions(String title) {
        System.out.println("┏━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("┃ MAIN:  " + title);
        System.out.println("┣━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("┃   ");
        long startTime = System.nanoTime();
        testForegroundScheduler().triggerActions();
        long elapsed = System.nanoTime() - startTime;
        testForegroundScheduler().advanceTimeBy(elapsed, TimeUnit.NANOSECONDS);
        System.out.println("┃   ");
        System.out.println(MessageFormat.format("┃  current: {0}ms", TimeUnit.NANOSECONDS.toMillis(elapsed)));
        System.out.println(MessageFormat.format("┃  total  : {0}ms", testForegroundScheduler().now()));
        System.out.println("┃   ");
        System.out.println("┗━━━━━━━━━━━━━━━━━━━━━━");
    }

    protected int triggerForegroundEvents(String s) throws OnErrorEventsException {
        int beforeCount = getForegroundEventsCount();
        int beforeErrorCount = testSubscriber().getOnErrorEvents().size();

        triggerForegroundActions(s);

        int afterErrorCount = testSubscriber().getOnErrorEvents().size();
        assertNoOnErrorEvents(beforeErrorCount, afterErrorCount);
        int afterCount = getForegroundEventsCount();
        return afterCount - beforeCount;
    }

    protected int triggerBackgroundRequests(String s) throws OnErrorEventsException {
        int beforeCount = getBackgroundEventsCount();
        int beforeErrorCount = testSubscriber().getOnErrorEvents().size();

        triggerBackgroundActions(s);

        int afterErrorCount = testSubscriber().getOnErrorEvents().size();
        assertNoOnErrorEvents(beforeErrorCount, afterErrorCount);
        int afterCount = getBackgroundEventsCount();
        return afterCount - beforeCount;
    }

    private void assertNoOnErrorEvents(int beforeErrorCount, int afterErrorCount) throws OnErrorEventsException {
        if (afterErrorCount > beforeErrorCount) {
            throw new OnErrorEventsException(
                    MessageFormat.format("{0} more Errors", afterErrorCount > beforeErrorCount),
                    testSubscriber().getOnErrorEvents().get(beforeErrorCount));
        }
    }

    protected int getBackgroundEventsCount() {
        return backgroundEventsCount.call();
    }

    protected int getForegroundEventsCount() {
        return testSubscriber().getOnNextEvents().size();
    }

    public int triggerBackgroundRequests() throws OnErrorEventsException {
        return triggerBackgroundRequests("");
    }

    public int triggerForegroundEvents() throws OnErrorEventsException {
        return triggerForegroundEvents("");
    }

    protected Throwable triggerForegroundEventsWithError() {
        try {
            triggerForegroundEvents("");
        } catch (OnErrorEventsException e) {
            return e.getCause();
        }
        return null;
    }

    protected Throwable triggerBackgroundRequestsWithError() {
        try {
            triggerBackgroundRequests("");
        } catch (OnErrorEventsException e) {
            return e.getCause();
        }
        return null;
    }

    //protected void resetTestSubscriber() {
    //    subscriberWrapper = new TestSubscriber<>(new LogSubscriber<>());
    //}

    public TestScheduler testForegroundScheduler() {
        return foregroundScheduler;
    }

    public TestScheduler testBackgroundScheduler() {
        return backgroundScheduler;
    }

    public TestSubscriber<Object> testSubscriber() {
        return subscriberWrapper.testSubscriber;
    }

    public TestSubscriber<? super Object> newTestSubscriber() {
        return subscriberWrapper.subscriber();
    }

    public TestSubscriber<? super Object> newTestSubscriber(Subscriber<? super Object> subscriber) {
        return subscriberWrapper.subscriber(subscriber);
    }

    protected class OnErrorEventsException extends Exception {
        public OnErrorEventsException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static final class Builder {
        private TestScheduler foregroundScheduler;
        private TestScheduler backgroundScheduler;
        private TestSubscriberWrapper subscriberWrapper;
        private Func0<Integer> backgroundEventsCount;
        private Subscriber<Object> delegateSubscriber;

        private Builder() {
        }

        public Builder foregroundScheduler(TestScheduler val) {
            foregroundScheduler = val;
            return this;
        }

        public Builder backgroundScheduler(TestScheduler val) {
            backgroundScheduler = val;
            return this;
        }

        public Builder subscriber(Subscriber<? super Object> val) {
            delegateSubscriber = val;
            return this;
        }

        public Builder backgroundEventsCount(Func0<Integer> val) {
            backgroundEventsCount = val;
            return this;
        }

        public RxTestSchedulers build() {
            if (backgroundEventsCount == null) {
                throw new IllegalArgumentException("attribute [backgroundEventsCount] expected not to be null.");
            }

            if (foregroundScheduler == null) {
                foregroundScheduler = new TestScheduler();
            }

            if (backgroundScheduler == null) {
                backgroundScheduler = new TestScheduler();
            }

            subscriberWrapper = new TestSubscriberWrapper(delegateSubscriber);

            return new RxTestSchedulers(this);
        }
    }

    private static class TestSubscriberWrapper {

        private Subscriber<Object> delegateSubscriber;
        private TestSubscriber<Object> testSubscriber;

        public TestSubscriberWrapper(Subscriber<Object> subscriber) {
            this.delegateSubscriber = subscriber;
            testSubscriber = getObjectTestSubscriber(delegateSubscriber);
        }

        public TestSubscriber<Object> subscriber() {
            testSubscriber = getObjectTestSubscriber(delegateSubscriber);
            return testSubscriber;
        }
        public TestSubscriber<Object> subscriber(Subscriber<Object> subscriber) {
            testSubscriber = getObjectTestSubscriber(subscriber);
            return testSubscriber;
        }

        private TestSubscriber<Object> getObjectTestSubscriber(Subscriber<Object> subscriber) {
            if (subscriber != null) {
                return TestSubscriber.create(subscriber);
            } else {
                return TestSubscriber.create();
            }
        }
    }
}