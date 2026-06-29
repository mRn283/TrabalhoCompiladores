# Documentação Técnica - Fase 2: CompilaMeme (MemeLang)

Bem-vindo à documentação oficial da **Fase 2** do projeto **CompilaMeme**, o front-end do compilador para a linguagem lúdico-acadêmica **MemeLang**. Esta documentação detalha os avanços realizados para tornar o compilador robusto, resiliente a falhas sintáticas/léxicas, capaz de representar a estrutura gramatical na forma de Árvore de Sintaxe Abstrata (AST) e integrado a uma IDE moderna baseada em Java Swing.

---

## 1. Visão Geral da Fase 2

A Fase 2 elevou o patamar do compilador de um analisador linear e frágil para um sistema interativo e tolerante a falhas. As principais melhorias implementadas foram:

*   **Adequação para LL(1):** Ajuste estrito das regras sintáticas para operar com preditividade e complexidade linear $O(N)$ usando `LOOKAHEAD = 1`.
*   **Recuperação de Erros (Panic Mode):** Implementação de rotinas de sincronização que impedem que o compilador aborte na primeira falha detectada. O compilador descarta a instrução incorreta e continua a análise do código seguinte, acumulando erros em um relatório detalhado.
*   **Geração e Visualização de AST:** Criação de uma estrutura de dados de árvore sintática abstrata (`ASTNode`) que é preenchida dinamicamente durante o parsing e renderizada em tempo real na interface gráfica.
*   **IDE Integrada:** Interface de desenvolvimento rica com editor de texto, numeração dinâmica de linhas (gutter), realce visual de erros na tela, tabela de análise de tokens e um console interativo capaz de apontar e navegar até a linha do erro com um clique.

---

## 2. Adequação para Linguagem LL(1)

Para permitir que o parser avance de forma preditiva examinando um único token à frente (`LOOKAHEAD = 1`), a gramática do CompilaMeme foi reformulada para evitar ambiguidades e recursões à esquerda.

### Configuração no JavaCC
No arquivo gramaticameme.jj, o analisador foi parametrizado explicitamente para processamento preditivo determinístico:

```jj
options
{
  static = true;
  LOOKAHEAD = 1;
}
```

### Eliminação de Recursão à Esquerda
Regras gramaticais clássicas como a de expressões aritméticas apresentam recursividade à esquerda (por exemplo, $E --> E + T | T$). Em um analisador sintático descendente recursivo (como o gerado pelo JavaCC), essa estrutura causaria loops de recursão infinita.

A solução adotada no arquivo gramaticameme.jj foi eliminar a recursão substituindo-a por iterações em formato EBNF (com o operador de repetição `*`). Veja os exemplos reais extraídos do código:

#### Expressão Aritmética:
```jj
ASTNode ExpressaoAritmetica() :
{
  ASTNode left = null;
  ASTNode right = null;
  Token op = null;
}
{
  left = Termo() 
  ( 
    ( op = <PLUS> | op = <MINUS> ) 
    right = Termo()
    {
      ASTNode temp = new ASTNode("OpAritmético", op.image, op.beginLine, op.beginColumn);
      temp.addChild(left);
      temp.addChild(right);
      left = temp;
    }
  )*
  { return left; }
}
```

#### Termo Aritmético:
```jj
ASTNode Termo() :
{
  ASTNode left = null;
  ASTNode right = null;
  Token op = null;
}
{
  left = Fator() 
  ( 
    ( op = <MULTIPLY> | op = <DIVIDE> ) 
    right = Fator()
    {
      ASTNode temp = new ASTNode("OpMultiplicativo", op.image, op.beginLine, op.beginColumn);
      temp.addChild(left);
      temp.addChild(right);
      left = temp;
    }
  )*
  { return left; }
}
```

**Mecanismo de funcionamento:**
1. O parser processa o primeiro operando chamando a regra de maior precedência (`Termo()` ou `Fator()`).
2. Entra em um loop `( ... )*` que avalia se o próximo token é um operador associativo à esquerda (como `+` ou `-` para expressões aritméticas).
3. Se houver o operador correspondente, o parser consome o operador, avalia o próximo operando à direita e reconstrói a árvore de sintaxe agregando o acumulado à esquerda como filho.
4. Caso o próximo token não combine, o loop é finalizado imediatamente sem consumir nada. Isso é feito com lookahead de apenas 1 token, garantindo linearidade e eficiência.

---

## 3. Sincronização e Recuperação de Erros (Panic Mode)

A capacidade de recuperação é vital para uma experiência de desenvolvimento aceitável. O compilador do CompilaMeme adota o **Modo de Pânico (Panic Mode)**, no qual ele tenta ignorar fragmentos de código inválidos para restabelecer a análise no início da próxima instrução viável.

