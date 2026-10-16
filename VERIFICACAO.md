# Verificacao da implementacao

## Testes locais

24 testes automatizados passaram, sem falhas, em 15/10/2026:

- 10 testes de pedidos: preco do catalogo, total, entrada, quantidades, transicoes, saida, desconto, cancelamento e bloqueios.
- 6 testes de filtros Spring Security: administracao de usuarios, desconto/cancelamento exclusivos do gerente, CSRF, auditoria, responsabilidades e revogacao de perfil numa sessao existente.
- 3 testes de auditoria/serializacao: valores anterior/novo, identidade e horario, recusa de credenciais e omissao da senha no JSON.
- 5 testes de telas com Spring MVC/Thymeleaf: login com CSRF, paginas dos tres perfis, formulario sem hash de senha, consulta de auditoria e cadastro de clientes.

Os testes de autorizacao enviam token CSRF valido quando avaliam o perfil. Assim o 403 nao e causado apenas pela ausencia do token.

## Verificacao que ainda depende da execucao do aluno

A aplicacao original foi executada com sucesso pelo aluno antes das alteracoes. A tentativa de iniciar a versao alterada no ambiente de ferramentas falhou em uma consulta DNS do Atlas, causada por erro de socket/loopback do Java deste ambiente. Nao foi possivel confirmar daqui a persistencia real ou as transacoes com o Atlas.

Reiniciar pelo Iniciar.cmd e executar o roteiro completo no navegador. Conferir que pedido e auditoria aparecem juntos no MongoDB, que os horarios/responsaveis correspondem a operacao e que os tres perfis entram normalmente. Registrar eventuais erros para correcao antes da entrega.

A gravacao do video, o link do video e o envio na plataforma da faculdade ainda dependem do aluno.
