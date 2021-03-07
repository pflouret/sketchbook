import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

import com.google.common.collect.Streams;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.stream.IntStream;
import jdrasil.graph.Graph;
import jdrasil.graph.GraphFactory;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.parser.DefaultPathHandler;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PathParser;
import org.apache.batik.util.XMLResourceDescriptor;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;

public class TreeWalk {
  enum Color {
    WHITE,
    GREY,
    BLACK
  }

  public static void main(String[] args) throws Exception {
    List<Segment> segments = getSegments();
    Graph<Point> g = GraphFactory.emptyGraph();
    segments.forEach(
        s -> {
          g.addVertex(s.a);
          g.addVertex(s.b);
          g.addEdge(s.a, s.b);
        });

    Set<Point> todo = new TreeSet<>(g.getCopyOfVertices());
    Map<Point, Color> colors = new HashMap<>();
    Stack<List<Point>> paths = new Stack<>();
    paths.ensureCapacity(5000);

    while (!todo.isEmpty()) {
      Point v = todo.stream().findFirst().get();
      Point vi = v;
      paths.push(new ArrayList<>());

      while (true) {
        colors.put(v, Color.BLACK);
        paths.peek().add(v);
        Map<Color, List<Point>> neighbors = g.getNeighbourhoodAsList(v).stream()
            .filter(todo::contains)
            .collect(groupingBy(n -> colors.getOrDefault(n, Color.WHITE)));
        List<Point> whiteNeighbors = neighbors.get(Color.WHITE);

        if (whiteNeighbors == null) {
          if (neighbors.get(Color.GREY) == null) {
            todo.remove(v);
          }
          if (v == vi) {
            paths.pop();
          }
          break;
        } else if (whiteNeighbors.size() == 1) {
          if (neighbors.get(Color.GREY) == null) {
            todo.remove(v);
          }
          v = whiteNeighbors.get(0);
        } else {
          colors.put(v, Color.GREY);
          v = whiteNeighbors.get(0);
        }
        System.out.println(paths.peek());
        System.out.println();
      }

      colors.clear();
    }

    paths.forEach(System.out::println);

    System.out.println();
  }

  private static List<Segment> getSegments() throws IOException {
    SVGDocument doc =
        (SVGDocument)
            new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName())
                .createDocument(
                    // "file:///Users/pablo_flouret/Code/sketchbook/projects/svg-maps/svg/ba-withres.svg");
                    "file:///Users/pablo_flouret/Code/sketchbook/projects/svg-maps/test.svg");
    SimplePathHandler handler = new SimplePathHandler();
    PathParser pp = new PathParser();
    pp.setPathHandler(handler);

    NodeList elements = doc.getDocumentElement().getElementsByTagName("path");
    IntStream.range(0, elements.getLength())
        .forEach(i -> pp.parse(elements.item(i).getAttributes().getNamedItem("d").getNodeValue()));

    return handler.paths.stream()
        .map(p -> Streams.zip(p.stream(), p.stream().skip(1), Segment::of).collect(toList()))
        .flatMap(List::stream)
        .collect(toList());
  }

  @NoArgsConstructor
  static class SimplePathHandler extends DefaultPathHandler {
    public List<List<Point>> paths = new ArrayList<>();

    @Override
    public void startPath() throws ParseException {
      paths.add(new ArrayList<>());
    }

    @Override
    public void movetoAbs(float x, float y) throws ParseException {
      add(x, y);
    }

    @Override
    public void linetoAbs(float x, float y) throws ParseException {
      add(x, y);
    }

    private void add(float x, float y) {
      paths.get(paths.size() - 1).add(Point.of(x, y));
    }
  }

  @AllArgsConstructor(staticName = "of")
  @EqualsAndHashCode
  static class Point implements Comparable<Point> {
    public float x, y;

    @Override
    public String toString() {
      // return String.format("(%.1f,%.1f)", x, y);
      return String.format("(%d,%d)", (int) x, (int) y);
    }

    @Override
    public int compareTo(Point o) {
      return x == o.x ? Float.compare(y, o.y) : Float.compare(x, o.x);
    }
  }

  @AllArgsConstructor(staticName = "of")
  static class Segment {
    public Point a, b;

    @Override
    public String toString() {
      return String.format("%s-%s", a.toString(), b.toString());
    }
  }
}
