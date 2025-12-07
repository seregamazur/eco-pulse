package com.seregamazur.pulse.exception;

import io.smallrye.faulttolerance.api.RateLimitException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class RateLimitExceptionHandler implements ExceptionMapper<RateLimitException> {

    @Override
    public Response toResponse(RateLimitException exception) {
        return Response.status(Response.Status.TOO_MANY_REQUESTS)
            .entity(new ErrorDto("Rate Limit Exceeded"))
            .build();
    }
}
