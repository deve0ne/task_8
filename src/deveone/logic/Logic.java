package deveone.logic;

import deveone.logic.graph.*;

import java.util.*;

public class Logic {
    /**
     * Проверяет, является ли вершина графа началом параллельного соединения.
     * Для этого считает количество выходящих соединений. Если соединений > 1, следовательно, является началом.
     *
     * @param vToCheck Вершина для проверки
     * @param graph    Граф, в котором будет осуществлена проверка
     */
    private static boolean isParallelStart(Integer vToCheck, Graph graph) {
        Iterable<Integer> adj = graph.adjacency(vToCheck);

        int count = 0;
        for (Integer ignored : adj)
            count++;

        return count > 1;
    }

    /**
     * Возвращает список параллельных соединений. Последнее соединение не будет содержать последовательных соединений.
     *
     * @param graph граф
     * @param from  Вершина, с которой начинается поиск
     * @return
     */
    private static int findParallelConnection(Graph graph, int from) {
        boolean[] visited = new boolean[graph.vertexCount()];
        Queue<Integer> queue = new LinkedList<>();
        LinkedList<Integer> parallelList = new LinkedList<>();
        queue.add(from);
        visited[from] = true;
        while (queue.size() > 0) {
            Integer curr = queue.remove();
            if (isParallelStart(curr, graph))
                parallelList.add(curr);
            for (Integer v : graph.adjacency(curr)) {
                if (!visited[v]) {
                    queue.add(v);
                    visited[v] = true;
                }
            }
        }

        return parallelList.isEmpty() ? -1 : parallelList.getLast();
    }

    private static void getAllPaths(Graph graph, List<List<Integer>> result, List<Integer> path, int start, int finish, boolean[] visited) {
        visited[start] = true; //Отмечаем текущую вершину посещённой.
        path.add(start);

        if (start != finish) {
            //Сохраняем в памяти путь до текущей вершины. Когда поиск вернётся в эту вершину, восстановим этот путь.
            LinkedList<Integer> tempPath = new LinkedList<>(path);
            //Сохраняем посещённые вершины по аналогии.
            boolean[] tempVisited = visited.clone();

            for (int v : graph.adjacency(start)) {
                if (!visited[v]) {
                    getAllPaths(graph, result, path, v, finish, visited);

                    //Возвращаем путь и посещённые вершины к исходным значениям для текущей вершины.
                    //Если поиск вглубь найдёт корректный путь, этот путь уже будет записан в result,
                    //так что мы ничего не теряем.
                    path = new LinkedList<>(tempPath);
                    visited = tempVisited.clone();
                }
            }
        } else {
            result.add(path);
        }
    }

    private static AdjMatrixWeightedDigraph stripCircuit(WeightedGraph graph, int startNode, int endNode) {
        List<List<Integer>> result = new ArrayList<>();
        boolean[] visited = new boolean[graph.vertexCount()];
        getAllPaths(graph, result, new ArrayList<>(), startNode, endNode, visited);

        AdjMatrixWeightedDigraph newGraph = new AdjMatrixWeightedDigraph();
        for (List<Integer> path : result)
            for (int i = 0; i < path.size() - 1; i++) {
                int vertex1 = path.get(i);
                int vertex2 = path.get(i + 1);
                if (!newGraph.isAdj(vertex2, vertex1)) //Избегаем неориентированных ребер
                    newGraph.addEdge(vertex1, vertex2, graph.getWeight(vertex1, vertex2));
            }

        return newGraph;
    }

    /**
     * Упрощение параллельного соединения
     *
     * @param adjMatrix - матрица смежности
     * @param resistors - сопротивления резисторов
     * @param curr      - текущая вершина, начало параллельного соединения
     */
    private static void simplifyParallel(boolean[][] adjMatrix, double[][] resistors, int curr) {
        int parralelEnd = curr;

        //Будем искать вершину, которая будет концом параллельного соединения, т.е. в которую будут сходиться прочие ребра параллельного соединения
        for (int i = 0; i < adjMatrix[curr].length; i++) {

            for (int j = 0; j < adjMatrix[curr].length; j++) { //Ищем конец пар-го соединения
                if (adjMatrix[curr][j]) {
                    parralelEnd = searchForEndOfSerial(adjMatrix, j);
                    break;
                }
            }

            //Для каждой смежной вершины упрощаем цепочку последовательных соединений, начало коей лежит в i, в один резистор
            if (adjMatrix[curr][i]) {
                double serialResistance = 0;
                double parallelResistance;
                if (i != parralelEnd)
                    serialResistance = simplifySerial(adjMatrix, resistors, i, parralelEnd);

                double serialSum = resistors[curr][i] + serialResistance;
//                parallelResistance = serialSum != 0 ? 1 / (serialSum) : 0;
                parallelResistance =  1 / serialSum;
                adjMatrix[curr][i] = false;

                if (adjMatrix[curr][parralelEnd])
                    parallelResistance += 1 / resistors[curr][parralelEnd];

                resistors[curr][parralelEnd] = 1 / parallelResistance;
                adjMatrix[curr][parralelEnd] = true;
            }
        }
    }

