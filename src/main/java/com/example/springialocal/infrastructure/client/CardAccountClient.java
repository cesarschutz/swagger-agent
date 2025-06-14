package com.example.springialocal.infrastructure.client;

import com.example.springialocal.domain.api.dto.ApiDtoResponse;
import com.example.springialocal.infrastructure.client.config.CustomFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;


@FeignClient(name = "cardAccountApi", url = "${app.card-account-api.base-url}", configuration = CustomFeignConfig.class)
public interface CardAccountClient {
    @GetMapping(value = "${app.card-account-api.get-card-by-uuid}")
    ApiDtoResponse getCardByUuid(@PathVariable("uuid") String uuid);

    @PostMapping(value = "${app.card-account-api.block-card-by-uuid}")
    ApiDtoResponse blockCardByUuid(@PathVariable("uuid") String uuid);
}