package _experiments;

import pfp5.ProcessingApp;
import processing.core.PApplet;
import toxi.geom.Circle;
import toxi.geom.PointQuadtree;
import toxi.geom.ReadonlyVec2D;
import toxi.geom.Vec2D;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Diff extends ProcessingApp {
	private static final int N_NODES = 1000;

	private PointQuadtree spatialIndex;
	private Nodes nodes = new Nodes();

	@Override
	public void setup() {
		super.setup();

		spatialIndex = new PointQuadtree(0, 0, width, height);

		List<Vec2D> vertices = new Circle(0, 0, 10).toPolygon2D(30).vertices;
		for (int i=0; i < vertices.size(); i++) {
			nodes.add(new Node(vertices.get(i)));
			nodes.addAdj(i, (i+1) % vertices.size());
		}
	}

	public void drawNode(Node n) {
		pushStyle();;
		strokeWeight(4); stroke(0, 100); fill(0, 240);
		ellipse(n.x, n.y, 4, 4);
		popStyle();
	}

	public void drawEdge(Node a, Node b) {
		pushStyle();
		strokeWeight(1); stroke(255, 0, 0, 20); noFill();
		line(a.x, a.y, b.x, b.y);
		popStyle();
	}

	@Override
	public void draw() {
		clear();

		pushMatrix();
		translate(width/2, height/2);

		nodes.nodeWithAdjIterator().forEachRemaining((e) -> {
			drawNode(e.getKey());
			drawEdge(e.getKey(), e.getValue());
		});

		//noLoop();

		updateNodes();
		popMatrix();
	}

	private void updateNodes() {
		nodes.keySet().forEach(n -> n.scaleSelf(1.02f));

		nodes.keySet().stream()
				.map(a -> new AbstractMap.SimpleEntry<Node, Node>(a, nodes.getFwdAdj(a)))
				.filter(e -> e.getKey().distanceTo(e.getValue()) > 10)
				.map(e -> new AbstractMap.SimpleEntry<Node, Node>(
						new Node(e.getKey().add(e.getValue()).scale(0.5f)), e.getKey()))
				.sorted((e1, e2) -> 0) // Buffer all results until now (hacky sink).
				.forEachOrdered(e -> {
					nodes.add(e.getKey());
					nodes.modifyAdj(e.getValue(), e.getKey());
					noLoop();
				});
	}

	@Override
	public void keyPressed() {
		super.keyPressed();
		redraw();
	}

	@Override
	public void mousePressed() {
		redraw();
	}


	private static class Node extends Vec2D {
		final Object hasher = new Object();

		public Node(float x, float y) {
			super(x, y);
		}

		public Node(ReadonlyVec2D v) {
			super(v);
		}

		@Override
		public int hashCode() {
			return hasher.hashCode();
		}
	}

	private static class Nodes extends AbstractMap<Node, Integer> {
		LinkedHashMap<Node, Integer> nodeToIndex = new LinkedHashMap<>(N_NODES);
		HashMap<Integer, Node> indexToNode = new HashMap<>(N_NODES);

		// Adjacencies
		int[] fwdAdj = new int[N_NODES];
		int[] reverseAdj = new int[N_NODES];
		int adjSize = 0;

		@Override
		public Set<Entry<Node, Integer>> entrySet() {
			return nodeToIndex.entrySet();
		}

		public Iterator<Entry<Node, Node>> nodeWithAdjIterator() {
			return new Iterator<Entry<Node, Node>>() {
				Iterator<Node> iter = nodeToIndex.keySet().iterator();

				@Override
				public boolean hasNext() {
					return iter.hasNext();
				}

				@Override
				public Entry<Node, Node> next() {
					Node n = iter.next();
					return new SimpleEntry<>(n, indexToNode.get(fwdAdj[nodeToIndex.get(n)]));
				}
			};
		}

		public void add(Node node) {
			int i = nodeToIndex.size();
			nodeToIndex.put(node, i);
			indexToNode.put(i, node);
		}

		public Node getNode(int index) {
			return indexToNode.get(index);
		}

		public Integer getIndex(Node node) {
			return nodeToIndex.get(node);
		}

		public void addAdj(int from, int to) {
			assert from < N_NODES && to < N_NODES;

			fwdAdj[from] = to;
			reverseAdj[to] = from;
			adjSize++;
		}

		public void addAdj(Node from, Node to) {
			addAdj(nodeToIndex.get(from), nodeToIndex.get(to));
		}

		public void modifyAdj(int from, int newFwdTo) {
			assert from < newFwdTo && from < adjSize && newFwdTo < N_NODES;
			int oldTo = fwdAdj[from];
			fwdAdj[from] = newFwdTo;
			fwdAdj[newFwdTo] = oldTo;
			reverseAdj[oldTo] = newFwdTo;
			reverseAdj[newFwdTo] = from;
			adjSize++;
		}

		public void modifyAdj(Node from, Node newFwdTo) {
			modifyAdj(nodeToIndex.get(from), nodeToIndex.get(newFwdTo));
		}
		/*
		public int getFwdAdj(int nodeIndex) {
			assert nodeIndex < adjSize;
			return fwdAdj[nodeIndex];
		}
		*/

		public Node getFwdAdj(Node a) {
			return indexToNode.get(fwdAdj[nodeToIndex.get(a)]);
		}

		public int getReverseAdj(int nodeIndex) {
			assert nodeIndex < adjSize;
			return reverseAdj[nodeIndex];
		}
	}

	/*
	private static class TwoCycleGraphAdjacencies {
		int[] fwd;
		int[] reverse;
		int capacity;
		int size = 0;

		public TwoCycleGraphAdjacencies(int capacity) {
			fwd = new int[capacity];
			reverse = new int[capacity];
			this.capacity = capacity;
		}

		public void add(int from, int to) {
			assert from < capacity && to < capacity;

			fwd[from] = to;
			reverse[to] = from;
			size++;
		}

		public void newAdj(int from, int fwdTo) {
			assert from < fwdTo && from < size && fwdTo < capacity;
			int oldTo = fwd[from];
			fwd[from] = fwdTo;
			fwd[fwdTo] = oldTo;
			reverse[oldTo] = fwdTo;
			reverse[fwdTo] = from;
			size++;
		}

		public int getFwd(int nodeIndex) {
			assert nodeIndex < size;
			return fwd[nodeIndex];
		}

		public int getReverse(int nodeIndex) {
			assert nodeIndex < size;
			return reverse[nodeIndex];
		}
	}
	*/

	public static void main(String[] args) {
		PApplet.main("_experiments.Diff");
	}
}