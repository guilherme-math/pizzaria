# Pediu Patrão

Sistema de pedidos com dashboard administrativo, desenvolvido em Java 21, Spring Boot 3.4.4, Spring Security, Thymeleaf e MongoDB Atlas. A aplicação reúne cadastro de clientes e produtos, fluxo de pedidos, permissões por perfil, auditoria e análise de vendas.

## Dashboard administrativo M2

Acesse `/dashboard` após entrar como ADMIN ou GERENTE. O painel usa o mesmo layout e menu das demais telas e consome sua própria API. Os gráficos usam Chart.js local. Bootstrap e ícones seguem as dependências da interface existente.

O painel apresenta faturamento líquido, ticket médio, pedidos, clientes atendidos, situação dos pedidos, vendas por período, horário e região, produtos mais vendidos, vendas por cliente e custos operacionais diários. Os filtros permitem período, faixa de horário, região, cliente, situação e agrupamento por dia ou mês. Há atualização manual e automática a cada 60 segundos.

Faturamento e ticket médio consideram pedidos finalizados após descontos. O período usa a entrada do pedido no horário de São Paulo. Custos são informados por dia. Sem todos os dias preenchidos, ou com um recorte segmentado, o resultado operacional fica indisponível.

- [Requisitos, decisões de projeto e contratos da API](docs/DASHBOARD.md)
- [Evidências dos testes automatizados](docs/evidencias/testes-dashboard.txt)
- [Registro da validação manual](docs/evidencias/validacao-manual.md)
- [Exemplo de resposta gerada com dados de teste](docs/evidencias/resposta-api-dados-de-teste.json)

## Apresentação M2

Vídeo demonstrativo de até três minutos: aguardando a gravação. O link será adicionado aqui após o envio e a conferência do acesso.

## Versão e branches

A implementação da API e do dashboard está na branch `feature/dashboard-administrativo`, com integração prevista pelo PR para `develop` e depois para `main`. A branch feature será mantida para identificar o trabalho desta atividade. Enquanto a integração não for concluída, consulte essa branch para executar a versão M2.

## Executar no Windows

1. Instale JDK 21 e Git. Abra a pasta que contem `pom.xml` na IDE.
2. Crie um cluster gratuito MongoDB Atlas, um usuario do banco e autorize seu IP atual.
3. Se o banco já possui um ADMIN, execute `Iniciar.cmd`. Para banco novo, use o comando com `-CriarAdministrador` da seção de primeiro acesso. Na primeira execucao de uma copia nova, informe a URI completa do Atlas com usuario e senha. A URI fica criptografada pelo Windows em `.local/mongodb.xml`, vinculada ao usuario do Windows. A pasta `.local/` nao deve ser enviada ao GitHub.
4. Aguarde a mensagem `Started PizzariaPedidosApplication` e acesse http://localhost:8080.
5. Pare com Ctrl+C. Para iniciar novamente, execute `Iniciar.cmd`.

No terminal do IntelliJ tambem e possivel executar:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Iniciar.ps1
```

Para trocar a conexao salva:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Iniciar.ps1 -Reconfigurar
```

Alternativa multiplataforma: configure `JAVA_HOME` para JDK 21 e `MONGODB_URI` no ambiente e execute `./mvnw spring-boot:run` (Linux/macOS) ou `.\mvnw.cmd spring-boot:run` (Windows). Nao e necessario Maven ou Tomcat separado. Ao iniciar pela IDE, configure tambem `MONGODB_URI` na configuracao de execucao.

A persistencia de pedidos/clientes e auditoria utiliza transacoes MongoDB. Exige replica set, disponivel no Atlas. Um MongoDB local standalone nao atende a esta configuracao.

## Primeiro acesso sem senhas padrao

O projeto nao cria contas de demonstracao nem possui senhas iniciais fixas. Em um banco sem ADMIN, forneca o login e uma senha exclusiva por variaveis de ambiente apenas para a primeira inicializacao. A senha inicial exige pelo menos 12 caracteres e no maximo 72 bytes UTF-8. O login aceita de 3 a 80 letras, numeros, pontos, hifens ou sublinhados.

