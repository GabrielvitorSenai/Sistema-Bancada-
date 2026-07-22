# Sistema de Gerenciamento da Planta SMART 4.0 — Backend

Painel operacional (Spring Boot) da linha didática 4.0. Concentra a criação e a
gestão de pedidos, o controle do magazine de estoque e da expedição, e a
integração com os CLPs da bancada. Este documento é o **manual de instalação e
operação** do backend, incluindo a Etapa 4 (Confiabilidade e Implantação):
testes automatizados, documentação da API (Swagger/OpenAPI) e go-live.

---

## 1. Pilha de tecnologias

| Camada            | Tecnologia                                            |
|-------------------|-------------------------------------------------------|
| Linguagem         | Java 17                                               |
| Framework         | Spring Boot 3.5.3 (Web, Data JPA, Thymeleaf)          |
| Banco (produção)  | MySQL 8 — banco `dbSmart40`                           |
| Banco (testes)    | H2 em memória / em arquivo                             |
| Testes            | JUnit 5 + Mockito + Spring Test (MockMvc)             |
| Documentação API  | springdoc-openapi (Swagger UI / OpenAPI 3)            |
| Mensageria        | Broker MQTT embutido (Moquette) para supervisão       |
| Build             | Maven (wrapper `mvnw` incluído)                       |

---

## 2. Pré-requisitos

- **JDK 17** (ou superior) instalado e no `PATH`.
- **MySQL 8** ativo e acessível (para execução em produção/desenvolvimento).
- **Maven** — opcional; o projeto traz o wrapper `./mvnw` (Linux/macOS) e
  `mvnw.cmd` (Windows), que não exige Maven instalado na máquina.
- Git para clonar o repositório.

> Os **testes automatizados não precisam de MySQL**: eles usam o banco H2, para
> não poluir o `dbSmart40` de produção.

---

## 3. Instalação passo a passo

### 3.1. Clonar o repositório

```bash
git clone https://github.com/GabrielvitorSenai/Sistema-Bancada-.git
cd "Sistema-Bancada-/Sistema Bancada"
```

### 3.2. Criar o banco de dados (script SQL inicial)

Execute o script [`database/criar-banco.sql`](database/criar-banco.sql) no MySQL
Workbench ou pela linha de comando:

```bash
mysql -u root -p < database/criar-banco.sql
```

O script cria o banco `dbSmart40` com o charset correto. **As tabelas**
(`pedido`, `bloco`, `lamina`, `estoque`, `expedicao`) **são criadas
automaticamente** pelo Hibernate na primeira execução
(`spring.jpa.hibernate.ddl-auto=update`). O script traz ainda uma carga inicial
**opcional** do magazine de estoque (comentada), útil para a demonstração de
go-live — execute-a apenas depois do primeiro start da aplicação.

> A URL de conexão usa `createDatabaseIfNotExist=true`, então a aplicação
> também cria o banco sozinha caso ele não exista. O script garante o
> charset/collation recomendados.

### 3.3. Configurar o acesso ao banco

Há três formas (em ordem de precedência), todas apontando por padrão para
`localhost:3306`, usuário `root`, senha `senai`:

1. **Variáveis de ambiente** (recomendado para implantação) — ver seção 4.
2. **Arquivo externo opcional** `config/db-config.properties` — se existir, seus
   valores sobrescrevem o `application.properties`. O caminho é definido por
   `db.config.path` (padrão `./config/db-config.properties`).
3. **`src/main/resources/application.properties`** — valores padrão do projeto.

### 3.4. Executar a aplicação

```bash
./mvnw spring-boot:run
```

Ou gere o `.jar` e execute:

```bash
./mvnw clean package
java -jar target/painel-linha-didatica-0.0.1-SNAPSHOT.jar
```

### 3.5. Acessar

- Interface web: <http://localhost:8088>
- Documentação da API (Swagger): <http://localhost:8088/swagger-ui.html>

---

## 4. Variáveis de ambiente do Spring Boot

O `application.properties` usa placeholders com valores padrão, então qualquer
variável abaixo sobrescreve a configuração sem editar arquivos:

