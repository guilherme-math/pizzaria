# Validação manual

## Resultado informado pelo responsável

Em 05/11/2026, o responsável informou que o dashboard passou a funcionar com o MongoDB Atlas e que revisou todos os usuários do sistema. A confirmação ocorreu depois da atualização do IP autorizado no Atlas e da correção dos intervalos das consultas de pedidos e custos.

Esse relato confirma o carregamento do painel na execução do responsável. Ele não equivale a um relatório de teste automatizado contra o Atlas. Não foram coletados valores de senha nem conferidos seus conteúdos.

## Verificações automatizadas associadas

- Os filtros BSON foram conferidos usando o mecanismo real de repositórios Spring Data, sem conexão ao banco.
- Cálculos, filtros, custos e autorização foram testados com repositórios simulados.
- A resposta JSON em resposta-api-dados-de-teste.json é uma evidência com dados de teste.
- Quantidades e resultados da suíte estão em testes-dashboard.txt.

## Conferência durante a apresentação final

Use dados existentes e escolha um período em que haja pedidos finalizados. Mostre as ações na própria gravação.

| Conferência | Resultado esperado |
| --- | --- |
| Entrar como ADMIN ou GERENTE e abrir Dashboard | Painel com dados do Atlas |
| Aplicar período e faixa de horário | Indicadores e gráficos atualizados para o recorte |
| Alternar DIA e MES | Evolução agrupada conforme a opção |
| Filtrar região e cliente | Análise limitada à seleção |
| Ver situação dos pedidos e produtos mais vendidos | Quantidades e ranking do mesmo recorte |
| Consultar GET /api/dashboard na aba Rede | HTTP 200, filtros e resposta JSON |
| Informar um custo diário e atualizar a consulta | Valor reaparece para a data informada |
| Consultar um único dia com custo preenchido, sem filtros adicionais | Resultado operacional disponível |
| Consultar período com custos faltantes | Aviso de cobertura e resultado indisponível |
| Acessar /api/dashboard como ATENDENTE | HTTP 403 |

As linhas acima são um roteiro de conferência. Não estão marcadas como testes manuais já executados quando não há evidência individual registrada. O vídeo final deverá demonstrar os itens selecionados para a apresentação de até três minutos.
