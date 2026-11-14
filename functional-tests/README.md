# Testes funcionais (caixa-preta): alugar um carro

Suíte de testes funcionais da funcionalidade **alugar um carro**, que cobre a reserva no checkout, a confirmação do aluguel e o cálculo do valor. Os casos foram derivados só da especificação abaixo, sem olhar o código, e são executados contra a API real por HTTP.

## Especificação usada como oráculo

| Regra | Descrição |
|---|---|
| RN01 | Só usuário autenticado aluga. O cliente só aluga para si; o administrador pode alugar em nome de qualquer cliente. |
| RN02 | O carro precisa existir. |
| RN03 | Carro em manutenção não pode ser reservado nem alugado. |
| RN04 | Carro sem valor de diária não pode ser reservado nem alugado. |
| RN05 | Carro com locação ativa não pode ser reservado nem alugado. |
| RN06 | A data prevista de devolução é obrigatória, no formato AAAA-MM-DD, e não pode ser anterior a hoje. |
| RN07 | Carro reservado no checkout por outro cliente fica bloqueado para terceiros durante a reserva (10 minutos). |
| RN08 | O cliente precisa de sessão ativa e do PDF da CNH enviado. |
| RN09 | Total previsto = diária × dias, com dias = max(1, devolução − hoje), em reais com 2 casas. |
| RN10 | A diária fica entre R$ 1,00 e R$ 99.999.999,99, com no máximo 2 casas decimais. |

Recusas de regra de negócio no aluguel voltam como texto com HTTP 200. Erros de entrada voltam como 400, falta de login como 401, falta de permissão como 403 e recurso inexistente como 404. Nenhuma entrada deve produzir erro interno (5xx).

## Critérios de teste combinados

- **Classes de equivalência**: data de devolução, diária, status do carro, reserva, CNH, sessão e perfil de quem aluga.
- **Análise de valor-limite**: data (ontem, hoje, amanhã), diária (0,99 / 1,00 / 1,01 e 99.999.999,99 / 100.000.000,00) e total (100 e 101 dias na diária máxima).
- **Tabela de decisão**: combinação das condições do carro, da reserva e do cliente.
- **Transição de estados**: disponível → reservado → alugado → disponível, mais transições inválidas.

## Como executar

Suba os serviços user e car apontando para bancos descartáveis. A suíte cria usuários e carros. Depois rode:

```bash
ADMIN_EMAIL=... ADMIN_PASSWORD=... python3 functional-tests/rental_black_box.py \
  --user-api http://localhost:8093 --car-api http://localhost:8094 \
  --mysql-container ft-mysql --mysql-db cardb_ft --redis-container ft-redis \
  --out resultados.json
```

A API não permite criar duas pré-condições (um carro legado sem diária e uma sessão expirada no servidor). Para elas, a suíte usa os containers do MySQL e do Redis. O script só depende da biblioteca padrão do Python.

## Última execução (14/11/2026)

33 de 35 casos passaram. Os dois que falharam apontam defeitos reais:

| Caso | Defeito |
|---|---|
| CT18 | Diária máxima por 101 dias: o total passa do limite da coluna `estimated_total` e a API responde 500, expondo a mensagem interna do MySQL. |
| CT27 | Alugar um carro inexistente responde 500 em vez de 404. |
