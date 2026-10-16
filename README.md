# PediuPatrao — Pratica M1 Parte 2

Evolucao individual do sistema de pedidos em Java 21, Spring Boot 3.4.4, Spring Security, Thymeleaf e MongoDB Atlas. A arquitetura original de entidades, repositorios, servicos e controladores foi preservada.

## Executar no Windows

1. Instale JDK 21 e Git. Abra a pasta que contem `pom.xml` na IDE.
2. Crie um cluster gratuito MongoDB Atlas, um usuario do banco e autorize seu IP atual.
3. Execute `Iniciar.cmd`. Na primeira execucao de uma copia nova, informe a URI completa do Atlas com usuario e senha. A URI fica criptografada pelo Windows em `.local/mongodb.xml`, vinculada ao usuario do Windows; `.local/` nao deve ser enviada ao GitHub.
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

A persistencia de pedidos/clientes e auditoria utiliza transacoes MongoDB. Exige replica set, disponivel no Atlas; um MongoDB local standalone nao atende a esta configuracao.

## Contas de demonstracao

Criadas somente quando ausentes. Senhas existentes nao sao redefinidas no reinicio.

| Usuario | Senha inicial | Perfil |
|---|---|---|
| admin | teste123 | ADMIN |
| gerente | Gerente123 | GERENTE |
| atendente | Atendente123 | ATENDENTE |

Sao contas de demonstracao academica. Novos usuarios e novas senhas exigem pelo menos oito caracteres. O login admin original foi preservado para compatibilidade.

## Permissoes

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

Decisoes para operacoes nao detalhadas no enunciado: o GERENTE executa transicoes operacionais; ADMIN e GERENTE mantem o catalogo de produtos; a exclusao de cliente e preservada para ATENDENTE, mas bloqueada se houver pedidos associados. O usuario nao pode excluir sua propria conta nem alterar seu proprio login ou perfil. O perfil e revalidado no banco a cada requisicao para evitar privilegios antigos na sessao.

## Requisitos implementados

- **RF01:** tres perfis, autenticacao com perfil persistido, autorizacao de paginas e APIs, protecao de servicos sensiveis, CSRF em formularios e chamadas JavaScript, senha omitida dos retornos JSON e do formulario de edicao. Administracao de perfis restrita ao ADMIN.
- **RF02:** itens com quantidades e precos copiados do catalogo; subtotal, desconto e total calculados em BigDecimal no servidor; identificacao e horario de entrada obtidos da sessao e do relogio do servidor. DTO de entrada nao aceita total, preco unitario, responsavel ou status como fonte de dados.
- **RF03:** fluxo RECEBIDO -> EM_PREPARACAO -> PRONTO -> SAIU_PARA_ENTREGA ou RETIRADO -> FINALIZADO. Saida grava horario e responsavel. Transicoes invalidas e alteracoes de pedidos cancelados sao rejeitadas.
- **RF04:** somente GERENTE aplica desconto de 0 a 20%, com ate duas casas decimais, ou cancela mediante justificativa. Permitido somente em RECEBIDO, EM_PREPARACAO ou PRONTO. Novo desconto substitui o anterior, sem acumular. Cancelamento preserva dados.
- **RF05:** inclusoes/alteracoes de pedidos e clientes, status, descontos, cancelamentos e exclusao de cliente sao auditados em `auditorias`, com usuario, perfil, horario UTC, entidade/ID, campos anteriores/novos e justificativa. Consulta paginada para ADMIN/GERENTE. Nao ha endpoints de edicao ou exclusao de auditoria; repositorio de consulta nao expoe escrita. Snapshots aceitam apenas Pedido/Cliente, sem credenciais. Dados cadastrais completos sao limitados aos perfis autorizados.
- **Integridade:** alteracao de pedido/cliente e auditoria na mesma transacao. Controle otimista de versao evita sobrescrever mudancas concorrentes. Em conflito, atualize os dados antes de repetir a operacao.

Datas sao armazenadas como instantes UTC e exibidas na interface no fuso America/Sao_Paulo.

Pedidos antigos permanecem consultaveis com seus campos legados. Pedidos com estados fora do fluxo novo nao sao convertidos automaticamente; use novos pedidos para demonstrar as novas regras.

## Estrutura das principais alteracoes

- `config/SecurityConfig` e `PerfilAtualFilter`: autorizacao, CSRF e atualizacao dos perfis da sessao.
- `config/TransactionConfig`: transacoes MongoDB.
- `entity/Perfil`, `StatusPedido`, `ItemPedido`, `Pedido`, `Auditoria`: modelo.
- `dto/CriarPedido`: contrato de entrada controlado.
- `service/PedidoService`: calculos, transicoes, descontos e cancelamentos.
- `service/ClienteService` e `AuditoriaService`: alteracoes cadastrais e eventos persistentes.
- `controller/AuditoriaController` e `ErroAdvice`: consulta restrita e mensagens de erro.
- Templates de usuarios, clientes, produtos, pedidos e auditoria: operacoes e navegacao por perfil.

## Testes

Com JDK 21 configurado:

```powershell
.\mvnw.cmd test
.\mvnw.cmd package
```

Os testes nao exigem Atlas: usam repositorios simulados e Spring MVC com filtros reais de seguranca. Cobrem calculos, regras, auditoria, bloqueios com CSRF valido, revogacao de perfil na sessao, omissao de senha e renderizacao das telas Thymeleaf. Nao substituem a verificacao da persistencia real no Atlas.

Veja `ROTEIRO_DEMONSTRACAO.md` para testar no navegador e gravar o video. Veja `VERIFICACAO.md` para o resultado das verificacoes feitas durante o desenvolvimento.

## Principais APIs

Todas exigem sessao autenticada; alteracoes exigem token CSRF. Para APIs de escrita, use JSON com Content-Type application/json.

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

## Entrega individual

Entregar o repositorio GitHub com este README e o video da execucao individual. Incluir o link do video na entrega ou neste README quando estiver disponivel. Conferir que o professor tem acesso a ambos. Nao enviar `.local/`, conexoes, logs ou `target/`.
