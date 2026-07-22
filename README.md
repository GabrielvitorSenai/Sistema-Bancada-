# Sistema de Gerenciamento da Planta SMART 4.0

Sistema de acompanhamento e operação de uma linha didática 4.0 (bancada SMART
4.0), desenvolvido ao longo das etapas de modelagem do banco, backend, frontend
e, por fim, confiabilidade e implantação (go-live).

## Estrutura do repositório

| Pasta              | Descrição                                                                 |
|--------------------|---------------------------------------------------------------------------|
| `Sistema Bancada/` | **Backend** Spring Boot (API REST, regras de negócio, integração CLP/MQTT, testes e Swagger). |
| `frontend-vue/`    | **Frontend** em Vue 3 (dashboard, loja, linha, gestão e expedição).       |

## Começando

- **Backend / API / testes / Swagger:** consulte o manual de instalação em
  [`Sistema Bancada/README.md`](Sistema%20Bancada/README.md).
- **Frontend:** consulte [`frontend-vue/`](frontend-vue/) (Vue 3 + Vite).

## Etapa 4 — Confiabilidade e Implantação (destaques)

- **Testes automatizados** (JUnit 5 + Mockito + H2): unitários das Regras de
  Ouro, integração Controller → Banco, persistência após reinício e histórico
  sob operações simultâneas. Rode com `./mvnw test` dentro de `Sistema Bancada/`.
- **Documentação da API** com Swagger/OpenAPI em `/swagger-ui.html`.
- **Manual de instalação** completo (script SQL inicial, variáveis de ambiente
  do Spring Boot e coleção Postman) no README do backend.
