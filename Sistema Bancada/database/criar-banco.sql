-- =====================================================================
--  Script SQL inicial da planta SMART 4.0
--  Banco: dbSmart40 (MySQL 8+)
-- =====================================================================
--
--  PASSO 1 - Criação do banco (execute ANTES de iniciar a aplicação).
--  A aplicação também cria o banco automaticamente
--  (createDatabaseIfNotExist=true na URL), mas este comando garante o
--  charset/collation corretos caso você prefira criá-lo manualmente.
-- ---------------------------------------------------------------------

CREATE DATABASE IF NOT EXISTS dbSmart40
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE dbSmart40;

-- ---------------------------------------------------------------------
--  PASSO 2 - Tabelas
--  As tabelas (pedido, bloco, lamina, estoque, expedicao) são criadas e
--  atualizadas automaticamente pelo Hibernate na primeira execução da
--  aplicação (spring.jpa.hibernate.ddl-auto=update). Por isso NÃO é
--  necessário criá-las manualmente aqui.
-- ---------------------------------------------------------------------

-- ---------------------------------------------------------------------
--  PASSO 3 (OPCIONAL) - Carga inicial do magazine de estoque
--  Execute este bloco APENAS DEPOIS de iniciar a aplicação pela primeira
--  vez (quando a tabela `estoque` já existe). Ele preenche as 28 posições
--  do magazine com uma distribuição de exemplo para a demonstração de
--  Go-Live: 1 = preto, 2 = vermelho, 3 = azul, 0 = vazio.
--  Descomente as linhas abaixo para usar.
-- ---------------------------------------------------------------------

-- DELETE FROM estoque;
-- INSERT INTO estoque (posicao_estoque, cor) VALUES
--   (1,1),(2,1),(3,1),(4,1),(5,1),(6,1),(7,1),(8,1),(9,1),(10,1),
--   (11,2),(12,2),(13,2),(14,2),(15,2),(16,2),(17,2),(18,2),(19,2),
--   (20,3),(21,3),(22,3),(23,3),(24,3),(25,3),(26,3),(27,3),(28,3);

-- Consulta rápida para conferir a carga:
-- SELECT cor, COUNT(*) AS quantidade FROM estoque GROUP BY cor;
