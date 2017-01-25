package _experiments;

import pfp5.ProcessingApp;
import processing.core.PApplet;
import toxi.geom.Circle;
import toxi.geom.PointQuadtree;
import toxi.geom.ReadonlyVec2D;
import toxi.geom.Vec2D;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

public class Diff extends ProcessingApp {
	private static final int N_NODES = 1000;

	private PointQuadtree spatialIndex;
	private Nodes nodes = new Nodes();

	@Override
	public void setup() {
		super.setup();

		spatialIndex = new PointQuadtree(0, 0, width, height);

		//List<Vec2D> vertices = new Circle(width / 2, height / 2, 10).toPolygon2D(10).vertices;
		List<Vec2D> vertices = new Circle(0, 0, 10).toPolygon2D(10).vertices;
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

		noLoop();

		updateNodes();
		popMatrix();
	}

	private void updateNodes() {
		nodes.keySet().forEach(n -> n.scaleSelf(1.05f));
		/*
		for (Vec2D n : nodes) {
			spatialIndex.itemsWithinRadius(n, 80, new ArrayList<>()).forEach(nn -> {
				nn.set(nn.interpolateTo(n, 0.05f));
			});
			spatialIndex.itemsWithinRadius(n, 120, new ArrayList<>()).forEach(nn -> {
				nn.set(nn.interpolateTo(n, -0.08f));
			});
		}
		*/
	}

	@Override
	public void keyPressed() {
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
		private LinkedHashMap<Node, Integer> nodeToIndex = new LinkedHashMap<>(N_NODES);
		private HashMap<Integer, Node> indexToNode = new HashMap<>(N_NODES);

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

		public void modifyAdj(int from, int newFwdTo) {
			assert from < newFwdTo && from < adjSize && newFwdTo < N_NODES;
			int oldTo = fwdAdj[from];
			fwdAdj[from] = newFwdTo;
			fwdAdj[newFwdTo] = oldTo;
			reverseAdj[oldTo] = newFwdTo;
			reverseAdj[newFwdTo] = from;
			adjSize++;
		}

		public int getFwdAdj(int nodeIndex) {
			assert nodeIndex < adjSize;
			return fwdAdj[nodeIndex];
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