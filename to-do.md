# 1. Corrigir Stage 1

## 1.1. Condição de Validity no IBFT
~~1. Mudar link de Digital Signature para MAC~~
~~2. Quando o cliente manda pedido tem que fazer DS do valor~~ + nonce!
3. Os nós quando recebem verificam se o valor é válido
4. Para qualquer tipo de mensagens, quem envia tem que assinar o valor por cima e quem recebe tem que validar

## 1.2. Prepare Piggyback
1. Quando um nó propaga uma mensagem de round change, a mensagem deve ser acompanhada por um quorum de prepares que justifiquem o valor (se mandar valor, se não mandar valor nao precisa de mandar quorum).

## 1.3. Justificação de Round Changes
1. Sendo o lider, antes de mandar um pre-prepare, tem que se ver o valor:
    - O valor é o input inicial caso todas as mensagens de round change que recebeu não têm valor
    - O valor é determinado de forma a que: o valor do highet prepared do quorum round changes = ao valor de um quorum de prepares
2. A receber pre-prepare é preciso confirmar se é valido:
    - Se a ronda = 1
    - Justify PrePrepare

## 1.4. Execução "concorrente" de instâncias de consenso
~~1. Mudar o while da linha 131 do NodeService para outro sitio do algoritmo. As instancias do algoritmo param sempre no uponCommit caso estejam no "futuro"~~

## 1.5. Camadas de abstração nos servidores (Library - Consensus - Application)
1. Mudar ledger de sítio (tirar de NodeService e pôr no SerenityLedgerService)

## 1.6. Melhor validação de respostas do lado do cliente
1. O cliente tem que esperar por f+1 respostas.
    - f+1 respostas? e assim temos que saber distinguir qual está bem e qual está mal
    - f+1 respotas iguais? e assim é só esperar por f+1 respostas iguais sem necessidade de confirmar

## 1.7. Mais testes de comportamento bizantino

# 2. Stage 2

## 2.1. Corrigir Stage 1

## 2.2. Implementar clientes bizantinos
O que são?
Como?

## 2.3. Criar conceito de bloco, com os valores a serem strings á mesma e tendo 1 transação por bloco
1. Criar noção base de bloco em que este deve ter os campos:
    - Transação (tx)
    - Client ID (p/tx)
    - Client Signature (p/tx)
    

## 2.4. Várias transações por bloco

## 2.5. Implementar conceito de transfer e de checkAccount

## 2.6. Testes
