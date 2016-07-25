package com.ncornette.rx.test.service;

import java.util.List;

import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.Subject;

/**
 * Created by nic on 14/07/16.
 */
public interface SpamRXService {

    Observable<List<Spam>> searchSpams(String query, int limit, PublishSubject<Integer> pagePublishSubject);

    Observable<List<Spam>> latestSpams(int count);


    class Spam {
        private String foo;

        public Spam(String foo) {
            this.foo = foo;
        }

        public String foo() {
            return foo;
        }

        @Override
        public String toString() {
            return "Spam{" +
                    "foo='" + foo + '\'' +
                    '}';
        }
    }

}
