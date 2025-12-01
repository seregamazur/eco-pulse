package com.seregamazur.pulse.exception;

import java.io.IOException;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class OpenSearchExceptionHandler implements ExceptionMapper<IOException> {

    @Override
    public Response toResponse(IOException exception) {
        return Response.status(Response.Status.BAD_GATEWAY)
            .entity(new ErrorDto(exception.getMessage()))
            .build();
    }
}
