package com.example.swaggeragent.service.tool;

import com.example.swaggeragent.model.domain.DynamicTool;
import com.example.swaggeragent.model.OpenApiEndpoint;
import com.example.swaggeragent.service.parser.OpenApiParserService;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Serviço para carregamento sob demanda (lazy loading) das ferramentas dinâmicas.
 * <p>
 * Esta classe implementa o padrão de carregamento preguiçoso (lazy loading) para
 * as ferramentas dinâmicas, carregando-as apenas quando necessário. Isso melhora
 * o tempo de inicialização da aplicação e reduz o uso de memória inicial.
 * <p>
 * <b>Características:</b>
 * <ul>
 *   <li><b>Carregamento sob demanda:</b> As ferramentas são carregadas apenas na primeira chamada</li>
 *   <li><b>Cache implícito:</b> Após o primeiro carregamento, as ferramentas ficam em memória</li>
 *   <li><b>Integração com parser:</b> Utiliza o {@link OpenApiParserService} para processar especificações</li>
 *   <li><b>Geração dinâmica:</b> Usa o {@link DynamicToolGeneratorService} para criar as ferramentas</li>
 * </ul>
 * <p>
 * <b>Fluxo de carregamento:</b>
 * <ol>
 *   <li>Primeira chamada ao método {@link #loadTools()}</li>
 *   <li>Parser processa todas as especificações OpenAPI</li>
 *   <li>Gerador cria as ferramentas dinâmicas</li>
 *   <li>Resultado é armazenado em cache implícito</li>
 *   <li>Chamadas subsequentes retornam o cache</li>
 * </ol>
 * <p>
 * <b>Uso:</b> Este serviço é injetado no {@link ChatService} para fornecer
 * as ferramentas apenas quando necessário, melhorando a performance de inicialização.
 */
@Service
public class LazyToolLoader {
    
    private final OpenApiParserService openApiParserService;
    private final DynamicToolGeneratorService dynamicToolGeneratorService;

    /**
     * Constrói uma nova instância do carregador preguiçoso.
     *
     * @param openApiParserService        o serviço para parsing de especificações OpenAPI
     * @param dynamicToolGeneratorService o serviço para geração de ferramentas dinâmicas
     */
    public LazyToolLoader(OpenApiParserService openApiParserService, DynamicToolGeneratorService dynamicToolGeneratorService) {
        this.openApiParserService = openApiParserService;
        this.dynamicToolGeneratorService = dynamicToolGeneratorService;
    }

    /**
     * Carrega todas as ferramentas dinâmicas de forma preguiçosa.
     * <p>
     * Este método implementa o carregamento sob demanda das ferramentas:
     * <ul>
     *   <li><b>Primeira execução:</b> Processa todas as especificações OpenAPI e gera as ferramentas</li>
     *   <li><b>Execuções subsequentes:</b> Retorna as ferramentas já carregadas (cache implícito)</li>
     * </ul>
     * <p>
     * <b>Processo de carregamento:</b>
     * <ol>
     *   <li>O parser lê e processa todos os arquivos OpenAPI do diretório configurado</li>
     *   <li>Para cada endpoint encontrado, uma ferramenta dinâmica é gerada</li>
     *   <li>As ferramentas são configuradas com funções de execução apropriadas</li>
     *   <li>O resultado é retornado e mantido em memória</li>
     * </ol>
     * <p>
     * <b>Performance:</b> O carregamento inicial pode levar alguns segundos,
     * mas as chamadas subsequentes são instantâneas.
     *
     * @return uma lista de todas as ferramentas dinâmicas carregadas
     */
    @Lazy
    public List<DynamicTool> loadTools() {
        // Processa todas as especificações OpenAPI para extrair endpoints
        List<OpenApiEndpoint> endpoints = openApiParserService.parseAllOpenApiFiles();
        
        // Gera ferramentas dinâmicas a partir dos endpoints encontrados
        return dynamicToolGeneratorService.generateToolsFromEndpoints(endpoints);
    }
} 