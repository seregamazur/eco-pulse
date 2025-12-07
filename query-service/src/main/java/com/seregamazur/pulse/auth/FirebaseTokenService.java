package com.seregamazur.pulse.auth;

import java.net.URL;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;

import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FirebaseTokenService {

    @ConfigProperty(name = "firebase.project.number")
    private String projectNumber;

    @ConfigProperty(name = "firebase.web.appId")
    private String appId;

    @ConfigProperty(name = "firebase.appcheck.jwks")
    private String jwksUrl;

    public void verify(String token) {
        if (token == null || token.isBlank()) {
            throw new RuntimeException("Missing App Check token");
        }

        try {
            JwkProvider jwkProvider = new JwkProviderBuilder(new URL(jwksUrl))
                .timeouts(5_000, 5_000)
                .cached(10, 6, TimeUnit.HOURS)
                .rateLimited(10, 1, TimeUnit.MINUTES)
                .build();

            DecodedJWT jwt = JWT.decode(token);
            Jwk jwk = jwkProvider.get(jwt.getKeyId());

            Algorithm alg = Algorithm.RSA256((RSAPublicKey) jwk.getPublicKey(), null);
            JWTVerifier verifier = JWT.require(alg).withIssuer(jwt.getIssuer()).build();
            verifier.verify(token);

            if (!"RS256".equals(jwt.getAlgorithm())) {
                throw new RuntimeException("Invalid algorithm");
            }

            String expectedIssuer = "https://firebaseappcheck.googleapis.com/" + projectNumber;
            if (!expectedIssuer.equals(jwt.getIssuer())) {
                throw new RuntimeException("Invalid issuer");
            }

            if (jwt.getExpiresAt().before(new Date())) {
                throw new RuntimeException("App Check token expired");
            }

            String expectedAudience = "projects/" + projectNumber;
            if (!jwt.getAudience().contains(expectedAudience)) {
                throw new RuntimeException("Invalid audience");
            }

            if (!jwt.getSubject().equals(appId)) {
                throw new RuntimeException("Invalid subject (appId mismatch)");
            }

        } catch (JwkException ex) {
            throw new RuntimeException("Error verifying App Check token", ex);
        } catch (Exception ex) {
            throw new RuntimeException("Invalid App Check token", ex);
        }
    }
}
