# Pautas Worker

Decidi criar esse componente pois achei por bem realizar o processamento dos votos de forma assincrona. O maior motivador dessa decisão foi o fato de em cada voto ser necessário consultar um sistema externo para validação do CPF do associado (como sugerido pelas tarefas bônus do desafio). Como não tenho controle da estabilidade de um sistema externo, considero sensato manter o processamento assincrono.

Também utilizo esse componente para verificar de tempos em tempos se uma sessão de uma pauta já encerrou, ou se foi concluída, ou se ainda está aguardando o processamento dos votos.

## Pré-requisitos para rodar

Tenologias utilizadas:
- `JAVA - openjdk version "11.0.9" 2020-10-20`
- `Apache Maven 3.6.0`
- `RabbitMQ 3.7.15`
- `Postgres`

Comando que utilizei para rodar o RabbitMQ via docker.

```bash
docker run --rm --hostname localhost --name rabbit-test -p 15672:15672 -p 5672:5672 rabbitmq:3.7.15-management
```

Comando que utilizei para rodar o Postgres via docker (já com o usuário, senha e database utilizados na solução).

```bash
docker run --rm --hostname localhost --name postgres-test -p 5432:5432 -e POSTGRES_USER='root' -e POSTGRES_PASSWORD='123456' -e POSTGRES_DB='pautas' postgres
```

## Rodar o Worker

```bash
mvn spring-boot:run
```

Mais detalhes em https://github.com/tonisantes/pautas-api