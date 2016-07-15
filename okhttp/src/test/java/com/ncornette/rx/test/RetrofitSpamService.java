package com.ncornette.rx.test;

import com.ncornette.rx.test.service.SpamRXService.Spam;

import java.util.List;

import retrofit2.http.GET;
import retrofit2.http.Query;
import rx.Observable;

/**
 * Created by nic on 14/07/16.
 */
public interface RetrofitSpamService {

    @GET("/")
    Observable<List<Spam>> searchSpams(
            @Query("query") String query,
            @Query("limit") int limit,
            @Query("page") int page
    );

    @GET("/")
    Observable<List<Spam>> latestSpams(
            @Query("count") int count
    );
}
