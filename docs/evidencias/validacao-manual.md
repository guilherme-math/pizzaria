# Validação manual

## Validação com o MongoDB Atlas

Em 05/11/2026, validei o carregamento do dashboard com os dados do MongoDB Atlas e revisei os usuários do sistema. Realizei essa conferência após atualizar o IP autorizado no Atlas e corrigir os intervalos das consultas de pedidos e custos.

Essa validação manual confirmou o carregamento do painel conectado ao banco. Registrei os testes automatizados separadamente, pois eles usam repositórios simulados e não comprovam, por si só, a persistência no Atlas.

## Verificações automatizadas associadas

- Conferi a montagem dos filtros BSON pelo mecanismo real de repositórios Spring Data, sem conexão ao banco.
- Validei cálculos, filtros, custos e autorização com repositórios simulados.
- Mantive um exemplo de resposta JSON com dados de teste em [resposta-api-dados-de-teste.json](resposta-api-dados-de-teste.json).
- Registrei as quantidades e os resultados da suíte em [testes-dashboard.txt](testes-dashboard.txt).
