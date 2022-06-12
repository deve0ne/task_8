package deveone.graphics;

import deveone.graphics.additional.ManipulateModes;
import deveone.graphics.additional.ObjectModes;
import deveone.graphics.additional.Resistor;
import deveone.graphics.additional.Vertex;
import deveone.logic.graph.AdjMatrixWeightedGraph;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.ArrayList;

public class Canvas extends JComponent {
    private Graphics2D g;
    private Image img;

    private final int vertexSize = 60;

    private final ArrayList<Vertex> graphicalVertices = new ArrayList<>(); //Массив вершин
    private final ArrayList<Resistor> graphicalResistors = new ArrayList<>();

    private Vertex[] linesBuffer = new Vertex[2]; //Хранит информацию о точках линии во время её создания

    private AdjMatrixWeightedGraph graph = new AdjMatrixWeightedGraph(); //Матрица смежности вершин

    private ObjectModes objectMode = ObjectModes.VERTEX; //Текущий режим (вершины или линии)
    private ManipulateModes manipulateMode = ManipulateModes.ADD;

    private int currId = 0;
    private int weight = 0; //Текущий вес ребра, задаётся извне


    public Canvas() {
        createMouseListener();
    }

    public void changeObjectMode(ObjectModes newMode) {
        objectMode = newMode;
    }

    public void changeManipulateMode(ManipulateModes newMode) {
        manipulateMode = newMode;
    }

    public void setWeight(int newWeight) {
        weight = newWeight;
    }

    public AdjMatrixWeightedGraph getGraph() {
        return graph;
    }

