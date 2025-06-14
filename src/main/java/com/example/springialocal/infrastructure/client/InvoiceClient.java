package com.example.springialocal.infrastructure.client;

import com.example.springialocal.domain.api.dto.ApiDtoResponse;
import com.example.springialocal.infrastructure.client.config.CustomFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "invoiceApi", url = "${app.invoice-api.base-url}", configuration = CustomFeignConfig.class)
public interface InvoiceClient {
    @GetMapping(value = "${app.invoice-api.get-invoice-by-uuid}")
    ApiDtoResponse getInvoiceByUuid(@PathVariable("uuid") String uuid);
}