package com.seregamazur.pulse.pipeline.daily;

import java.time.LocalDate;
import java.util.concurrent.CompletionStage;

import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

import com.seregamazur.pulse.pipeline.daily.dto.TheGuardianResponse;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;

@RegisterRestClient(baseUri = "https://content.guardianapis.com")
@Produces(MediaType.APPLICATION_JSON)
public interface TheGuardianClient {

    @GET
    @Path("/search")
    CompletionStage<TheGuardianResponse> search(
            @QueryParam("section") String section,
            @QueryParam("from-date") LocalDate fromDate,
            @QueryParam("to-date") LocalDate toDate,
            @QueryParam("api-key") String apiKey,
            @QueryParam("page-size") int pageSize,
            @QueryParam("show-fields") String fields,
            @QueryParam("tag") String tag
    );
}
