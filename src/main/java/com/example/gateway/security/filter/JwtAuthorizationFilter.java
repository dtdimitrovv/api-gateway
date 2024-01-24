package com.example.gateway.security.filter;

import com.example.gateway.constant.AuthConstant;
import com.example.gateway.security.JwtGenerator;
import com.example.gateway.service.RedisService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import javax.ws.rs.core.HttpHeaders;
import java.util.List;
import java.util.Optional;

import static com.example.gateway.constant.AuthConstant.EMPTY_STRING;
import static com.example.gateway.constant.ClaimConstant.USER_ID_CLAIM;
import static com.example.gateway.constant.ErrorConstant.BLACKLISTED_TOKEN_MESSAGE;
import static com.example.gateway.constant.ErrorConstant.INVALID_TOKEN_MESSAGE;
import static com.example.gateway.constant.HeaderConstant.X_USER_ID_HEADER_NAME;

@Component
public class JwtAuthorizationFilter extends AbstractGatewayFilterFactory {

    private final RedisService redisService;
    private final JwtGenerator jwtGenerator;
    private final List<String> admittedUrls;

    public JwtAuthorizationFilter(RedisService redisService,
                                  JwtGenerator jwtGenerator,
                                  @Value("${admitted-urls}") List<String> admittedUrls) {
        this.redisService = redisService;
        this.jwtGenerator = jwtGenerator;
        this.admittedUrls = admittedUrls;
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
                if (!this.admittedUrls.contains(url)) {
                    return Mono.empty();
                }

                return chain.filter(exchange);
            }

            var token = header
                    .get(0)
                    .replace(AuthConstant.TOKEN_PREFIX, EMPTY_STRING);

            if (this.redisService.isBlackListed(token)) {
                throw new IllegalAccessError(BLACKLISTED_TOKEN_MESSAGE);
            }

            try {
                var id = Optional.ofNullable(this.redisService.getById(token))
                        .orElseGet(() -> {
                                    var decodedJwt = this.jwtGenerator.decodeToken(token);
                                    var idClaim = Long.parseLong(decodedJwt.getClaim(USER_ID_CLAIM).toString());
                                    this.redisService.cache(token, idClaim);

                                    return idClaim;
                                }
                        );

                var modifiedRequest = exchange
                        .getRequest()
                        .mutate()
                        .header(X_USER_ID_HEADER_NAME, String.valueOf(id))
                        .build();

                return chain.filter(
                        exchange
                                .mutate()
                                .request(modifiedRequest)
                                .build()
                );
            } catch (Exception ex) {
                this.redisService.blackList(token);

                throw new Error(INVALID_TOKEN_MESSAGE);
            }
        };
    }
}
