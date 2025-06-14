package com.example.springialocal.domain.tool;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import com.example.springialocal.domain.api.dto.ApiDtoResponse;
import com.example.springialocal.infrastructure.client.InvoiceClient;

@Component
public class InvoiceApiTools {
    private static final Logger logger = LoggerFactory.getLogger(CardAccountApiTools.class);

    private final InvoiceClient invoiceClient;

    public InvoiceApiTools(InvoiceClient invoiceClient) {
        this.invoiceClient = invoiceClient;
    }

    @Tool(
        name = "get_invoice_by_uuid",
        description = "Consulta dados de uma fatura através do seu UUID.")
    public ApiDtoResponse getInvoiceByUuid(String uuid) {
        logger.info("---> tool:get_invoice_by_uuid invoked with UUID: {}", uuid);
        return invoiceClient.getInvoiceByUuid(uuid);
    }
}