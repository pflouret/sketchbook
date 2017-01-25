package _experiments;

import processing.core.PApplet;
import toxi.geom.Circle;
import toxi.geom.PointQuadtree;
import toxi.geom.Rect;
import toxi.geom.SpatialIndex;
import toxi.geom.Vec2D;
import toxi.physics2d.VerletParticle2D;
import toxi.physics2d.VerletPhysics2D;
import toxi.physics2d.behaviors.ParticleBehavior2D;
import toxi.processing.ToxiclibsSupport;

public class Verlet extends PApplet {
	private VerletPhysics2D ph = new VerletPhysics2D();
	private Circle circle;
	private ToxiclibsSupport gfx;
	private static final int EFFECT_RADIUS = 80;

	@Override
	public void settings() {
		size(500, 500);
	}

	@Override
	public void setup() {
		super.setup();

		background(255);
		gfx = new ToxiclibsSupport(this);

		ph.setWorldBounds(new Rect(0, 0, width, height));
		ph.setIndex(new PointQuadtree(0, 0, width, height));


		addParticles();
		//noLoop();
	}

	private void addParticles() {
		Circle circle = new Circle(width/2, height/2, 50);
		circle.toPolygon2D(10).vertices.stream()
				.map(VerletParticle2D::new)
				.forEach(p -> {
					p.addBehavior(new ConstantAttractionBehavior2D(p, 80, 0.5f));
					p.addBehavior(new ConstantAttractionBehavior2D(p, 120, -0.6f));
					ph.addParticle(p);
				});
		ph.particles.get(6).x += 0.2;
	}

	@Override
	public void keyPressed() {
		//addParticles();
		redraw();
	}

	@Override
	public void draw() {
		background(255);

		/*
		strokeWeight(1);
		stroke(0, 20);
		fill(0, 5);
		gfx.ellipse(circle);
		*/

		strokeWeight(4);
		stroke(0, 100);
		fill(0, 240);
		ph.particles.forEach(p -> {
			ellipse(p.x, p.y, 4, 4);
		});

		strokeWeight(1);
		stroke(255, 0, 0, 20);
		noFill();

		for (int i=0; i < ph.particles.size(); i++) {
			VerletParticle2D a = ph.particles.get(i);
			if (i == 6) {
				ellipse(a.x, a.y, 80, 80);
				ellipse(a.x, a.y, 120, 120);
			}
			for (int j=i+1; j < ph.particles.size(); j++) {
				VerletParticle2D b = ph.particles.get(j);
				line(a.x, a.y, b.x, b.y);
			}
		}

		ph.update();

	}

	static class ConstantAttractionBehavior2D implements ParticleBehavior2D {
		private VerletParticle2D a;
		private float distance = 0;
		private float radius = 0;

		public ConstantAttractionBehavior2D(VerletParticle2D a, float radius, float distance) {
			this.a = a;
			this.distance = distance;
			this.radius = radius;
		}

		@Override
		public void apply(VerletParticle2D p) {
			a.set(a.interpolateTo(p, distance));
		}

		@Override
		public void applyWithIndex(SpatialIndex<Vec2D> index) {
			index.itemsWithinRadius(a, radius, null).stream()
					.map(v -> (VerletParticle2D)v)
					.forEach(this::apply);
		}

		@Override
		public void configure(float timeStep) {
		}

		@Override
		public boolean supportsSpatialIndex() {
			return true;
		}
	}

	public static void main(String[] args) {
		PApplet.main("_experiments.verlet.Verlet");
	}
}