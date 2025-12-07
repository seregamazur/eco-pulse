package com.seregamazur.pulse.exception;

import org.eclipse.microprofile.faulttolerance.exceptions.CircuitBreakerOpenException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class CircuitBreakerExceptionHandler implements ExceptionMapper<CircuitBreakerOpenException> {

    @Override
    public Response toResponse(CircuitBreakerOpenException exception) {
        return Response.status(Response.Status.SERVICE_UNAVAILABLE)
            .entity(new ErrorDto("Circuit Breaker is open"))
            .build();
    }
}
