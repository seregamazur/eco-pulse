package com.seregamazur.pulse.controller;

import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.faulttolerance.CircuitBreaker;

import com.seregamazur.pulse.dto.DataForPeriodRequest;
import com.seregamazur.pulse.dto.geo.GeoMapChart;
import com.seregamazur.pulse.dto.sentiment.AverageSentiment;
import com.seregamazur.pulse.dto.today.TodayNews;
import com.seregamazur.pulse.dto.tone.Tone;
import com.seregamazur.pulse.dto.topics.TopicsChart;
import com.seregamazur.pulse.dto.topics.TopicsOverTime;
import com.seregamazur.pulse.service.NewsAnalyticsService;

import io.smallrye.faulttolerance.api.RateLimit;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;

@Path("/api/charts")
@RateLimit
@CircuitBreaker
public class DashboardController {

    @Inject
    NewsAnalyticsService chartService;

    @GET
    @Path("sentiment")
    public List<AverageSentiment> sentiment(@Valid @BeanParam DataForPeriodRequest request) {
        return chartService.sentimentTrendsOverTime(request.from(), request.to(), request.period());
    }

    @GET
    @Path("topics-overtime")
    public TopicsChart topTopicsOvertime(@Valid @BeanParam DataForPeriodRequest request) {
        return chartService.topTopicsOvertime(request.from(), request.to(), request.period());
    }

    @GET
    @Path("topics")
    public List<TopicsOverTime> topTopics(@Valid @BeanParam DataForPeriodRequest request) {
        return chartService.topTopics(request.from(), request.to(), request.period());
    }

    @GET
    @Path("geo")
    public GeoMapChart geo(@Valid @BeanParam DataForPeriodRequest request) {
        return chartService.geoMap(request.from(), request.to());
    }

    @GET
    @Path("articles")
    public Long articlesRead(@Valid @BeanParam DataForPeriodRequest request) {
        return chartService.articlesCount(request.from(), request.to());
    }

    @GET
    @Path("countries")
    public Long countriesMentioned(@Valid @BeanParam DataForPeriodRequest request) {
        return chartService.countriesCount(request.from(), request.to());
    }

    @GET
    @Path("global")
    public Double globalSentiment(@Valid @BeanParam DataForPeriodRequest request) {
        return chartService.globalSentiment(request.from(), request.to());
    }

    @GET
    @Path("tone")
    public Map<Tone, Long> toneDistribution(@Valid @BeanParam DataForPeriodRequest request) {
        return chartService.toneDistribution(request.from(), request.to());
    }

    @GET
    @Path("today")
    public List<TodayNews> todayNews() {
        return chartService.todayNews();
    }

}