    private void createMouseListener() {
        setDoubleBuffered(false);

        MouseListener listener = new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                Point clickPoint = new Point(e.getX(), e.getY());
                handleClick(clickPoint);
            }
        };

        addMouseListener(listener);
    }

    private Vertex checkVertexCollision(Point clicked) {
        int offsetRectSize = vertexSize * 2;

        for (Vertex vertex : graphicalVertices) {
            Ellipse2D r = new Ellipse2D.Float(vertex.getPos().x - vertexSize, vertex.getPos().y - vertexSize,
                    offsetRectSize, offsetRectSize);

            if (r.contains(clicked))
                return vertex;
        }
        return null;
    }

    private Resistor checkResistorCollision(Point clicked) {
        for (Resistor resistor : graphicalResistors) {
            Line2D.Float line = new Line2D.Float(resistor.getStart().getPos(), resistor.getEnd().getPos());
            if (line.getBounds().contains(clicked))
                return resistor;
        }
        return null;
    }

    private void handleClick(Point clickPoint) {
        clearColors(); //Для удаления фокуса

        if (manipulateMode == ManipulateModes.ADD)
            handleAddModeClick(clickPoint);
        else
            handleDelModeClick(clickPoint);
    }

    private void handleAddModeClick(Point clickPoint) {
        Vertex v = checkVertexCollision(clickPoint);

        if (objectMode == ObjectModes.VERTEX) { //Режим вершин
            if (v != null) //Запрещаем рисовать вершины на существующих вершинах.
                return;

            createVertex(clickPoint);
        } else { //Режим линий
            if (v == null) //Запрещаем создавать линии там, где нет вершин
                return;

            if (linesBuffer[0] == null || linesBuffer[0].equals(v)) {
                linesBuffer[0] = v; //Записывает первую точку линии в буфер
                v.setColor(Color.GRAY); // фокус
            } else {
                linesBuffer[1] = v;

                createResistor(linesBuffer[0], linesBuffer[1]);

                linesBuffer = new Vertex[2];
            }

            redraw();
        }
    }

    private void handleDelModeClick(Point clickPoint) {
        if (objectMode == ObjectModes.VERTEX) { //Режим вершин
            Vertex vertexToRemove = checkVertexCollision(clickPoint);

            if (vertexToRemove == null) //Запрещаем пытаться удалять несуществующие вершины.
                return;

            removeVertex(vertexToRemove);
        } else {
            Resistor resistorToRemove = checkResistorCollision(clickPoint);

            if (resistorToRemove == null) //Запрещаем пытаться удалять несуществующие резисторы
                return;

            removeResistor(resistorToRemove);
        }
    }

    //Записывает вершину в список и рисует её
    private void createVertex(Point p) {
        Vertex vertex = new Vertex(p, currId++);
        graphicalVertices.add(vertex);
        redraw();
    }

    private void removeVertex(Vertex vertexToRemove) {
        Iterable<Integer> adj = graph.adjacency(vertexToRemove.getId());
        for (Integer vertex : adj) {
            graph.removeEdge(vertex, vertexToRemove.getId());
        }
//        graph.removeEdge(vertexToRemove);

        graphicalVertices.remove(vertexToRemove);

        for (int i = 0; i < graphicalResistors.size(); i++) {
            Resistor resistor = graphicalResistors.get(i);

            if (resistor.getStart() == vertexToRemove || resistor.getEnd() == vertexToRemove) {
                removeResistor(resistor);
                i--;
            }
        }

        redraw();
    }

    //Рисует вершину(без записи в список)
    private void drawVertex(Vertex v) {
        int halfSize = vertexSize / 2;
        int drawX = v.getPos().x - halfSize;
        int drawY = v.getPos().y - halfSize;

        g.setStroke(new BasicStroke(4));

        g.setColor(Color.WHITE);
        g.fillOval(drawX, drawY, vertexSize, vertexSize);
        g.setColor(v.getColor());
        g.drawOval(drawX, drawY, vertexSize, vertexSize);

        String strId = String.valueOf(v.getId());
        Rectangle2D stringBounds = g.getFontMetrics(g.getFont()).getStringBounds(strId, g);

        g.setColor(Color.BLACK);
        g.drawString(strId, v.getPos().x - (int) stringBounds.getCenterX(), v.getPos().y - (int) stringBounds.getCenterY());
    }

    //Рисует резистор между двумя точками
    private void createResistor(Vertex start, Vertex end) {
        Resistor resistor = new Resistor(start, end, weight);

        if (graphicalResistors.contains(resistor))
            return;

        graphicalResistors.add(resistor);
        graph.addEdge(start.getId(), end.getId(), weight);

        redraw();
    }

    private void removeResistor(Resistor resistorToRemove) {
        graph.removeEdge(resistorToRemove.getStart().getId(), resistorToRemove.getEnd().getId());
        graphicalResistors.remove(resistorToRemove);

        redraw();
    }

    private void drawResistor(Resistor resistor) {
        Line2D.Float line = new Line2D.Float(resistor.getStart().getPos(), resistor.getEnd().getPos());

        //Рисуем линию
        g.setColor(resistor.getColor());
        g.draw(line);

        Rectangle bounds = line.getBounds();
        int centerX = (int) bounds.getCenterX();
        int centerY = (int) bounds.getCenterY();

        //Вычисляем поворот резистора
        double theta = Math.atan2(resistor.getEnd().getPos().y - resistor.getStart().getPos().y,
                resistor.getEnd().getPos().x - resistor.getStart().getPos().x);
        AffineTransform rotation = new AffineTransform();
        rotation.rotate(theta, centerX, centerY);

        int resistorSize = 60;
        Rectangle r = new Rectangle(centerX - resistorSize / 2, centerY - resistorSize / 4, resistorSize, resistorSize / 2);
        Shape resistorBody = rotation.createTransformedShape(r);


        if (resistor.getWeight() != 0) {
            //Рисуем резистор
            g.setColor(Color.WHITE);
            g.fill(resistorBody);
            g.setColor(resistor.getColor());
            g.draw(resistorBody);

            //Рисуем сопротивление резиcтора
            String strWeight = String.valueOf(resistor.getWeight());
            Rectangle2D stringBounds = g.getFontMetrics(g.getFont()).getStringBounds(strWeight, g);

            g.setColor(Color.BLACK);
            g.drawString(strWeight, (int) (centerX - stringBounds.getCenterX()), (int) (centerY - stringBounds.getCenterY()));
        }
    }

    private void redraw() {
        clear();
        redrawResistors();
        redrawVertices();

        if (g != null)
            repaint();
    }

    private void clearColors() {
        graphicalVertices.forEach(o -> o.setColor(Color.black));
        graphicalResistors.forEach(o -> o.setColor(Color.black));
    }

    private void redrawVertices() {
        for (Vertex v : graphicalVertices)
            drawVertex(v);
    }

    private void redrawResistors() {
        for (Resistor resistor : graphicalResistors)
            drawResistor(resistor);
    }

    @Override
    protected void paintComponent(Graphics g1) {
        super.paintComponent(g);

        if (img == null) {
            img = createImage(getSize().width, getSize().height);

            g = (Graphics2D) img.getGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                    RenderingHints.VALUE_ANTIALIAS_ON);

            Font font = g.getFont().deriveFont(Font.PLAIN, 24);
            g.setFont(font);

            clear();
        }

        g1.drawImage(img, 0, 0, null);
    }

    public void clear() {
        g.setPaint(Color.white);
        g.fillRect(0, 0, getSize().width, getSize().height);
        g.setPaint(Color.black);
        repaint();
    }

    //Удаляет
    public void clearAll() {
        currId = 0;
        graph = new AdjMatrixWeightedGraph();
        graphicalVertices.clear();
        graphicalResistors.clear();
        clear();
    }
}