### Capturando a Exceção
O parser intercepta a `ParseException` nos pontos estratégicos da gramática para que uma falha local não aborte o processamento de todo o script.

Em cada bloco de instrução em gramaticameme.jj, como na regra `Comando()`, temos:

```jj
ASTNode Comando() :
{
  ASTNode node = null;
}
{
  try {
    (
      node = Declaracao()
    | node = AtribuicaoOuImpressao()
    | node = Condicional()
    | node = LacoEnquanto()
    | node = LacoPara()
    )
    { return node; }
  } catch (ParseException e) {
    registrarErro(e);
    recuperarErro();
    return new ASTNode("Erro Sintático", e.currentToken.next.image, e.currentToken.next.beginLine, e.currentToken.next.beginColumn);
  }
}
```

E na regra principal da raiz (`Programa()`), para que o parser continue capturando instruções mesmo após desvios graves:

```jj
ASTNode Programa() :
{
  ASTNode node = new ASTNode("Programa");
  ASTNode cmd;
  ASTNode rest = null;
}
{
  try {
    ( cmd = Comando() { if (cmd != null) node.addChild(cmd); } )*
    <EOF>
    { return node; }
  } catch (ParseException e) {
    registrarErro(e);
    if (getToken(1).kind != EOF) {
        getNextToken();
    }
    rest = Programa();
    if (rest != null) {
        for (ASTNode child : rest.getChildren()) {
            node.addChild(child);
        }
    }
    return node;
  }
}
```

### Mecanismo de Sincronização
Ao capturar a exceção, o parser executa duas tarefas essenciais:
1. **`registrarErro(ParseException e)`**: Registra o erro no console e infere quais tokens eram esperados a partir das sequências esperadas presentes no objeto da exceção. Também salva o número da linha na lista estática `listaLinhasErros` para que a IDE possa realçar as linhas defeituosas.
2. **`recuperarErro()`**: Desperdiça ("consome") tokens de forma iterativa até encontrar um caractere delimitador seguro (neste caso, o ponto e vírgula `;` ou uma chave de fechamento `}`). 

A rotina de recuperação real é implementada da seguinte forma:

```jj
  public static void recuperarErro() {
      Token t;
      while (true) {
          t = getToken(1);
          if (t.kind == EOF) {
              break;
          }
          if (t.kind == PONTO_VIRGULA) {
              getNextToken(); // consome ponto e vírgula para finalizar o comando atual
              break;
          }
          if (t.kind == FECHA_CHAVES) {
              // não consome o fecha chaves para que o escopo externo consiga tratar a saída do bloco
              break;
          }
          getNextToken(); // consome outros tokens inválidos ou fora de contexto
      }
  }
```

Dessa forma, o compilador ignora a linha problemática e retoma a compilação de maneira limpa logo no início do comando seguinte, registrando um nó especial `"Erro Sintático"` na árvore AST para identificação visual.

---

## 4. Interface Gráfica de Interação (A IDE CompilaMeme)

A IDE do CompilaMeme foi construída em CompilerGUI.java com componentes robustos da biblioteca Java Swing e configurada com uma identidade visual moderna voltada ao tema escuro.

```
+-------------------------------------------------------------+
|  IDE CompilaMeme - Toolbar (Compilar (F5) / Limpar Tudo)    |
+------------------------------+------------------------------+
|                              |                              |
|                              |   Abas:                      |
|   [Gutter] Editor de Código  |   [Árvore Sintática (JTree)] |
|   (JTextArea com Dark Theme) |   [Tokens (JTable)]          |
|                              |                              |
|                              |                              |
+------------------------------+------------------------------+
|  Console de Compilação (Status + Área de Logs interativa)   |
+-------------------------------------------------------------+
```

### Detalhamento dos Componentes Principais

