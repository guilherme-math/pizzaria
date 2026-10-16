# Roteiro de verificacao e video

Execute Iniciar.cmd, aguarde o servidor e abra http://localhost:8080. Use o botao Sair para alternar perfis; nao inicie duas instancias na porta 8080.

## 1 Autenticacao e cadastro

1. Entre como admin / teste123. Mostre o perfil ADMIN no menu.
2. Abra Usuarios. Mostre os tres perfis existentes. Crie um usuario de teste pelo formulario e atribua ATENDENTE. Edite-o para mostrar a administracao de perfis.
3. Abra Produtos. Cadastre Pizza Calabresa, tipo Pizza, preco 50,00, ativo.
4. Saia e entre como atendente / Atendente123.
5. Abra Clientes e cadastre Cliente Demonstracao com telefone e endereco ficticios.
6. Edite o telefone e o logradouro; anote os valores anterior e novo.

## 2 Pedido e calculo

1. Como ATENDENTE, abra Pedidos.
2. Selecione o cliente e duas unidades da pizza de R$ 50,00.
3. Registre o pedido. Mostre total R$ 100,00, RECEBIDO, horario e responsavel.
4. Registre mais dois pedidos para cancelamento e para retirada.

## 3 Provar bloqueio nas APIs

Na pagina Pedidos, abra o console do navegador (F12 -> Console). Copie o ID de um pedido criado. Substitua ID_DO_PEDIDO nos comandos. A funcao api envia o token CSRF valido, de modo que o teste comprova autorizacao por perfil.

Como ATENDENTE:

```javascript
api('/api/pedidos/ID_DO_PEDIDO/desconto', 'POST', {percentual: 10}).catch(e => console.log(e.message))
```

Mostre a resposta 403 na aba Network/Rede. Teste tambem:

```javascript
api('/api/usuarios').catch(e => console.log(e.message))
api('/api/auditoria').catch(e => console.log(e.message))
```

Como ADMIN, repita o desconto e tente cancelamento:

```javascript
api('/api/pedidos/ID_DO_PEDIDO/cancelamento', 'POST', {justificativa: 'Teste de autorizacao'}).catch(e => console.log(e.message))
```

Ambos devem retornar 403. O ADMIN nao e superusuario das operacoes exclusivas do GERENTE.

## 4 Operacoes do gerente

1. Entre como gerente / Gerente123. Mostre o perfil.
2. Em Pedidos, tente desconto 21: deve ser rejeitado. Aplique 10: total do pedido de R$ 100,00 deve ficar R$ 90,00.
3. Avance o primeiro pedido: EM PREPARACAO -> PRONTO -> SAIU PARA ENTREGA. Mostre horario/responsavel da saida. Finalize.
4. No segundo pedido, tente cancelar sem justificativa: deve ser rejeitado. Cancele com Cliente desistiu. Mostre o historico preservado.
5. No terceiro pedido, avance ate PRONTO -> RETIRADO -> FINALIZADO.
6. Demonstre uma transicao invalida pelo console, em pedido RECEBIDO:

```javascript
api('/api/pedidos/ID_DO_PEDIDO/status?status=FINALIZADO', 'PUT').catch(e => console.log(e.message))
```

7. Em pedido cancelado, tente alteracao de status; em pedido que ja saiu, tente desconto ou cancelamento. Devem ser rejeitados pelo servidor, mesmo que os botoes nao estejam visiveis.

## 5 Auditoria

1. Como GERENTE, abra Auditoria.
2. Mostre o evento de cadastro do pedido e as mudancas de status, desconto e cancelamento executadas.
3. Mostre usuario, perfil, horario e valores anteriores/novos.
4. Mostre a alteracao do telefone/endereco do cliente realizada pelo ATENDENTE.
5. Como ADMIN, mostre que tambem consegue consultar auditoria.
6. Como ATENDENTE, mostre que /api/auditoria e /auditoria sao bloqueadas.

## 6 Encerramento da entrega

1. Grave diretamente a tela com narracao curta explicando o que esta demonstrando. Use dados ficticios.
2. Execute os testes com .\mvnw.cmd test e mostre o resultado.
3. Confirme que o README tem as instrucoes e que o codigo esta no seu repositorio individual.
4. Publique o video em local acessivel ao professor e inclua o link na entrega. Confira a permissao de acesso.
5. Envie antes de 16/10/2026 as 22h, horario de Sao Paulo.

O video deve demonstrar a sua execucao e os registros de auditoria gerados por ela. A gravacao ainda precisa ser feita pelo aluno.
