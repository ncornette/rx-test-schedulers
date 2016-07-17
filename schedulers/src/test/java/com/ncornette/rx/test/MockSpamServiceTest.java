package com.ncornette.rx.test;

import com.ncornette.rx.test.service.SpamRXService.Spam;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import rx.functions.Action1;
import rx.functions.Func0;

import static org.assertj.core.api.Assertions.assertThat;

public class MockSpamServiceTest {

    private MockSpamService testServiceClient;
    private RxTestSchedulers rxTestSchedulers;
    private SpamServiceAssertions spamServiceTest;

    @Before
    public void setUp() throws Exception {

        rxTestSchedulers = RxTestSchedulers.builder()
                .backgroundEventsCount(new Func0<Integer>() {
                    @Override
                    public Integer call() {
                        return testServiceClient.getRequestCount();
                    }
                })
                .build();

        rxTestSchedulers.logger().level(RxTestSchedulers.Logger.Level.VERBOSE);

        testServiceClient = new MockSpamService(
                rxTestSchedulers.testBackgroundScheduler(),
                rxTestSchedulers.testForegroundScheduler()
        );

        spamServiceTest = new SpamServiceAssertions(testServiceClient, rxTestSchedulers);
    }

    @Test
    public void testCustom() throws Exception {
        testServiceClient.latestSpams(6)
                .doOnNext(new Action1<List<Spam>>() {
                    @Override
                    public void call(List<Spam> spams) {
                        assertThat(spams).hasSize(6);
                    }
                })
                .subscribe(rxTestSchedulers.newTestSubscriber());

        assertThat(rxTestSchedulers.triggerBackgroundRequests("Generate 6 Spams")).isEqualTo(1);
        assertThat(rxTestSchedulers.triggerForegroundEvents("List of 6 Spams")).isEqualTo(1);

        rxTestSchedulers.testSubscriber().assertCompleted();
    }


    @Test
    public void test() throws Exception {
        spamServiceTest.assertSimpleCall();
    }

    @Test
    public void test_DISTINCT_PAGE_UNTIL_CHANGED() throws Exception {
        spamServiceTest.assertDistinctPageUntilChanged();
    }

    @Test
    public void test_LOAD_3_PAGES() throws Exception {
        spamServiceTest.assertLoad3Pages();
    }

    @Test
    public void test_COMPLETES_AT_FIRST_EMPTYLIST() throws Exception {
        spamServiceTest.assertCompletesAtFirstEmptyList();
    }

    @Test
    public void test_CACHED_RESULT() throws Exception {
        spamServiceTest.assertCachedResult();
    }

    @Test
    public void test_CACHED_RESULT_COMPLETED() throws Exception {
        spamServiceTest.assertCachedResultAfterComplete();
    }

}
