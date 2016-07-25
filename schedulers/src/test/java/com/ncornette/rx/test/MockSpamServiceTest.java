package com.ncornette.rx.test;

import com.ncornette.rx.test.RxTestSchedulers.Logger;
import com.ncornette.rx.test.service.SpamRXService;
import com.ncornette.rx.test.service.SpamRXService.Spam;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import rx.Subscriber;
import rx.functions.Action1;
import rx.functions.Func0;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class MockSpamServiceTest extends SpamServiceAssertions {

    private MockSpamService testServiceClient;
    private RxTestSchedulers rxTestSchedulers;

    @Before
    @Override
    public void setUp() throws Exception {

        rxTestSchedulers = RxTestSchedulers.builder()
                .backgroundEventsCount(new Func0<Integer>() {
                    @Override
                    public Integer call() {
                        return testServiceClient.getRequestCount();
                    }
                })
                .logger(Logger.verbose())
                .build();

        testServiceClient = new MockSpamService(
                rxTestSchedulers.testBackgroundScheduler(),
                rxTestSchedulers.testForegroundScheduler(),
                rxTestSchedulers.logger()
        );
        super.setUp();
    }

    @Override
    protected RxTestSchedulers rxTestSchedulers() {
        return rxTestSchedulers;
    }

    @Override
    protected SpamRXService testClient() {
        return testServiceClient;
    }

    @Test
    public void testCustomSubscriber() throws Exception {
        Subscriber mockSubscriber = mock(Subscriber.class);

        testServiceClient.latestSpams(6)
                .doOnNext(new Action1<List<Spam>>() {
                    @Override
                    public void call(List<Spam> spams) {
                        assertThat(spams).hasSize(6);
                    }
                })
                .subscribe(rxTestSchedulers.newTestSubscriber(mockSubscriber));


        assertThat(rxTestSchedulers.triggerBackgroundRequests("Generate 6 Spams")).isEqualTo(1);
        verify(mockSubscriber, never()).onNext(anyObject());
        assertThat(rxTestSchedulers.triggerForegroundEvents("List of 6 Spams")).isEqualTo(1);
        verify(mockSubscriber, times(1)).onNext(anyObject());
        verify(mockSubscriber, times(1)).onCompleted();

        rxTestSchedulers.testSubscriber().assertCompleted();
    }


    @Test
    @Override
    public void assertSimpleCall() throws Exception {
        super.assertSimpleCall();
    }

    @Test
    @Override
    public void assertDistinctPageUntilChanged() throws Exception {
        super.assertDistinctPageUntilChanged();
    }

    @Test
    @Override
    public void assertLoad3Pages() throws Exception {
        super.assertLoad3Pages();
    }

    @Test
    @Override
    public void assertCompletesAtFirstEmptyList() throws Exception {
        super.assertCompletesAtFirstEmptyList();
    }

    @Test
    @Override
    public void assertCachedResult() throws Exception {
        super.assertCachedResult();
    }

    @Test
    @Override
    public void assertCachedResultAfterComplete() throws Exception {
        super.assertCachedResultAfterComplete();
    }

    @Test
    @Override
    public void checkBackgroundError() throws Exception {
        super.checkBackgroundError();
    }

    @Override
    public void checkHandleUnexpectError() throws Exception {
        super.checkHandleUnexpectError();
    }
}