1.  **Editor de Código (`txtEditor`):** Um painel de edição `JTextArea` configurado com fundo escuro (`COLOR_BG_EDITOR`) e fonte monoespaçada `Consolas` de tamanho 15px. Possui suporte nativo à tecla de atalho **F5** para disparar a compilação instantaneamente.
2.  **Régua Lateral de Linhas (Gutter):** A classe interna `LineNumberGutter` estende `JTextArea` e escuta eventos de alteração do documento no editor de código. Ela calcula e imprime verticalmente o número de cada linha em sincronia pixel a pixel com a rolagem do editor principal.
3.  **Tabela de Símbolos / Tokens (`tblTokens`):** Um componente `JTable` de leitura exclusiva contido em uma aba lateral, preenchido após a varredura léxica da fonte. Exibe quatro colunas básicas: **Linha**, **Coluna**, **Token/Tipo** (como definido nas constantes do parser) e o **Lexema** (texto de origem correspondente).
4.  **Console de Logs de Compilação (`txtErrors`):**
    *   **Indicador de Status:** Exibe mensagens claras destacadas com cores: verde para sucesso absoluto ("Aceito! Código sintaticamente correto") e vermelho quando há falhas léxicas ou sintáticas.
    *   **Navegação Rápida:** A área de logs possui um `MouseListener` inteligente. Quando o usuário clica sobre uma linha que detalha um erro (ex: `"Erro Sintático na Linha 12..."`), o listener extrai a informação numérica da linha e posiciona o cursor (`caret`) do editor de texto exatamente nessa coordenada, focando a janela no ponto do erro.
    *   **Realce de Erros:** O editor de código destaca as linhas de erros detectados aplicando máscaras translúcidas avermelhadas sobre elas por meio do `Highlighter` do Swing.

---

## 5. Geração e Visualização da Árvore Sintática (AST)

A estruturação semântica e hierárquica do código-fonte é capturada sob o modelo de árvore de nós após o sucesso do analisador.

### Estrutura do Nó da Árvore (`ASTNode.java`)
O arquivo ASTNode.java define a estrutura fundamental da nossa árvore sintática:

```java
public class ASTNode {
    private String name;
    private String value;
    private int line;
    private int column;
    private List<ASTNode> children;
    
    // Construtores, addChild(ASTNode child), getters e toString()
}
```

Cada nó armazena seu nome lógico (tipo do nó, como `"Declaracao"`, `"Atribuicao"`, `"Condicional"`), um valor léxico opcional associado (como o nome da variável ou sinal do operador), metadados de localização física no código-fonte (`line`, `column`) e referências para a lista de nós filhos (`children`).

### Conexão de Nós na Gramática (.jj)
Durante a execução de regras sintáticas no JavaCC, os objetos `ASTNode` são instanciados e vinculados de forma aninhada. 

Tomemos como exemplo o fluxo de um comando condicional `tem_certeza?` (o `if` da MemeLang):

```jj
ASTNode Condicional() :
{
  ASTNode node = new ASTNode("Condicional (tem_certeza?)");
  ASTNode expr = null;
  ASTNode cmd = null;
  ASTNode blockTrue = new ASTNode("Então");
  ASTNode blockFalse = new ASTNode("Senão");
}
{
  <T_SE> <ABRE_PAR> expr = Expressao() <FECHA_PAR> 
  { 
    ASTNode condNode = new ASTNode("Condição");
    condNode.addChild(expr);
    node.addChild(condNode); 
  }
  <ABRE_CHAVES> 
    ( cmd = Comando() { if (cmd != null) blockTrue.addChild(cmd); } )* 
  <FECHA_CHAVES>
  { node.addChild(blockTrue); }
  ( 
    <T_SENAO> 
    <ABRE_CHAVES> 
      ( cmd = Comando() { if (cmd != null) blockFalse.addChild(cmd); } )* 
    <FECHA_CHAVES>
    { node.addChild(blockFalse); }
  )?
  { return node; }
}
```

Nesse trecho, a regra:
1. Instancia o nó principal da estrutura condicional.
2. Instancia um nó `"Condição"` e anexa a expressão avaliada como seu filho.
3. Instancia os blocos `"Então"` (obrigatório) e `"Senão"` (opcional), preenchendo-os com os respectivos comandos que forem compilados dentro de suas chaves.
4. Retorna o nó estruturado de volta ao chamador, propagando a hierarquia recursivamente.

### Renderização com JTree do Swing
Uma vez concluída a análise do programa principal, a estrutura hierárquica baseada em `ASTNode` é entregue à IDE. 

No arquivo CompilerGUI.java, a tradução da árvore da gramática para os componentes gráficos do Swing é realizada pelo método `buildSwingTree`:

```java
    private DefaultMutableTreeNode buildSwingTree(ASTNode node) {
        if (node == null) return null;
        DefaultMutableTreeNode swingNode = new DefaultMutableTreeNode(node.toString());
        for (ASTNode child : node.getChildren()) {
            DefaultMutableTreeNode childSwingNode = buildSwingTree(child);
            if (childSwingNode != null) {
                swingNode.add(childSwingNode);
            }
        }
        return swingNode;
    }
```

O nó resultante `DefaultMutableTreeNode` é definido como o modelo do `JTree` (`treeSyntax`). Em seguida, a IDE dispara o método `expandAllNodes` para forçar todas as subpastas da árvore a se apresentarem expandidas na tela por padrão, garantindo que o programador veja a hierarquia inteira do seu programa MemeLang de maneira imediata e clara.

---
