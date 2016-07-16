package com.ncornette.rx.test;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import rx.Observable;
import rx.functions.Func0;
import rx.functions.Func1;
import rx.observables.StringObservable;

/**
 * Created by nic on 11/07/16.
 */
public final class RxTestOkHttp {

    private final OkHttpClient okHttpClient;
    private final CountRequestInterceptor countRequestInterceptor;
    private final MockWebServer mockWebServer;
    private final RxTestSchedulers rxTestSchedulers;

    public RxTestOkHttp() {
        this(new Builder().build().newBuilder());
    }

    private RxTestOkHttp(Builder builder) {
        mockWebServer = new MockWebServer();
        countRequestInterceptor = builder.countRequestInterceptor;
        rxTestSchedulers = builder.rxTestSchedulers;
        okHttpClient = builder.okHttpClient;

        try {
            mockWebServer.start();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public Builder newBuilder() {
        return newBuilder(this);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static Builder newBuilder(RxTestOkHttp copy) {
        if (copy == null) return builder();
        Builder builder = new Builder();
        builder.countRequestInterceptor = copy.countRequestInterceptor;
        builder.okHttpClient = copy.okHttpClient;
        builder.rxTestSchedulers = copy.rxTestSchedulers;
        return builder;
    }

    public void enqueueResponseFromFile(final String resourceFilePath) throws FileNotFoundException {
        Observable<String> o = StringObservable
                .join(StringObservable.using(new StringObservable.UnsafeFunc0<Reader>() {
                    @Override
                    public Reader call() throws Exception {
                        return new InputStreamReader(resourceFilePath.getClass()
                                .getResourceAsStream(resourceFilePath));
                    }
                }, new Func1<Reader, Observable<String>>() {
                    @Override
                    public Observable<String> call(Reader reader) {
                        return StringObservable.from(reader);
                    }
                }), "");

        mockWebServer.enqueue(new MockResponse().setBody(o.toBlocking().single()));

    }

    public void enqueueResponse(String s) throws FileNotFoundException {
        mockWebServer.enqueue(new MockResponse().setBody(s));
    }

    public OkHttpClient okHttpClient() {
        return okHttpClient;
    }

    public RxTestSchedulers testSchedulers() {
        return rxTestSchedulers;
    }

    public CountRequestInterceptor countRequestInterceptor() {
        return countRequestInterceptor;
    }

    public MockWebServer mockWebServer() {
        return mockWebServer;
    }

    public static class CountRequestInterceptor implements Interceptor {
        private int requestCount;

        @Override
        public Response intercept(Chain chain) throws IOException {
            requestCount++;
            return chain.proceed(chain.request());
        }

        public int getRequestCount() {
            return requestCount;
        }
    }

    public static final class Builder {
        private CountRequestInterceptor countRequestInterceptor;
        private RxTestSchedulers rxTestSchedulers;
        private OkHttpClient okHttpClient;

        private Builder() {
        }

        public Builder countRequestInterceptor(CountRequestInterceptor val) {
            if (val == null) throw new IllegalArgumentException("[countRequestInterceptor] expected not to be null");
            countRequestInterceptor = val;
            return this;
        }

        public Builder rxTestSchedulers(RxTestSchedulers val) {
            if (val == null) throw new IllegalArgumentException("[rxTestSchedulers] expected not to be null");
            rxTestSchedulers = val;
            return this;
        }

        public Builder okHttpClient(OkHttpClient val) {
            this.okHttpClient = val;
            return this;
        }

        public RxTestOkHttp build() {
            if (countRequestInterceptor == null) countRequestInterceptor = new CountRequestInterceptor();
            if (okHttpClient == null) okHttpClient = new OkHttpClient();

            okHttpClient = okHttpClient.newBuilder()
                    .addInterceptor(countRequestInterceptor)
                    .build();

            rxTestSchedulers = RxTestSchedulers.newBuilder(rxTestSchedulers)
                    .backgroundEventsCount(new Func0<Integer>() {
                        @Override
                        public Integer call() {
                            return countRequestInterceptor.getRequestCount();
                        }
                    })
                    .build();
            return new RxTestOkHttp(this);
        }
    }
}
