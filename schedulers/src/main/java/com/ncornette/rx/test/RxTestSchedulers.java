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
    private final Logger logger;

    public RxTestSchedulers() {
        this(builder().build().newBuilder());
    }

    private RxTestSchedulers(Builder builder) {
        foregroundScheduler = builder.foregroundScheduler;
        backgroundScheduler = builder.backgroundScheduler;
        subscriberWrapper = builder.subscriberWrapper;
        backgroundEventsCount = builder.backgroundEventsCount;
        logger = builder.logger;
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
        builder.logger = copy.logger;
        return builder;
    }

    private void triggerBackgroundActions(String title) {
        logger.d("┌┄┄┄┄┄┄┄┄┄┄");
        if (title != null && !title.isEmpty()) {
            logger.v("┆ " + title);
            logger.v("├┄┄┄┄┄┄┄┄┄┄");
        }
        logger.i("┆  ");
        long startTime = System.nanoTime();
        testBackgroundScheduler().triggerActions();
        long elapsed = System.nanoTime() - startTime;
        testBackgroundScheduler().advanceTimeBy(elapsed, TimeUnit.NANOSECONDS);
        logger.i("┆  ");
        logger.i("┆ Background: ");
        logger.i("┆  current: {0}ms", TimeUnit.NANOSECONDS.toMillis(elapsed));
        logger.i("┆  total  : {0}ms", testBackgroundScheduler().now());
        logger.d("└┄┄┄┄┄┄┄┄┄┄");
    }

    private void triggerForegroundActions(String title) {
        logger.d("┏━━━━━━━━━━━━━━━━━━━━━━");
        if (title != null && !title.isEmpty()) {
            logger.v("┃ " + title);
            logger.v("┣━━━━━━━━━━━━━━━━━━━━━━");
        }
        logger.i("┃   ");
        long startTime = System.nanoTime();
        testForegroundScheduler().triggerActions();
        long elapsed = System.nanoTime() - startTime;
        testForegroundScheduler().advanceTimeBy(elapsed, TimeUnit.NANOSECONDS);
        logger.i("┃   ");
        logger.i("┃ Main: ");
        logger.i("┃  current: {0}ms", TimeUnit.NANOSECONDS.toMillis(elapsed));
        logger.i("┃  total  : {0}ms", testForegroundScheduler().now());
        logger.d("┗━━━━━━━━━━━━━━━━━━━━━━");
    }

    public int triggerForegroundEvents(String s) throws OnErrorEventsException {
        int beforeCount = foregroundEventsCount();
        int beforeErrorCount = testSubscriber().getOnErrorEvents().size();

        triggerForegroundActions(s);

        int afterErrorCount = testSubscriber().getOnErrorEvents().size();
        assertNoOnErrorEvents(beforeErrorCount, afterErrorCount);
        int afterCount = foregroundEventsCount();
        return afterCount - beforeCount;
    }

    public int triggerBackgroundRequests(String s) throws OnErrorEventsException {
        int beforeCount = backgroundEventsCount();
        int beforeErrorCount = testSubscriber().getOnErrorEvents().size();

        triggerBackgroundActions(s);

        int afterErrorCount = testSubscriber().getOnErrorEvents().size();
        assertNoOnErrorEvents(beforeErrorCount, afterErrorCount);
        int afterCount = backgroundEventsCount();
        return afterCount - beforeCount;
    }

    private void assertNoOnErrorEvents(int beforeErrorCount, int afterErrorCount) throws OnErrorEventsException {
        if (afterErrorCount > beforeErrorCount) {
            throw new OnErrorEventsException(
                    MessageFormat.format("{0} more Errors", afterErrorCount > beforeErrorCount),
                    testSubscriber().getOnErrorEvents().get(beforeErrorCount));
        }
    }

    protected int backgroundEventsCount() {
        return backgroundEventsCount.call();
    }

    protected int foregroundEventsCount() {
        return testSubscriber().getOnNextEvents().size();
    }

    public int triggerBackgroundRequests() throws OnErrorEventsException {
        return triggerBackgroundRequests("");
    }

    public int triggerForegroundEvents() throws OnErrorEventsException {
        return triggerForegroundEvents("");
    }

    public Throwable triggerForegroundEventsWithError() {
        try {
            triggerForegroundEvents("");
        } catch (OnErrorEventsException e) {
            return e.getCause();
        }
        return null;
    }

    public Throwable triggerBackgroundRequestsWithError() {
        try {
            triggerBackgroundRequests("");
        } catch (OnErrorEventsException e) {
            return e.getCause();
        }
        return null;
    }

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

    public TestSubscriber<Object> newTestSubscriber(Subscriber<Object> subscriber) {
        return subscriberWrapper.subscriber(subscriber);
    }

    public Logger logger() {
        return logger;
    }

    public static class OnErrorEventsException extends Exception {
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
        private Logger logger;

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

        public Builder subscriber(Subscriber<Object> val) {
            delegateSubscriber = val;
            return this;
        }

        public Builder logger(Logger val) {
            logger = val;
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

            if (logger == null) {
                logger = new Logger();
            }

            subscriberWrapper = new TestSubscriberWrapper(delegateSubscriber, logger);

            return new RxTestSchedulers(this);
        }
    }

    private static class TestSubscriberWrapper {

        private final Logger logger;
        private Subscriber<Object> delegateSubscriber;
        private TestSubscriber<Object> testSubscriber;

        public TestSubscriberWrapper(Subscriber<Object> subscriber, Logger logger) {
            this.logger = logger;
            testSubscriber = getObjectTestSubscriber(subscriber);
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
            return TestSubscriber.create(new LogSubscriber(logger, subscriber));
        }
    }

    /**
     * Created by nic on 16/07/16.
     */

    public static class Logger {

        private int currentLevelValue;
        public Logger() {
            this.currentLevelValue = Level.DEBUG.value;
        }
        public void v(String s) {
            log(Level.VERBOSE, s);
        }
        public void v(String s, Object... o) {
            log(Level.VERBOSE, s, o);
        }
        public void d(String s) {
            log(Level.DEBUG, s);
        }
        public void d(String s, Object... o) {
            log(Level.DEBUG, s, o);
        }
        public void i(String s) {
            log(Level.INFO, s);
        }
        public void i(String s, Object... o) {
            log(Level.INFO, s, o);
        }
        public void e(String s) {
            log(Level.ERROR, s);
        }
        public void e(String s, Object... o) {
            log(Level.ERROR, s, o);
        }

        private void log(Level level, String s) {
            if(currentLevelValue >= level.value) {
                if (level.value > Level.ERROR.value) {
                    System.out.println(s);
                } else {
                    System.err.println(s);
                }
            }
        }

        private void log(Level level, String s, Object... o) {
            if(currentLevelValue >= level.value) {
                if (level.value > Level.ERROR.value) {
                    System.out.println(MessageFormat.format(s, o));
                } else {
                    System.err.println(MessageFormat.format(s, o));
                }
            }
        }

        public void level(Level currentLevel) {
            this.currentLevelValue = currentLevel.value;
        }

        public enum Level {
            ERROR(0),
            INFO(1),
            DEBUG(2),
            VERBOSE(3);

            private int value;

            Level(int value) {
                this.value = value;
            }
        }
    }
}