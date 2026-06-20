# Códigos de Teste - CompilaMeme 🚀

Este diretório contém códigos de teste com a extensão `.meme` para você carregar e validar na IDE do seu compilador.

## Arquivos Disponíveis

1. 📝 **[declaracoes.meme](file:///c:/Users/GABRIEL%20VIEIRA/Desktop/TrabalhoCompiladores/exemplos/declaracoes.meme)**
   - **Objetivo**: Testar declarações simples e múltiplas de variáveis com os tipos de dados customizados (`brabo`, `realoficial`, `fofoca`), atribuições e a função de impressão `F()`.

2. 🔀 **[condicionais.meme](file:///c:/Users/GABRIEL%20VIEIRA/Desktop/TrabalhoCompiladores/exemplos/condicionais.meme)**
   - **Objetivo**: Testar estruturas de decisão baseadas em memes: `tem_certeza?` (if) e `senao` (else), utilizando operadores de comparação como `>=` e `==`.

3. 🔄 **[repeticao_enquanto.meme](file:///c:/Users/GABRIEL%20VIEIRA/Desktop/TrabalhoCompiladores/exemplos/repeticao_enquanto.meme)**
   - **Objetivo**: Testar o laço condicional de repetição `segue_o_baile` (while), controlando variáveis e imprimindo o contador a cada iteração.

4. 🏃 **[repeticao_para.meme](file:///c:/Users/GABRIEL%20VIEIRA/Desktop/TrabalhoCompiladores/exemplos/repeticao_para.meme)**
   - **Objetivo**: Testar o laço contado `ja_acabou_jessica` (for), validando a declaração da variável contadora, a condição limite e o incremento.

5. 🏆 **[programa_completo.meme](file:///c:/Users/GABRIEL%20VIEIRA/Desktop/TrabalhoCompiladores/exemplos/programa_completo.meme)**
   - **Objetivo**: Um script integrado misturando declaração múltipla, atribuições, expressões matemáticas complexas, desvios condicionais e laços de repetição (`segue_o_baile` e `ja_acabou_jessica`).

6. ⚠️ **[contem_erros.meme](file:///c:/Users/GABRIEL%20VIEIRA/Desktop/TrabalhoCompiladores/exemplos/contem_erros.meme)**
   - **Objetivo**: Testar a **robustez** do analisador léxico e sintático da sua IDE. Ele possui erros intencionais (como caracteres inválidos `@` e `$` e falta de `;`), permitindo que você veja as mensagens vermelhas na IDE e teste o mecanismo de recuperação de erros da gramática.

---

## Como Rodar a IDE por Linha de Comando

Se você quiser rodar o compilador por fora do Eclipse/VS Code direto no seu terminal, use:

1. **Compilar o projeto**:
   ```bash
   javac -d bin src/pacotememe/*.java
   ```

2. **Executar a IDE**:
   ```bash
   java -cp bin pacotememe.gramaticameme
   ```
