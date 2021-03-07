import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Streams;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.apache.batik.dom.svg.SAXSVGDocumentFactory;
import org.apache.batik.parser.DefaultPathHandler;
import org.apache.batik.parser.ParseException;
import org.apache.batik.parser.PathParser;
import org.apache.batik.util.XMLResourceDescriptor;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.connectivity.ConnectivityInspector;
import org.jgrapht.alg.cycle.ChinesePostman;
import org.jgrapht.alg.cycle.StackBFSFundamentalCycleBasis;
import org.jgrapht.alg.interfaces.CycleBasisAlgorithm;
import org.jgrapht.graph.DefaultUndirectedGraph;
import org.jgrapht.traverse.DepthFirstIterator;
import org.w3c.dom.NodeList;
import org.w3c.dom.svg.SVGDocument;

public class EdgeCoverSvg {
  private static final String SVG_TMPL =
      """
      <?xml version="1.0" encoding="utf-8"?>
      <!DOCTYPE svg PUBLIC "-//W3C//DTD SVG 1.1//EN" "http://www.w3.org/Graphics/SVG/1.1/DTD/svg11.dtd">
      <svg version="1.1" xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" viewBox="0 0 2000 2000">
      <style>path { vector-effect: non-scaling-stroke; } </style>
      <g fill="none" stroke-width="0.6" stroke="#1a1a1a">
      %s
      </g>
      </svg>
      """;

  public static void main(String[] args) throws Exception {
    List<Segment> segments = getSegments(args[0]);
    DefaultUndirectedGraph<Point, Segment> g = new DefaultUndirectedGraph<>(null, null, false);
    segments.forEach(
        s -> {
          g.addVertex(s.a);
          g.addVertex(s.b);
          g.addEdge(s.a, s.b, s);
        });

//    DepthFirstIterator<Point, Segment> it = new DepthFirstIterator<>(g);
//    it.isCrossComponentTraversal()
    ConnectivityInspector<Point, Segment> ci = new ConnectivityInspector<>(g);
    List<Set<Point>> sets = ci.connectedSets().stream().sorted((o1, o2) -> -Integer.compare(o1.size(), o2.size())).collect(toList());
    ChinesePostman<Point, Segment> cp = new ChinesePostman<>();
//    StackBFSFundamentalCycleBasis<Point, Segment> cycles = new StackBFSFundamentalCycleBasis<>(g);
//    CycleBasisAlgorithm.CycleBasis<Point, Segment> basis = cycles.getCycleBasis();
//    List<GraphPath<Point, Segment>> blah = basis.getCyclesAsGraphPaths().stream().filter(p -> p.getLength() > 1).limit(20).collect(toList());
//    IntStream.range(0, blah.size()).forEach(i -> writeSvg(ImmutableList.of(blah.get(i).getVertexList()), String.format("out%02d.svg", i)));
    Path pp = Paths.get("").toAbsolutePath().getParent();
//    List<List<Point>> fuck = sets.get(0).stream().map(g::edgesOf).flatMap(Set::stream).collect(toSet()).stream().map(s -> ImmutableList.of(s.a, s.b)).collect(toList());
//    writeSvg(fuck, pp.resolve("out1.svg").toString());
//    fuck = sets.get(1).stream().map(g::edgesOf).flatMap(Set::stream).collect(toSet()).stream().map(s -> ImmutableList.of(s.a, s.b)).collect(toList());
//    writeSvg(fuck, pp.resolve("out2.svg").toString());
//    fuck = sets.get(2).stream().map(g::edgesOf).flatMap(Set::stream).collect(toSet()).stream().map(s -> ImmutableList.of(s.a, s.b)).collect(toList());
//    writeSvg(fuck, pp.resolve("out3.svg").toString());
//    writeSvg(ImmutableList.of(sets.get(1)), pp.resolve("out2.svg").toString());
//    writeSvg(ImmutableList.of(sets.get(2)), pp.resolve("out3.svg").toString());
    System.out.println();
    System.out.println();
//    run(g);
  }

  public static void run(DefaultUndirectedGraph<Point, Segment> g) throws Exception {
    TreeSet<Segment> remainingEdges =
        g.edgeSet().stream()
            .sorted()
            .collect((Supplier<TreeSet<Segment>>) TreeSet::new, TreeSet::add, TreeSet::addAll);
    Stack<List<Point>> paths = new Stack<>();
    paths.ensureCapacity(5000);

    while (!remainingEdges.isEmpty()) {
      Point v = remainingEdges.stream().findFirst().get().a;
      paths.push(new ArrayList<>());

      while (true) {
        paths.peek().add(v);
        Optional<Segment> e =
            g.edgesOf(v).stream().filter(remainingEdges::contains).sorted().findFirst();
        if (e.isPresent()) {
          remainingEdges.remove(e.get());
          v = e.get().a.equals(v) ? e.get().b : e.get().a;
        } else {
          if (paths.peek().size() < 2) {
            paths.pop();
          }
          break;
        }
      }
    }

    writeSvg(paths);
    System.out.println();
  }

  private static List<Segment> getSegments(String filepath) throws IOException {
    SVGDocument doc =
        (SVGDocument)
            new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName())
                .createDocument(Path.of(filepath).toUri().toString());
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

  private static void writeSvg(Collection<List<Point>> paths) {
    writeSvg(paths, Paths.get("").toAbsolutePath().getParent().resolve("out.svg").toString());
  }

  private static void writeSvg(Collection<List<Point>> paths, String outPath) {
    String pathTags = paths.stream()
        .map(
            path ->
                String.format(
                    "<path d=\"M%.4f %.4f %s\"/>",
                    path.get(0).x / 10000.0,
                    path.get(0).y / 10000.0,
                    path.stream()
                        .skip(1)
                        .map(p -> String.format("%.3f %.3f", p.x / 10000.0, p.y / 10000.0))
                        .collect(joining(","))))
        .collect(joining("\n"));
    try {
    FileWriter writer = new FileWriter(outPath);
    writer.write(SVG_TMPL.formatted(pathTags));
    writer.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
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
      paths.get(paths.size() - 1).add(Point.of((long)(x*10000), (long)(y*10000)));
    }
  }

  @AllArgsConstructor(staticName = "of")
  static class Point implements Comparable<Point> {
    public long x, y;

    @Override
    public String toString() {
      // return String.format("(%.1f,%.1f)", x, y);
      return String.format("(%d,%d)", (long) x, (long) y);
    }

    @Override
    public int compareTo(Point o) {
      long x1 = this.x / 1000, y1 = this.y / 1000, x2 = o.x / 1000, y2 = o.y / 1000;
      return x1 == x2 ? Long.compare(y1, y2) : Long.compare(x1, x2);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof Point)) return false;
      Point p = (Point) o;
      long x1 = this.x / 1000, y1 = this.y / 1000, x2 = p.x / 1000, y2 = p.y / 1000;
      return x1 == x2 && y1 == y2;
    }

    @Override
    public int hashCode() {
      return Objects.hash(x / 1000, y / 1000);
    }
  }

  @AllArgsConstructor(staticName = "of")
  static class Segment implements Comparable<Segment> {
    public Point a, b;

    @Override
    public String toString() {
      return String.format("%s-%s", a.toString(), b.toString());
    }

    @Override
    public int compareTo(Segment o) {
      int aa = a.compareTo(o.a);
      return aa == 0 ? b.compareTo(o.b) : aa;
    }
  }
}