    /**
     * Упрощение последовательного соединения
     *
     * @param adjMatrix - матрица смежности
     * @param resistors - значения резисторов
     * @param curr      - текущая вершина
     * @return Проблемный метод, хотя по логике не сложный
     */
    private static double simplifySerial(boolean[][] adjMatrix, double[][] resistors, int curr, int endOfSerial) {
        //Начало, откуда будем двигаться дальше, это же номер вершины-начала нового резистора, который будет суммой всех последующих

        //Конечный индекс, который будем соединять с начальным, образуя тем самым новый резистор
        double resistance = 0;

        //Пока текущая вершина не является окончанием последовательного участка цепи, т.е. если из нее исходит более одного ребра(начало параллельного соединения)
        //или если в нее направлено более одного ребра(точка окончания параллельного соединения)
        while (endOfSerial != curr) {


            int whereWeWantToMove = 0; //Индекс вершины, куда мы хотим попасть, т.е. следующей вершины в последовательном соединении
            for (int i = 0; i < adjMatrix[curr].length; i++) { //Ищем его
                if (adjMatrix[curr][i]) {
                    whereWeWantToMove = i;
                    break;
                }
            }
            //Прибавляем к значению сопротивления значение между текущей вершиной и вершиной, куда мы хотим попасть
            resistance += resistors[curr][whereWeWantToMove];

            adjMatrix[curr][whereWeWantToMove] = false; //Удаляем старый резистор
            curr = whereWeWantToMove; //И двигаемся дальше
        }
        return resistance;
    }

    /**
     * Поиск индекса, на котором окончится последовательное соединение соединение
     *
     * @param adjMatrix - матрица
     * @param curr      - текущий индекс
     * @return возвращает конец последовательного соединения
     */
    private static int searchForEndOfSerial(boolean[][] adjMatrix, int curr) {
        //Индекс следующей вершины, куда стремимся
        int next = curr;
        //Счетчики входящих и выходящих в/из верш. next
        int countOfInput = 0;
        int countOfOutput = 0;
        //Ищем индекс последующей ячейки
        for (int i = 0; i < adjMatrix[curr].length; i++) {
            if (adjMatrix[curr][i]) {
                next = i;
                break;
            }
        }

        //Ищем число ребер, входящих или выходящих из
        for (int i = 0; i < adjMatrix[next].length; i++) {
            if (adjMatrix[i][next])
                countOfInput++;

            if (adjMatrix[next][i])
                countOfOutput++;
        }
        //Если один из этих счетчиков больше единицы, то вершина next - это последняя вершина в последовательном соединении, возвращаем ее
        if (countOfInput > 1 || countOfOutput > 1)
            return next;

            //Если же в вершину next можно попасть только через curr и next, в свою очередь, ведет только в одну вершину,
            // то можем двигаться дальше и рекурсивно вызываем алгоритм для вершины next
        else if (countOfInput == 1 || countOfOutput == 1) {
            if (next == curr)
                return curr;
            else
                return searchForEndOfSerial(adjMatrix, next);
        } else //Если же счетчик остался равен нулю, то это конец графа, и текущая вершина - конец соединения
            return curr;
    }

    private static boolean checkHavePath(WeightedGraph inputGraph, int startNode, int endNode, boolean[] visited) {
        if (inputGraph.vertexCount() < startNode || inputGraph.vertexCount() < endNode || startNode == endNode)
            return false;

        boolean havePath = false;
        visited[startNode] = true;

        for (Integer v : inputGraph.adjacency(startNode)) {
            if (v == endNode)
                havePath = true;
            else if (!visited[v])
                if (checkHavePath(inputGraph, v, endNode, visited))
                    havePath = true;
        }

        return havePath;
    }


    public static double calcCircuitResistance(WeightedGraph inputGraph, int startNode, int endNode) {
        if (!checkHavePath(inputGraph, startNode, endNode, new boolean[inputGraph.vertexCount()]))
            return -1;

        AdjMatrixWeightedDigraph digraph = stripCircuit(inputGraph, startNode, endNode);

        while (findParallelConnection(digraph, startNode) != -1) {
            boolean[][] tempAdjMatrix = digraph.getBooleanAdjMatrix();
            double[][] tempWeightsMatrix = digraph.getWeightsMatrix();
            simplifyParallel(tempAdjMatrix, tempWeightsMatrix, findParallelConnection(digraph, startNode));
            digraph = new AdjMatrixWeightedDigraph(tempAdjMatrix, tempWeightsMatrix);
        }

        return simplifySerial(digraph.getBooleanAdjMatrix(), digraph.getWeightsMatrix(), startNode, endNode);
    }
}

