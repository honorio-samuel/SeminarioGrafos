# Planaridade de Grafos e Projeto de Circuitos Impressos (Algoritmo DMP)

Projeto do seminário de **Teoria dos Grafos**: dado um grafo que representa componentes (vértices) e
trilhas (arestas) de uma placa de circuito impresso, o programa decide se ele é **planar**, isto é,
se pode ser desenhado **sem cruzamento de trilhas em uma única camada**, usando o algoritmo
**DMP (Demoucron, Malgrange e Pertuiset)**. Tudo (código, logs, comentários) está em português.

## 1. Visão geral e divisão modular

```
src/main/java/br/edu/grafos/dmp/
├── modelo/      Grafo, Face, Caminho, Fragmento, SubgrafoPlanar, ResultadoFiltro, ResultadoPlanaridade
├── algoritmo/   AlgoritmoDMP (filtroEuler, cicloInicial, fragmentos, loop de decisão), Blocos (Tarjan)
├── util/        LogDidatico (log passo a passo)
├── visual/      Layout, RenderizadorPasso, Visualizador (janela Swing), ExportadorPng
└── demo/        Cenarios (os 4 grafos do Slide 16), Main (texto) e MainVisual (janela)
src/test/java/...  CenariosSeminarioTest, CasosDeBordaTest, EtapasDmpTest (JUnit 5)
```

| Etapa do slide | Onde está |
|---|---|
| `filtroEuler(Grafo g)` - confere `E <= 3V - 6` em O(1) | `AlgoritmoDMP.filtroEuler` |
| `cicloInicial(Grafo g)` - ciclo base por DFS, H com 2 faces | `AlgoritmoDMP.cicloInicial` + `SubgrafoPlanar.deCiclo` |
| `fragmentos(Grafo g, SubgrafoPlanar h)` - fragmentos, contatos, faces admissíveis | `AlgoritmoDMP.fragmentos` |
| `dividirFace(Face f, Caminho p)` - insere o caminho e divide a face | `SubgrafoPlanar.dividirFace` |
| Regra de decisão (0 / 1 / 2+ faces admissíveis) | loop em `AlgoritmoDMP.testarBloco` |

**Regra de decisão em cada rodada:** se algum fragmento tem **0** faces admissíveis, para e declara
**Não Planar**; se o menor número é **1**, a escolha é **forçada**; se todos têm **2 ou mais**, a escolha
é **livre**. Sempre se processa o fragmento com **menos** faces admissíveis (reduz ramificações).

**Casos de borda tratados:** grafo nulo/vazio, 1 ou 2 vértices, árvores e florestas (planares,
acíclicas), vértices isolados, grafos desconexos (cada componente é avaliado separadamente e
`F = E - V + c + 1`), vértices de corte (o DMP roda em cada **bloco 2-conexo**, via Tarjan) e entrada
suja (**laços e arestas paralelas são ignorados** e contados antes do filtro de Euler).

### Sugestão de divisão do trabalho em grupo

| Integrante | Área | Arquivos |
|---|---|---|
| 1 | Modelo de grafo e limpeza de entrada | `Grafo`, `ResultadoFiltro` |
| 2 | Faces, caminhos e divisão de face | `Face`, `Caminho`, `SubgrafoPlanar` |
| 3 | Filtro de Euler, ciclo inicial, blocos | `AlgoritmoDMP` (etapas 1-2), `Blocos` |
| 4 | Fragmentos e regra de decisão | `Fragmento`, `AlgoritmoDMP` (etapas 3-4) |
| 5 | Demonstração, log e testes | `Main`, `Cenarios`, `LogDidatico`, `src/test` |
| 6 (opcional) | Visualização gráfica | pacote `visual`, `MainVisual` |

## 2. Pré-requisitos, clonagem, build e execução

- **JDK 17 ou superior** (`java -version`)
- **Maven 3.8+** (`mvn -version`)
- Git

```bash
git clone https://github.com/<usuario-ou-org>/dmp-circuitos-impressos.git
cd dmp-circuitos-impressos

mvn compile                 # compila
mvn test                    # roda os testes JUnit 5
mvn package                 # gera target/dmp-circuitos-impressos-1.0.0.jar
```

### Visualização gráfica (recomendada para a apresentação)

Desenha o grafo passo a passo: **H em preto**, **fragmentos tracejados e coloridos**, **contatos com anel**,
**caminho escolhido em destaque**, faces ao lado e, no fim, o desenho final (sem cruzamentos se for planar).

```bash
mvn -q compile exec:java -Dexec.mainClass=br.edu.grafos.dmp.demo.MainVisual   # janela com os 4 cenários
mvn -q compile exec:java -Dexec.args="--png"   # grava imagens em ./imagens/cenarioN/passo_XX.png (para os slides)
```

