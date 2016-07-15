package com.ncornette.rx.test;

import org.junit.Before;
import org.junit.Test;

import rx.functions.Func0;

public class MockSpamServiceTest {

    private MockSpamService testServiceClient;
    private RxTestSchedulers rxTestSchedulers;
    private SpamServiceAssertions spamServiceTest;

    @Before
    public void setUp() throws Exception {

        rxTestSchedulers = RxTestSchedulers.builder()
                .subscriber(new MockSpamService.LogSpamSubscriber())
                .backgroundEventsCount(new Func0<Integer>() {
                    @Override
                    public Integer call() {
                        return testServiceClient.getRequestCount();
                    }
                })
                .build();

        testServiceClient = new MockSpamService(
                rxTestSchedulers.testBackgroundScheduler(),
                rxTestSchedulers.testForegroundScheduler()
        );

        spamServiceTest = new SpamServiceAssertions(testServiceClient, rxTestSchedulers);
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
