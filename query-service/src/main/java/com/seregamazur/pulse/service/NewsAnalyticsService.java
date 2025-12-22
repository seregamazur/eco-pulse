package com.seregamazur.pulse.service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.opensearch.client.json.JsonData;
import org.opensearch.client.opensearch._types.aggregations.Aggregate;
import org.opensearch.client.opensearch._types.aggregations.Buckets;
import org.opensearch.client.opensearch._types.aggregations.FieldDateMath;
import org.opensearch.client.opensearch._types.aggregations.MultiBucketAggregateBase;
import org.opensearch.client.opensearch._types.aggregations.StringTermsBucket;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.opensearch.client.opensearch.core.SearchResponse;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.seregamazur.pulse.dto.Period;
import com.seregamazur.pulse.dto.geo.CountryNewsData;
import com.seregamazur.pulse.dto.geo.GeoMapChart;
import com.seregamazur.pulse.dto.sentiment.AverageSentiment;
import com.seregamazur.pulse.dto.today.TodayNews;
import com.seregamazur.pulse.dto.tone.Tone;
import com.seregamazur.pulse.dto.topics.Topic;
import com.seregamazur.pulse.dto.topics.TopicsChart;
import com.seregamazur.pulse.dto.topics.TopicsOverTime;
import com.seregamazur.pulse.search.SearchExecutor;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class NewsAnalyticsService {

    @Inject
    private SearchExecutor searchExecutor;
    private static final String INDEX_NAME = "news";

    private static final String AGG_BY_DATE = "by_date";
    private static final String AGG_SENTIMENT_DIST = "sentiment_dist";
    private static final String AGG_AVG_SCORE = "avg_score";
    private static final String AGG_BY_COUNTRY = "by_country";
    private static final String AGG_TOPICS_BY_DATE = "topics_by_date";

    public List<AverageSentiment> sentimentTrendsOverTime(LocalDate start, LocalDate end, Period period) {
        SearchRequest req = buildBaseSearch(start, end)
            .aggregations(AGG_BY_DATE, a -> a
                .dateHistogram(d -> d.field("date").calendarInterval(period.interval())
                    //if no results for date, we will return date + empty result instead of no data at all
                    .minDocCount(0)
                    .extendedBounds(bounds -> bounds
                        .min(FieldDateMath.of(m -> m.expr(start.toString())))
                        .max(FieldDateMath.of(m -> m.expr(end.toString())))
                    ))
                .aggregations(AGG_AVG_SCORE, ag -> ag.avg(c -> c.field("sentiment_score")))
                .aggregations(AGG_SENTIMENT_DIST, s -> s.terms(v -> v.field("sentiment_label")))
            )
            .build();

        return searchExecutor.exec(req).aggregations()
            .get(AGG_BY_DATE)
            .dateHistogram()
            .buckets().array().stream()
            .map(bucket -> {
                var distMap = AggHelper.extractTermCounts(bucket.aggregations());
                double avg = AggHelper.extractAvg(bucket.aggregations());

                return new AverageSentiment(
                    parseDateKey(bucket.keyAsString()),
                    avg,
                    distMap.getOrDefault("positive", 0L),
                    distMap.getOrDefault("negative", 0L),
                    distMap.getOrDefault("neutral", 0L)
                );
            })
            .toList();
    }

    public TopicsChart topTopicsOvertime(LocalDate start, LocalDate end, Period period) {
        SearchRequest req = buildBaseSearch(start, end)
            .aggregations(AGG_BY_DATE, a -> a.dateHistogram(d -> d
                .field("date")
                .calendarInterval(period.interval())
                //if no results for date, we will return date + empty result instead of no data at all
                .minDocCount(0)
                .extendedBounds(bounds -> bounds
                    .min(FieldDateMath.of(m -> m.expr(start.toString())))
                    .max(FieldDateMath.of(m -> m.expr(end.toString())))
                ))
            .aggregations(AGG_TOPICS_BY_DATE, sub -> sub.terms(v -> v.field("topics"))))
            .build();

        SearchResponse<ObjectNode> resp = searchExecutor.exec(req);
        var aggs = resp.aggregations();

        Map<LocalDate, List<TopicsOverTime>> result = Optional.ofNullable(aggs.get(AGG_BY_DATE).dateHistogram())
            .map(MultiBucketAggregateBase::buckets)
            .map(Buckets::array)
            .map(arr -> arr.stream().collect(Collectors.toMap(
                dates -> parseDateKey(dates.keyAsString()),
                dates -> {
                    if (dates.aggregations().isEmpty()) {
                        return List.of(new TopicsOverTime(null, 0L));
                    }
                    return dates.aggregations().get(AGG_TOPICS_BY_DATE)
                        .sterms().buckets().array()
                        .stream()
                        .map(bucket -> {
                            long count = bucket.docCount();
                            Topic topic = Topic.valueOf(bucket.key());
                            return new TopicsOverTime(topic.getRaw(), count);
                        }).toList();
                })))
            .orElse(Collections.emptyMap());
        return new TopicsChart(result);
    }

    public List<TopicsOverTime> topTopics(LocalDate start, LocalDate end, Period period) {
        SearchRequest req = buildBaseSearch(start, end)
            .aggregations(AGG_TOPICS_BY_DATE, sub -> sub.terms(v -> v.field("topics")))
            .build();

        SearchResponse<ObjectNode> resp = searchExecutor.exec(req);
        var aggs = resp.aggregations();

        return Optional.ofNullable(aggs.get(AGG_TOPICS_BY_DATE))
            .map(Aggregate::sterms)
            .map(MultiBucketAggregateBase::buckets)
            .map(Buckets::array)
            .map(arr -> arr.stream()
                .map(bucket -> {
                    long count = bucket.docCount();
                    Topic topic = Topic.valueOf(bucket.key());
                    return new TopicsOverTime(topic.getRaw(), count);
                }).toList()).orElse(Collections.emptyList());
    }

    public GeoMapChart geoMap(LocalDate start, LocalDate end) {
        SearchRequest req = buildBaseSearch(start, end)
            .aggregations(AGG_BY_COUNTRY, a -> a
                .terms(t -> t.field("country").size(200))
                .aggregations(AGG_AVG_SCORE, ag -> ag.avg(c -> c.field("sentiment_score")))
                .aggregations(AGG_SENTIMENT_DIST, sd -> sd.terms(t -> t.field("sentiment_label")))
            )
            .build();

        Map<String, CountryNewsData> map = AggHelper.extractStringBuckets(searchExecutor.exec(req).aggregations(), AGG_BY_COUNTRY)
            .stream()
            .collect(Collectors.toMap(
                StringTermsBucket::key,
                bucket -> {
                    var distMap = AggHelper.extractTermCounts(bucket.aggregations());
                    double avg = AggHelper.extractAvg(bucket.aggregations());

                    return new CountryNewsData(
                        bucket.docCount(),
                        distMap.getOrDefault("positive", 0L),
                        distMap.getOrDefault("negative", 0L),
                        distMap.getOrDefault("neutral", 0L),
                        avg
                    );
                }
            ));

        return new GeoMapChart(map);
    }

    public Long articlesCount(LocalDate start, LocalDate end) {
        return searchExecutor.exec(buildBaseSearch(start, end).build()).hits().total().value();
    }

    public Long countriesCount(LocalDate start, LocalDate end) {
        SearchRequest req = buildBaseSearch(start, end)
            .aggregations("count_unique", c -> c.cardinality(v -> v.field("country")))
            .build();

        return searchExecutor.exec(req).aggregations().get("count_unique").cardinality().value();
    }

    public Double globalSentiment(LocalDate start, LocalDate end) {
        SearchRequest req = buildBaseSearch(start, end)
            .aggregations(AGG_AVG_SCORE, c -> c.avg(v -> v.field("sentiment_score")))
            .build();
        return AggHelper.extractAvg(searchExecutor.exec(req).aggregations());
    }

    public Map<Tone, Long> toneDistribution(LocalDate start, LocalDate end) {
        SearchRequest req = buildBaseSearch(start, end)
            .aggregations("tones", c -> c.terms(v -> v.field("tone")))
            .build();

        return AggHelper.extractStringBuckets(searchExecutor.exec(req).aggregations(), "tones").stream()
            .collect(Collectors.toMap(
                b -> safeParseTone(b.key()),
                StringTermsBucket::docCount
            ));
    }

    public List<TodayNews> todayNews() {
        LocalDate todayDate = LocalDate.now();

        SearchRequest req = new SearchRequest.Builder()
            .index(INDEX_NAME)
            .size(0)
            .query(q -> q.range(r -> r
                .field("ingest_date")
                .gte(JsonData.of(todayDate.toString()))
                .lte(JsonData.of(todayDate.toString()))
            ))
            .size(10)
            .build();

        return searchExecutor.exec(req).hits().hits().stream()
            .map(hit -> {
                var json = hit.source();

                String title = json.path("title").asText( "No Title Available");
                String body = json.path("summary").asText("No Summary Available");
                String label = json.path("sentiment_label").asText("No Sentiment Label Available");

                return new TodayNews(title, body, label);
            })
            .collect(Collectors.toList());
    }

    private SearchRequest.Builder buildBaseSearch(LocalDate start, LocalDate end) {
        return new SearchRequest.Builder()
            .index(INDEX_NAME)
            .size(0)
            .query(q -> q.range(r -> r
                .field("date")
                .gte(JsonData.of(start.toString()))
                .lte(JsonData.of(end.toString()))
            ));
    }

    private LocalDate parseDateKey(String dateString) {
        return Instant.parse(dateString).atZone(ZoneOffset.UTC).toLocalDate();
    }

    private Tone safeParseTone(String key) {
        try {
            return Tone.valueOf(key.toUpperCase());
        } catch (IllegalArgumentException e) {
            return Tone.DESCRIPTIVE;
        }
    }

    private static class AggHelper {

        static Map<String, Long> extractTermCounts(Map<String, Aggregate> aggs) {
            if (!aggs.containsKey(NewsAnalyticsService.AGG_SENTIMENT_DIST)) return Collections.emptyMap();

            try {
                return aggs.get(NewsAnalyticsService.AGG_SENTIMENT_DIST).sterms().buckets().array().stream()
                    .collect(Collectors.toMap(StringTermsBucket::key, StringTermsBucket::docCount));
            } catch (Exception e) {
                return Collections.emptyMap();
            }
        }

        static double extractAvg(Map<String, Aggregate> aggs) {
            if (!aggs.containsKey(NewsAnalyticsService.AGG_AVG_SCORE)) return 0.0;
            try {
                double val = aggs.get(NewsAnalyticsService.AGG_AVG_SCORE).avg().value();
                return Double.isNaN(val) ? 0.0 : val;
            } catch (Exception e) {
                return 0.0;
            }
        }

        static List<StringTermsBucket> extractStringBuckets(Map<String, Aggregate> aggs, String name) {
            if (!aggs.containsKey(name)) return Collections.emptyList();
            try {
                return aggs.get(name).sterms().buckets().array();
            } catch (Exception e) {
                return Collections.emptyList();
            }
        }
    }
}