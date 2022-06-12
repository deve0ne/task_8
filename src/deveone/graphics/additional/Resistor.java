package deveone.graphics.additional;

import java.awt.*;

public class Resistor {
        private final Vertex start;
        private final Vertex end;
        private final int weight;
        private Color color;

    public Resistor(Vertex start, Vertex end, int weight) {
        this.start = start;
        this.end = end;
        this.weight = weight;
        this.color = Color.black;
    }

    public Vertex getStart() {
        return start;
    }

    public Vertex getEnd() {
        return end;
    }

    public int getWeight() {
        return weight;
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
        if (!(o instanceof Resistor resistor)) return false;

        return (start.equals(resistor.start) && end.equals(resistor.end)) ||
                (start.equals(resistor.end) && end.equals(resistor.start));
    }
}
