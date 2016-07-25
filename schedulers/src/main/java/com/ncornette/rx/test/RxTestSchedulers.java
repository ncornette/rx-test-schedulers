package com.ncornette.rx.test;

import java.io.PrintStream;
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
    private final Func0<Integer> backgroundEventsCount;
    private final Logger logger;

    private Subscriber<? super Object> delegateSubscriber;
    private TestSubscriber<? super Object> testSubscriber;

    public RxTestSchedulers() {
        this(builder().build().newBuilder());
    }

    private RxTestSchedulers(Builder builder) {
        foregroundScheduler = builder.foregroundScheduler;
        backgroundScheduler = builder.backgroundScheduler;
        delegateSubscriber = builder.delegateSubscriber;
        backgroundEventsCount = builder.backgroundEventsCount;
        logger = builder.logger;
        testSubscriber = TestSubscriber.create(new LogSubscriber<>(logger, delegateSubscriber));
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
        builder.delegateSubscriber = copy.delegateSubscriber;
        builder.backgroundEventsCount = copy.backgroundEventsCount;
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
            throw new OnErrorEventsException("Unexpected error event",
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

    public TestSubscriber<? super Object> testSubscriber() {
        return testSubscriber;
    }

    public TestSubscriber<? super Object> newTestSubscriber() {
        return newTestSubscriber(null);
    }

    public TestSubscriber<? super Object> newTestSubscriber(Subscriber<Object> subscriber) {
        delegateSubscriber = subscriber;
        this.testSubscriber = TestSubscriber.create(LogSubscriber.create(logger, subscriber));
        return this.testSubscriber;
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
        private Func0<Integer> backgroundEventsCount;
        private Subscriber<? super Object> delegateSubscriber;
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

            return new RxTestSchedulers(this);
        }
    }

    /**
     * Created by nic on 16/07/16.
     */

    public static class Logger {

        private int currentLevelValue;
        private final PrintStream out;
        private final PrintStream err;

        public Logger() {
            this(Level.DEBUG);
        }

        public Logger(Level level) {
            this(level, System.out);
        }

        public Logger(Level level, PrintStream out) {
            this.currentLevelValue = level.value;
            this.out = out;
            this.err = System.err;
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
                    out.println(s);
                } else {
                    err.println(s);
                }
            }
        }

        private void log(Level level, String s, Object... o) {
            if(currentLevelValue >= level.value) {
                if (level.value > Level.ERROR.value) {
                    out.println(MessageFormat.format(s, o));
                } else {
                    err.println(MessageFormat.format(s, o));
                }
            }
        }

        public void level(Level currentLevel) {
            this.currentLevelValue = currentLevel.value;
        }

        public static Logger error() {
            return new RxTestSchedulers.Logger(Level.ERROR);
        }

        public static Logger info() {
            return new RxTestSchedulers.Logger(Level.INFO);
        }

        public static Logger debug() {
            return new RxTestSchedulers.Logger(Level.DEBUG);
        }

        public static Logger verbose() {
            return new RxTestSchedulers.Logger(Level.VERBOSE);
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