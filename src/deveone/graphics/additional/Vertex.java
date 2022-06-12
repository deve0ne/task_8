package deveone.graphics.additional;

import java.awt.*;

public class Vertex {
    private Point pos;
    private int id;
    private Color color;

    public Vertex(Point pos, int id) {
        this.pos = pos;
        this.id = id;
        color = Color.black;
    }

    public Point getPos() {
        return pos;
    }

    public int getId() {
        return id;
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Vertex vertex)) return false;

        return getId() == vertex.getId();
    }
}