| Variável                     | Property Spring            | Padrão                                   |
|------------------------------|----------------------------|------------------------------------------|
| `SERVER_PORT`                | `server.port`              | `8088`                                   |
| `SPRING_DATASOURCE_URL`      | `spring.datasource.url`    | `jdbc:mysql://localhost:3306/dbSmart40?...` |
| `SPRING_DATASOURCE_USERNAME` | `spring.datasource.username` | `root`                                 |
| `SPRING_DATASOURCE_PASSWORD` | `spring.datasource.password` | `senai`                                |
| `DB_CONFIG_PATH`             | `db.config.path`           | `./config/db-config.properties`          |

Exemplo (Linux/macOS):

```bash
export SPRING_DATASOURCE_URL="jdbc:mysql://db-prod:3306/dbSmart40?useSSL=false&serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true"
export SPRING_DATASOURCE_USERNAME="app_smart40"
export SPRING_DATASOURCE_PASSWORD="********"
export SERVER_PORT="8080"
./mvnw spring-boot:run
```

Exemplo (Windows PowerShell):

```powershell
$env:SPRING_DATASOURCE_URL="jdbc:mysql://db-prod:3306/dbSmart40?useSSL=false&serverTimezone=America/Sao_Paulo&allowPublicKeyRetrieval=true"
$env:SPRING_DATASOURCE_PASSWORD="********"
.\mvnw.cmd spring-boot:run
```

---

## 5. Documentação da API (Swagger / OpenAPI)

Com a aplicação no ar:

- **Swagger UI:** <http://localhost:8088/swagger-ui.html>
- **Documento OpenAPI (JSON):** <http://localhost:8088/v3/api-docs>

Principais endpoints documentados:

### Pedidos
| Método | Caminho                       | Descrição                                   |
|--------|-------------------------------|---------------------------------------------|
| POST   | `/api/pedidos`                | Cria um pedido (valida as Regras de Ouro)   |
| GET    | `/api/pedidos`                | Lista todos os pedidos                       |
| GET    | `/listar-pedido/{id}`         | Consulta um pedido                           |
| PUT    | `/api/pedidos/{id}`           | Edita um pedido pendente                     |
| PUT    | `/api/pedidos/{id}/status`    | Atualiza o status (pendente/produção/concluído) |
| DELETE | `/api/pedidos/{id}`           | Exclui um pedido pendente                    |

### Estoque e Expedição
| Método | Caminho                         | Descrição                                 |
|--------|---------------------------------|-------------------------------------------|
| GET    | `/estoque/disponibilidade`      | Quantidade de blocos por cor              |
| GET    | `/api/estoque/disponivel`       | Posições de estoque ocupadas              |
| POST   | `/estoque/salvar`               | Atualiza o magazine de estoque            |
| GET    | `/blocos-estoque`               | Cor de cada posição do estoque            |
| GET    | `/pedidos-expedicao`            | Pedidos guardados na expedição            |

### Produção (bancada)
| Método | Caminho                       | Descrição                                   |
|--------|-------------------------------|---------------------------------------------|
| POST   | `/iniciar-pedido`             | Envia o pedido ao CLP e inicia a produção   |
| POST   | `/finalizar-pedido-producao`  | Conclui o pedido e grava na expedição       |

---

## 6. Coleção Postman

Importe [`docs/SistemaBancada.postman_collection.json`](docs/SistemaBancada.postman_collection.json)
no Postman. A coleção traz o fluxo de sucesso (configurar estoque → criar pedido
duplo → produção → conclusão) e os casos de erro das Regras de Ouro (estoque
insuficiente, tampa inválida, 4 lâminas). A variável `baseUrl` já aponta para
`http://localhost:8088` e o `id` do pedido criado é reaproveitado
automaticamente nas requisições seguintes.

---

## 7. Regras de Negócio ("Regras de Ouro")

Validadas na camada de Service (`PedidoService`) antes de qualquer gravação. Um
pedido que viole uma regra é recusado com HTTP `422` (ou `409` em conflitos) e
uma mensagem clara ao operador:

- **Tipo × quantidade de blocos:** simples = 1, duplo = 2, triplo = 3.
- **Cor da tampa:** deve ser 1 (preto), 2 (vermelho) ou 3 (azul).
- **Lâminas por bloco:** no máximo 3.
- **Cores/padrões de lâmina:** cor 0–6, padrão 0–3.
- **Disponibilidade de estoque:** o estoque configurado precisa ter blocos
  suficientes das cores pedidas.
