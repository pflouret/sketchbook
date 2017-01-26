package _experiments;

import pfp5.ProcessingApp;
import processing.core.PApplet;
import toxi.geom.Circle;
import toxi.geom.PointQuadtree;
import toxi.geom.ReadonlyVec2D;
import toxi.geom.Vec2D;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Diff extends ProcessingApp {
	private static final int N_NODES = 1000;

	private Nodes nodes = new Nodes();
	private int initialNodes = 15;
	private int growthDistance = 30;

	@Override
	public void setup() {
		super.setup();

		List<Vec2D> vertices = new Circle(0, 0, 10).toPolygon2D(initialNodes).vertices;
		for (int i=0; i < vertices.size(); i++) {
			nodes.add(new Node(vertices.get(i)));
			nodes.addAdj(i, (i+1) % vertices.size());
		}
	}

	@Override
	public void draw() {
		clear();

		pushMatrix();
		translate(width/2, height/2);
		//translate(mouseX, mouseY);

		nodes.stream().forEachOrdered(adj -> {
			drawNode(adj.a);
			drawEdge(adj);
		});
		popMatrix();

		updateNodes();
		//noLoop();
	}

	private void drawNode(Node n) {
		pushStyle();;
		strokeWeight(4); stroke(0, 100); fill(0, 240);
		ellipse(n.x, n.y, 4, 4);
		popStyle();
	}

	private void drawEdge(Adj adj) {
		pushStyle();
		strokeWeight(1.5f); stroke(255, 0, 0, 50); noFill();
		line(adj.a.x, adj.a.y, adj.b.x, adj.b.y);
		popStyle();
	}


	private void updateNodes() {
		nodes.stream().forEachOrdered(adj -> {
			adj.a.scaleSelf(1.005f);
			//adj.a.jitter(.1f);
		});

		if (nodes.size() >= N_NODES / 2) {
			return;
		}

		nodes.stream()
				.filter(adj -> adj.edgeLength() > growthDistance)
				.map(adj -> new Adj(adj.a, adj.arcMidPoint()))
				.sorted((j, k) -> 0) // Hack-ish buffer/sink to prevent concurrent modification.
				.forEachOrdered(adj -> {
					nodes.add(adj.b);
					nodes.modifyAdj(adj.a, adj.b);
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


	class Node extends Vec2D {
		final Object hasher = new Object();

		Node(float x, float y) {
			super(x, y);
		}

		Node(ReadonlyVec2D v) {
			super(v);
		}

		@Override
		public int hashCode() {
			return hasher.hashCode();
		}
	}

	class Adj {
		Node a, b;

		Adj(Node a, Node b) {
			this.a = a;
			this.b = b;
		}

		float edgeLength() {
			return a.distanceTo(b);
		}

		Node arcMidPoint() {
			float r = a.magnitude();
			float theta = atan2(a.y+b.y, a.x+b.x);
			return new Node(r*cos(theta), r*sin(theta));
		}

	}

	class Nodes {
		LinkedHashMap<Node, Integer> nodeToIndex = new LinkedHashMap<>(N_NODES);
		HashMap<Integer, Node> indexToNode = new HashMap<>(N_NODES);
		PointQuadtree spatialIndex;

		Nodes() {
			spatialIndex = new PointQuadtree(0, 0, width, height);
		}

		Stream<Adj> stream() {
			return StreamSupport.stream(nodeToIndex.keySet().spliterator(), false)
					.map(this::getFwdAdj);
		}

		// Adjacencies
		int[] fwdAdj = new int[N_NODES];
		int[] reverseAdj = new int[N_NODES];
		int adjSize = 0;

		void add(Node node) {
			int i = nodeToIndex.size();
			nodeToIndex.put(node, i);
			indexToNode.put(i, node);
		}

		Node getNode(int index) {
			return indexToNode.get(index);
		}

		Integer getIndex(Node node) {
			return nodeToIndex.get(node);
		}

		void addAdj(int from, int to) {
			assert from < N_NODES && to < N_NODES;

			fwdAdj[from] = to;
			reverseAdj[to] = from;
			adjSize++;
		}

		void addAdj(Node from, Node to) {
			addAdj(nodeToIndex.get(from), nodeToIndex.get(to));
		}

		void modifyAdj(int from, int newFwdTo) {
			assert from < newFwdTo && from < adjSize && newFwdTo < N_NODES;
			int oldTo = fwdAdj[from];
			fwdAdj[from] = newFwdTo;
			fwdAdj[newFwdTo] = oldTo;
			reverseAdj[oldTo] = newFwdTo;
			reverseAdj[newFwdTo] = from;
			adjSize++;
		}

		void modifyAdj(Node from, Node newFwdTo) {
			modifyAdj(nodeToIndex.get(from), nodeToIndex.get(newFwdTo));
		}

		Adj getFwdAdj(Node a) {
			return new Adj(a, indexToNode.get(fwdAdj[nodeToIndex.get(a)]));
		}

		Adj getReverseAdj(Node a) {
			return new Adj(indexToNode.get(reverseAdj[nodeToIndex.get(a)]), a);
		}

		int size() {
			return nodeToIndex.size();
		}
	}

	public static void main(String[] args) {
		PApplet.main("_experiments.Diff");
	}
}