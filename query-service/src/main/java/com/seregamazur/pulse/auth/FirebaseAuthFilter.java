package com.seregamazur.pulse.auth;

import jakarta.inject.Inject;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;

@Provider
public class FirebaseAuthFilter implements ContainerRequestFilter {

    private static final String APP_CHECK_HEADER_NAME = "X-Firebase-AppCheck";

    @Inject
    FirebaseTokenService tokenService;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String token = ctx.getHeaderString(APP_CHECK_HEADER_NAME);

        if (token == null) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("Missing App Check token")
                .build());
        }

        try {
            tokenService.verify(token);
        } catch (Exception e) {
            ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .entity("Invalid App Check token")
                .build());
        }
    }
}

