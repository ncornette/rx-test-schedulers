package com.ncornette.rx.test;

import com.ncornette.rx.test.service.SpamRXService;
import com.ncornette.rx.test.service.SpamRXService.Spam;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import rx.Observable;
import rx.functions.Action1;
import rx.subjects.PublishSubject;

import static org.assertj.core.api.Assertions.assertThat;

public abstract class SpamServiceAssertions {

    private SpamRXService testServiceClient;
    private RxTestSchedulers rxTestSchedulers;

    @Before
    public void setUp() throws Exception {
        this.testServiceClient = testClient();
        this.rxTestSchedulers = rxTestSchedulers();
    }

    protected abstract RxTestSchedulers rxTestSchedulers();

    protected abstract SpamRXService testClient();

    @Test
    public void assertSimpleCall() throws Exception {

        testServiceClient.latestSpams(6)
                .doOnNext(new Action1<List<Spam>>() {
                    @Override
                    public void call(List<Spam> spams) {
                        assertThat(spams).hasSize(6);
                    }
                })
                .subscribe(rxTestSchedulers.testSubscriber());

        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(1);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(1);

        rxTestSchedulers.testSubscriber().assertCompleted();

        testServiceClient.latestSpams(12)
                .doOnNext(new Action1<List<Spam>>() {
                    @Override
                    public void call(List<Spam> spams) {
                        assertThat(spams).hasSize(12);
                    }
                })
                .subscribe(rxTestSchedulers.newTestSubscriber());

        rxTestSchedulers.testSubscriber().assertNotCompleted();

        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(1);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(1);

        rxTestSchedulers.testSubscriber().assertCompleted();
    }

    @Test
    public void assertDistinctPageUntilChanged() throws Exception {

        PublishSubject<Integer> pagePublishSubject = PublishSubject.create();

        testServiceClient.searchSpams("eggs", 6, pagePublishSubject).subscribe(rxTestSchedulers.testSubscriber());

        rxTestSchedulers.triggerBackgroundRequests();

        pagePublishSubject.onNext(1);
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(1);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(1);

        pagePublishSubject.onNext(1); // SAME PAGE
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(0);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(0);

        pagePublishSubject.onNext(2);
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(1);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(1);

        pagePublishSubject.onNext(2); // SAME PAGE
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(0);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(0);

        pagePublishSubject.onNext(2); // SAME PAGE
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(0);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(0);

        pagePublishSubject.onNext(3);
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(1);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(1);
    }

    @Test
    public void assertLoad3Pages() throws Exception {

        PublishSubject<Integer> pagePublishSubject = PublishSubject.create();

        testServiceClient.searchSpams("eggs", 6, pagePublishSubject).subscribe(rxTestSchedulers.testSubscriber());

        System.out.println(rxTestSchedulers.triggerBackgroundRequests());

        pagePublishSubject.onNext(1);
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(1);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(1);

        pagePublishSubject.onNext(2);
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(1);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(1);

        pagePublishSubject.onNext(3);
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(1);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(1);
    }

    @Test
    public void assertCompletesAtFirstEmptyList() throws Exception {

        PublishSubject<Integer> pagePublishSubject = PublishSubject.create();

        testServiceClient.searchSpams("eggs", 6, pagePublishSubject).subscribe(rxTestSchedulers.testSubscriber());

        rxTestSchedulers.triggerBackgroundRequests();

        pagePublishSubject.onNext(1);
        pagePublishSubject.onNext(2);
        pagePublishSubject.onNext(3);
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(3);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(3);

        rxTestSchedulers.testSubscriber().assertNotCompleted();

        pagePublishSubject.onNext(4);
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(1);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(0);

        rxTestSchedulers.testSubscriber().assertCompleted();

        pagePublishSubject.onNext(5);
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(0);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(0);

        pagePublishSubject.onNext(6);
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(0);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(0);
    }

    @Test
    public void assertCachedResult() throws Exception {

        PublishSubject<Integer> pagePublishSubject = PublishSubject.create();

        Observable<List<Spam>> listObservable = testServiceClient.searchSpams("eggs", 6, pagePublishSubject);
        listObservable.subscribe(rxTestSchedulers.testSubscriber());

        rxTestSchedulers.triggerBackgroundRequests();

        pagePublishSubject.onNext(1);
        pagePublishSubject.onNext(2);
        pagePublishSubject.onNext(3);
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(3);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(3);

        rxTestSchedulers.testSubscriber().assertNotCompleted();

        listObservable.subscribe(rxTestSchedulers.testSubscriber());

        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(0);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(3);

    }

    @Test
    public void assertCachedResultAfterComplete() throws Exception {

        PublishSubject<Integer> pagePublishSubject = PublishSubject.create();

        Observable<List<Spam>> listObservable = testServiceClient.searchSpams("eggs", 6, pagePublishSubject);
        listObservable.subscribe(rxTestSchedulers.testSubscriber());

        rxTestSchedulers.triggerBackgroundRequests();

        pagePublishSubject.onNext(1);
        pagePublishSubject.onNext(2);
        pagePublishSubject.onNext(3);
        pagePublishSubject.onNext(4);
        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(4);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(3);

        rxTestSchedulers.testSubscriber().assertCompleted();

        listObservable.subscribe(rxTestSchedulers.newTestSubscriber());

        assertThat(rxTestSchedulers.triggerBackgroundRequests()).isEqualTo(0);
        assertThat(rxTestSchedulers.triggerForegroundEvents()).isEqualTo(3);
    }

}
