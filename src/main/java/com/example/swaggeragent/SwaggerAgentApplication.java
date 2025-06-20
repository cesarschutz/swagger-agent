/*
 * Copyright (c) 2024 Cesar Schutz and Swagger Agent contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 */

package com.example.swaggeragent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Classe principal que inicia a aplicação Spring Boot.
 * <p>
 * A anotação {@code @SpringBootApplication} habilita a autoconfiguração do Spring Boot,
 * a varredura de componentes no pacote atual e a configuração de beans.
 */
@SpringBootApplication
@EnableAsync
public class SwaggerAgentApplication {

    /**
     * O ponto de entrada principal para a aplicação.
     *
     * @param args argumentos de linha de comando passados durante a inicialização.
     */
    public static void main(String[] args) {
        SpringApplication.run(SwaggerAgentApplication.class, args);
    }
}