No Windows, use a entrada oculta do iniciador:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\Iniciar.ps1 -CriarAdministrador
```

O script pergunta login e senha, passa a configuracao ao processo Java e nao grava a senha inicial em arquivo. Depois que o administrador existir, inicie normalmente com `Iniciar.cmd`. Cadastre GERENTE e ATENDENTE pela tela Usuarios, escolhendo senhas individuais.

Em outros ambientes, injete `BOOTSTRAP_ADMIN_USERNAME` e `BOOTSTRAP_ADMIN_PASSWORD` pelo gerenciador de segredos ou ambiente de execucao, junto de `MONGODB_URI`. Nao escreva os valores no repositorio, no README, em argumentos de linha de comando ou em logs. Retire as variaveis de bootstrap apos o primeiro cadastro.

Se ja houver um ADMIN, a configuracao de bootstrap e ignorada. Nenhuma senha e redefinida no reinicio. Se o login informado ja pertencer a outra conta, o bootstrap e recusado, sem promover o usuario existente.

**Bancos da versao anterior:** remover as senhas fixas do codigo nao troca os hashes ja salvos. Entre como ADMIN e altere, pela tela Usuarios, as senhas das antigas contas de demonstracao, usando valores exclusivos. Nao e necessario apagar clientes, pedidos ou auditorias. A migracao de perfis USER passa a ATENDENTE e nunca concede ADMIN apenas pelo nome do login.

## Permissões

| Operacao | ADMIN | GERENTE | ATENDENTE |
|---|---|---|---|
| Administrar usuarios e perfis | Sim | Nao | Nao |
| Consultar pedidos | Sim | Sim | Sim |
| Registrar pedidos | Nao | Nao | Sim |
| Avancar status e registrar saida | Nao | Sim | Nao |
| Aplicar desconto / cancelar | Nao | Sim | Nao |
| Consultar clientes | Nao | Sim | Sim |
| Cadastrar / atualizar clientes | Nao | Nao | Sim |
| Excluir cliente sem pedidos | Nao | Nao | Sim |
| Consultar produtos | Sim | Sim | Sim |
| Cadastrar / editar / excluir produtos | Sim | Sim | Nao |
| Consultar auditoria | Sim | Sim | Nao |
| Consultar dashboard e indicadores | Sim | Sim | Nao |
| Informar custos operacionais diarios | Sim | Sim | Nao |

Decisoes para operacoes nao detalhadas no enunciado: o GERENTE executa transicoes operacionais. ADMIN e GERENTE mantem o catalogo de produtos. A exclusao de cliente e preservada para ATENDENTE, mas bloqueada se houver pedidos associados. O usuario nao pode excluir sua propria conta nem alterar seu proprio login ou perfil. O perfil e revalidado no banco a cada requisicao para evitar privilegios antigos na sessao.

## Funcionalidades do sistema de pedidos M1

- **RF01:** tres perfis, autenticacao com perfil persistido, autorizacao de paginas e APIs, protecao de servicos sensiveis, CSRF em formularios e chamadas JavaScript, senha omitida dos retornos JSON e do formulario de edicao. Administracao de perfis restrita ao ADMIN.
- **RF02:** itens com quantidades e precos copiados do catalogo. Subtotal, desconto e total calculados em BigDecimal no servidor. Identificacao e horario de entrada obtidos da sessao e do relogio do servidor. DTO de entrada nao aceita total, preco unitario, responsavel ou status como fonte de dados.
- **RF03:** fluxo RECEBIDO -> EM_PREPARACAO -> PRONTO -> SAIU_PARA_ENTREGA ou RETIRADO -> FINALIZADO. Saida grava horario e responsavel. Transicoes invalidas e alteracoes de pedidos cancelados sao rejeitadas.
- **RF04:** somente GERENTE aplica desconto de 0 a 20%, com ate duas casas decimais, ou cancela mediante justificativa. Permitido somente em RECEBIDO, EM_PREPARACAO ou PRONTO. Novo desconto substitui o anterior, sem acumular. Cancelamento preserva dados.
- **RF05:** inclusoes/alteracoes de pedidos e clientes, status, descontos, cancelamentos e exclusao de cliente sao auditados em `auditorias`, com usuario, perfil, horario UTC, entidade/ID, campos anteriores/novos e justificativa. Consulta paginada para ADMIN/GERENTE. Nao ha endpoints de edicao ou exclusao de auditoria. Repositorio de consulta nao expoe escrita. Snapshots aceitam apenas Pedido/Cliente, sem credenciais. Dados cadastrais completos sao limitados aos perfis autorizados.
- **Integridade:** alteracao de pedido/cliente e auditoria na mesma transacao. Controle otimista de versao evita sobrescrever mudancas concorrentes. Em conflito, atualize os dados antes de repetir a operacao.

Datas sao armazenadas como instantes UTC e exibidas na interface no fuso America/Sao_Paulo.

Pedidos antigos permanecem consultaveis com seus campos legados. Pedidos com estados fora do fluxo novo nao sao convertidos automaticamente. Use novos pedidos para demonstrar as novas regras.

## Estrutura das principais alteracoes

- `config/SecurityConfig` e `PerfilAtualFilter`: autorizacao, CSRF e atualizacao dos perfis da sessao.
- `config/TransactionConfig`: transacoes MongoDB.
- `entity/Perfil`, `StatusPedido`, `ItemPedido`, `Pedido`, `Auditoria`: modelo.
- `dto/CriarPedido`: contrato de entrada controlado.
- `service/PedidoService`: calculos, transicoes, descontos e cancelamentos.
- `service/ClienteService` e `AuditoriaService`: alteracoes cadastrais e eventos persistentes.
- `controller/AuditoriaController` e `ErroAdvice`: consulta restrita e mensagens de erro.
- Templates de usuários, clientes, produtos, pedidos, auditoria e dashboard: operações e navegação no mesmo layout.
- Pacote `dashboard`: controller, filtros, DTOs, serviço analítico e repositório de custos.
- `dashboard.js` e `dashboard.css`: consumo da API, gráficos e ajustes visuais do painel.

## Testes

Com JDK 21 configurado:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

Os testes nao exigem Atlas: usam repositorios simulados e Spring MVC com filtros reais de seguranca. Cobrem calculos, regras, auditoria, bloqueios com CSRF valido, revogacao de perfil na sessao, omissao de senha e renderizacao das telas Thymeleaf. Nao substituem a verificacao da persistencia real no Atlas.


## APIs do sistema de pedidos M1

Todas exigem sessao autenticada. Alteracoes exigem token CSRF. Para APIs de escrita, use JSON com Content-Type application/json.

| Metodo e rota | Uso |
|---|---|
| GET /api/usuarios | Consultar usuarios (ADMIN) |
| POST /api/usuarios | Criar usuario: username, password, role |
| PUT /api/usuarios/{id} | Atualizar usuario |
| DELETE /api/usuarios/{id} | Excluir usuario |
| GET /api/clientes | Consultar clientes |
| POST /api/clientes | Cadastrar cliente |
| PUT /api/clientes/{id} | Atualizar cliente |
| DELETE /api/clientes/{id} | Excluir cliente sem pedidos |
| GET /api/produtos | Consultar catalogo |
| POST /api/produtos | Criar produto: nome, tipo, preco, ativo |
| PUT /api/produtos/{id} | Editar produto |
| DELETE /api/produtos/{id} | Excluir produto |
| GET /api/pedidos | Consultar pedidos |
| POST /api/pedidos | Criar: clienteId, itens: [{produtoId, quantidade}] |
| PUT /api/pedidos/{id}/status?status=PRONTO | Avancar status |
| POST /api/pedidos/{id}/desconto | Aplicar: {"percentual":10} |
| POST /api/pedidos/{id}/cancelamento | Cancelar: {"justificativa":"Cliente desistiu"} |
| GET /api/auditoria?pagina=0 | Consultar eventos paginados |
