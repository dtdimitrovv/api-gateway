package com.example.gateway.security.filter;

import com.example.gateway.exception.UserNotAuthorizedException;
import com.example.gateway.security.AuthConstants;
import com.example.gateway.security.JwtGenerator;
import com.example.gateway.service.RedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;

import javax.ws.rs.core.HttpHeaders;

@Component
public class JWTAuthorizationFilter extends AbstractGatewayFilterFactory {

    private final RedisService redisService;
    private final JwtGenerator jwtGenerator;
    private final String loginUrl;
    private final String registerUrl;
    private final String fitnessDataUrl;

    public JWTAuthorizationFilter(RedisService redisService,
                                  JwtGenerator jwtGenerator,
                                  @Value("${admitted-urls.login}") String loginUrl,
                                  @Value("${admitted-urls.registration}") String registrationUrl,
                                  @Value("${admitted-urls.fitness-data}") String fitnessDataUrl) {
        this.redisService = redisService;
        this.jwtGenerator = jwtGenerator;
        this.loginUrl = loginUrl;
        this.registerUrl = registrationUrl;
        this.fitnessDataUrl = fitnessDataUrl;
    }

    @Override
    public GatewayFilter apply(Object config) {
        return (exchange, chain) -> {
            var url = exchange
                    .getRequest()
                    .getPath()
                    .toString();

            var header = exchange
                    .getRequest()
                    .getHeaders()
                    .get(HttpHeaders.AUTHORIZATION);

            if (header == null) {
                if (url.equals(this.loginUrl) || url.equals(this.registerUrl) || url.equals(this.fitnessDataUrl)) {
//                    return chain.filter(exchange);
                } else {
                    try {
                        throw new UserNotAuthorizedException();
                    } catch (UserNotAuthorizedException e) {
                        e.printStackTrace();
                    }
                }
                return chain.filter(exchange);
            }

            var token = header
                    .get(0)
                    .replace(AuthConstants.TOKEN_PREFIX, "");

            if (this.redisService.isBlackListed(token)) {
                throw new IllegalAccessError("Token is blacklisted!");
            }

            try {
                var id = this.redisService.getById(token);
                if (id == null) {
                    var decodedJWT = this.jwtGenerator.decodeToken(token);
                    id = Long.parseLong(decodedJWT.getClaim("User-Id").toString());
                    this.redisService.cache(token, id);
                }
                var modifiedRequest = exchange
                        .getRequest()
                        .mutate()
                        .header("X-User-Id", String.valueOf(id))
                        .build();

                return chain.filter(
                        exchange
                                .mutate()
                                .request(modifiedRequest)
                                .build()
                );
            } catch (Exception ex) {
                this.redisService.blackList(token);
                throw new Error("Token invalid");
            }

//            var req = exchange.getRequest();
//
//            var request = req
//                    .mutate()
//                    .header("X-Authorization-Id", String.valueOf(1))
//                    .build();
//            return chain.filter(exchange.mutate().request(request).build());
//            return chain.filter(exchange);
        };
    }
}
