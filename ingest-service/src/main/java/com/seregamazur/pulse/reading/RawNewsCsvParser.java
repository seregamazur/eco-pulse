package com.seregamazur.pulse.reading;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import com.seregamazur.pulse.reading.model.RawNews;

import jakarta.enterprise.context.ApplicationScoped;
import lombok.experimental.UtilityClass;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

@ApplicationScoped
@UtilityClass
public class RawNewsCsvParser {

    public static List<RawNews> parseFromS3Object(String s3ObjectName, ResponseBytes<GetObjectResponse> b) {
        InputStreamReader reader = new InputStreamReader(b.asInputStream(), StandardCharsets.UTF_8);
        CSVParser parser = null;
        try {
            parser = new CSVParser(reader, CSVFormat.DEFAULT
                .withFirstRecordAsHeader()
                .withIgnoreHeaderCase()
                .withTrim());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        List<RawNews> news = new ArrayList<>();
        for (CSVRecord record : parser) {
            String title = record.get("Title");
            String text = record.get("Article Text");
            LocalDate date = LocalDate.parse(s3ObjectName.replace(".csv", ""));
            news.add(new RawNews(title, text, date));
        }
        return news;
    }
}