Na IDE basta rodar a classe `demo/MainVisual` (sem argumentos). Na janela: **→ / espaço** avança, **←** volta e
o seletor troca de cenário. O desenho usa um layout automático só para visualizar; a decisão de planaridade
é sempre do DMP.

### Executando a demonstração em texto (`Main.java`)

```bash
mvn -q compile exec:java                          # os 4 cenários com log completo
mvn -q compile exec:java -Dexec.args="3"          # só o cenário 3 (K3,3)
mvn -q compile exec:java -Dexec.args="--pausar"   # espera ENTER entre cenários (ideal ao vivo)
mvn -q compile exec:java -Dexec.args="--silencioso"  # sem log passo a passo, só o resumo

# ou, após o mvn package:
java -jar target/dmp-circuitos-impressos-1.0.0.jar --pausar
```

> **Windows:** se os acentos aparecerem quebrados no terminal, rode `chcp 65001` antes.

### VS Code
1. Instale o **Extension Pack for Java** e abra a pasta do projeto (ele detecta o `pom.xml`).
2. Abra `demo/Main.java` e clique em **Run** acima do `main`. Os testes aparecem no painel *Testing*.
3. Para passar argumentos, use `.vscode/launch.json` (campo `args`) ou o terminal com `mvn exec:java`.

### IntelliJ IDEA
1. *File → Open* e selecione o `pom.xml` (**Open as Project**); aguarde a importação do Maven.
2. Confirme o SDK em *File → Project Structure → Project SDK = 17+*.
3. Clique com o direito em `Main` → *Run 'Main.main()'*; para testes, clique na pasta `src/test/java` →
   *Run 'All Tests'*. Argumentos: *Run → Edit Configurations → Program arguments*.

## 3. Os 4 cenários da apresentação (Slide 16)

| # | Cenário | O que mostra | Resultado esperado |
|---|---|---|---|
| 1 | **Planar Enganoso** | hexágono (V=6, E=9) com cordas que parecem se cruzar | **Planar**, com a lista de 5 faces finais |
| 2 | **K5** | V=5, E=10 > 3(5)-6 = 9 | **Não Planar** barrado no filtro de Euler |
| 3 | **K3,3** (3 MCUs x 3 Sensores) | V=6, E=9 <= 12 passa em Euler | **Não Planar**: fragmento com 0 faces admissíveis no DMP |
| 4 | **PCB simulada** | 8 componentes, 14 trilhas | **Planar**: roteável em camada única |

Roteiro de fala sugerido: *(1)* mostrar que a aparência de cruzamento não decide nada; *(2)* Euler como
filtro barato, mas só **condição necessária**; *(3)* o caso em que Euler deixa passar e só o DMP
encontra a obstrução; *(4)* o caso prático: "essa placa cabe em uma camada".

Interpretação para PCB: **Planar** = camada única, sem vias. **Não Planar** = pelo menos um cruzamento
inevitável, então é preciso outra camada, uma via/jumper ou reorganizar o leiaute.

## 4. Fluxo de branches e Pull Requests

1. `main` é protegida: ninguém dá push direto (configure em *Settings → Branches*).
2. Antes de começar: `git checkout main && git pull`.
3. Crie uma branch por tarefa, com nome curto: `git checkout -b feat/fragmentos` (ou `fix/...`, `docs/...`, `test/...`).
4. Faça commits pequenos e claros em português: `git commit -m "Implementa fragmentos do tipo corda"`.
5. Antes de abrir o PR, rode `mvn test` e traga as novidades: `git fetch && git merge origin/main`.
6. `git push -u origin feat/fragmentos` e abra o **Pull Request** no GitHub descrevendo o que mudou.
7. Pelo menos **um colega** revisa; o CI (`.github/workflows/ci.yml`) roda `mvn test` automaticamente.
8. Merge (preferencialmente *Squash and merge*), apague a branch e atualize sua `main` local.

Para evitar conflitos: **um integrante por arquivo** (veja a tabela de divisão), PRs pequenos e frequentes,
e avise o grupo antes de mexer em arquivos de outra pessoa (principalmente `AlgoritmoDMP.java`).

## 5. Limitações conhecidas

- O DMP aqui usa recursão (DFS/Tarjan); grafos com dezenas de milhares de vértices em cadeia podem
  estourar a pilha. Para PCBs didáticas isso não é um problema.
- Na escolha livre usa-se a primeira face admissível. O DMP garante que qualquer escolha preserva a
  planaridade quando nenhum fragmento tem 0 faces admissíveis.
- O programa decide a planaridade e lista as faces; ele não gera coordenadas de desenho.
