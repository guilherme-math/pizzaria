# Validação manual

## Resultado informado pelo responsável

Em 05/11/2026, o responsável informou que o dashboard passou a funcionar com o MongoDB Atlas e que revisou todos os usuários do sistema. A confirmação ocorreu depois da atualização do IP autorizado no Atlas e da correção dos intervalos das consultas de pedidos e custos.

Esse relato confirma o carregamento do painel na execução do responsável. Ele não equivale a um relatório de teste automatizado contra o Atlas. Não foram coletados valores de senha nem conferidos seus conteúdos.

## Verificações automatizadas associadas

- Os filtros BSON foram conferidos usando o mecanismo real de repositórios Spring Data, sem conexão ao banco.
- Cálculos, filtros, custos e autorização foram testados com repositórios simulados.
- A resposta JSON em resposta-api-dados-de-teste.json é uma evidência com dados de teste.
- Quantidades e resultados da suíte estão em testes-dashboard.txt.