- **Número de pedido único** e **edição/exclusão apenas de pedidos pendentes**.

---

## 8. Testes automatizados (Etapa 4 — QA)

Rode toda a suíte (usa H2, não precisa de MySQL):

```bash
./mvnw test
```

Rodar uma classe específica:

```bash
./mvnw test -Dtest=PedidoServiceUnitTest
```

### O que é coberto

| Suíte                              | Tipo             | O que valida                                                                 |
|------------------------------------|------------------|------------------------------------------------------------------------------|
| `PedidoServiceUnitTest`            | Unitário (Mockito) | Regras de Ouro isoladas: tampa 1–3, limite de 3 lâminas, tipos, estoque, vínculo do `orderProduction`. Repositórios são simulados (mock). |
| `PedidoServiceValidacaoTest`       | Integração (H2)  | Regras de negócio na camada de Service contra o banco.                        |
| `PedidoEdicaoBlocosTest`           | Integração (H2)  | Regressão do erro 500 ao editar blocos (orphanRemoval).                       |
| `PedidoIntegracaoControllerTest`   | Integração E2E   | Fluxo completo Controller → Service → Banco via HTTP (MockMvc): criação de pedido duplo, entrada em produção, estoque insuficiente (422), tampa/lâminas inválidas, número duplicado (409), ciclo de status até conclusão. |
| `PersistenciaAposReinicioTest`     | Persistência     | Sobe a aplicação **duas vezes** contra um H2 em arquivo e confirma que os dados sobrevivem ao reinício. |
| `HistoricoConcorrenteTest`         | Concorrência     | 20 pedidos criados **simultaneamente**; garante histórico íntegro, sem perdas nem IDs duplicados. |
| `SwaggerDocumentacaoTest`          | Documentação     | Garante que o OpenAPI é publicado em `/v3/api-docs` com os endpoints de pedidos. |
| `SistemaBancadaApplicationTests`   | Smoke            | O contexto Spring inteiro sobe corretamente.                                 |

### Como os testes usam o H2

Em `src/test/resources/application.properties`, a propriedade
`app.datasource.manual.enabled=false` desliga o `DataSourceManualConfig` (que
aponta para o MySQL), deixando o Spring Boot autoconfigurar o H2 em memória.
Assim os testes nunca tocam o banco de produção.

---

## 9. Estrutura do projeto (backend)

```
Sistema Bancada/
├── database/criar-banco.sql            # Script SQL inicial
├── config/db-config.properties         # Configuração externa opcional
├── docs/SistemaBancada.postman_collection.json
├── src/main/java/com/tecdes/sistema_bancada/
│   ├── config/          # DataSource, OpenAPI, URLs, carregamento externo
│   ├── controller/      # Endpoints (Loja/Pedidos, Gestor, Smart/Bancada, CLP, MQTT)
│   ├── dto/             # Objetos de transporte (PedidoDTO, BlocoDTO, ...)
│   ├── exception/       # BusinessException + GlobalExceptionHandler
│   ├── model/           # Entidades JPA (Pedido, Bloco, Lamina, Estoque, Expedicao)
│   ├── repository/      # Repositórios Spring Data JPA
│   └── service/         # Regras de negócio (PedidoService) e integração CLP/MQTT
└── src/test/java/...    # Suítes de teste descritas na seção 8
```

---

## 10. Solução de problemas

- **`Cannot load driver class: org.h2.Driver` ao rodar a aplicação:** o H2 é
  dependência de teste. Em produção use MySQL (padrão). Não force o driver H2 na
  execução normal.
- **A aplicação insiste em conectar no MySQL local:** verifique se
  `config/db-config.properties` existe — ele sobrescreve o `application.properties`.
  Ajuste-o ou aponte `DB_CONFIG_PATH` para outro arquivo (ou vazio).
- **Porta 8088 ocupada:** defina `SERVER_PORT` para outra porta.
- **Acesso negado ao MySQL:** confira usuário/senha via `SPRING_DATASOURCE_*`.
