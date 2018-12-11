package com.uh.fdl;

import java.util.concurrent.Callable;

import javax.vecmath.Vector2d;

import org.jgrapht.Graph;

import parsii.eval.Expression;
import parsii.eval.Parser;
import parsii.eval.Scope;
import parsii.eval.Variable;
import parsii.tokenizer.ParseException;

public class Simulation  implements Callable<Integer> {

	private Graph<Vertex, Edge> graph;

	private int frameWidth;
	private int frameHeight;
	
	private double condition;
	private double coolingRate;
	private int timeStep;
	
	private static final double constVal = 0.4;
	private int iteration = 0;

	private int area;
	private double stepSize;
	private double t;

	private Scope scope = Scope.create();
	private Variable varD = scope.getVariable("d");
	private Variable varK = scope.getVariable("k");
	private Expression forceAttractive;
	private Expression forceRepulsive;

	private boolean equilibriumAchieved = false;

	
	
	public Simulation(Graph<Vertex, Edge> graph, Parameter p) throws ParseException {
		this.graph = graph;
		this.frameWidth = p.getFrameWidth();
		this.frameHeight = p.getFrameHeight();
		this.condition = p.getCriterion();
		this.coolingRate = p.getCoolingRate();
		this.timeStep = p.getFrameDelay();

		// parse the force strings into Expressions that can be evaluated multiple times
		forceAttractive = Parser.parse(p.getAttractiveForce(), scope);
		forceRepulsive = Parser.parse(p.getRepulsiveForce(), scope);
	}

	private int startSimulation() {

		iteration = 0;
		equilibriumAchieved = false;

		area = Math.min(frameWidth * frameWidth, frameHeight * frameHeight);
		stepSize = constVal * Math.sqrt(area / graph.vertexSet().size());
		t = 1;

		for (Vertex v : graph.vertexSet()) {
			v.randomPos(frameWidth, frameHeight);
		}

		
		while (!equilibriumAchieved ) {
			simulateStep();
		}
		
		return iteration;
	}

	
	private void simulateStep() {


		for (Vertex v : graph.vertexSet()) {

			v.getDisp().set(0, 0);
			for (Vertex u : graph.vertexSet()) {
				if (!v.equals(u)) {

					Vector2d deltaPos = new Vector2d();
					deltaPos.sub(v.getPos(), u.getPos());
					double length = deltaPos.length();
					deltaPos.normalize();

					deltaPos.scale(this.forceRepulsive(length, stepSize));
					v.getDisp().add(deltaPos);
				}
			}
		}

		
		for (Edge e : graph.edgeSet()) {

			Vector2d deltaPos = new Vector2d();
			deltaPos.sub(e.getV().getPos(), e.getU().getPos());
			double length = deltaPos.length();
			deltaPos.normalize();

			deltaPos.scale(this.forceAttractive(length, stepSize));

			e.getV().getDisp().sub(deltaPos);
			e.getU().getDisp().add(deltaPos);
		}

		equilibriumAchieved = true;

		for (Vertex v : graph.vertexSet()) {

			Vector2d disp = new Vector2d(v.getDisp());
			double length = disp.length();

			if (length > condition) {
				equilibriumAchieved = false;
			}
			// System.out.print((int)length + "; ");
			disp.normalize();
			disp.scale(Math.min(length, t));
			v.getPos().add(disp);

			// prevent being displaced outside the frame
			v.getPos().x = Math.min(frameWidth-BaseMap.VERTEX_WIDTH, Math.max(BaseMap.VERTEX_WIDTH, v.getPos().x));
			v.getPos().y = Math.min(frameHeight-BaseMap.VERTEX_WIDTH, Math.max(BaseMap.VERTEX_WIDTH, v.getPos().y));
		}
		// System.out.println();
		// reduce the temperature as the layout approaches a better configuration
		t = Math.max(t * (1 - coolingRate), 1);

		// System.out.println("t: " + (float) t);

		try {
			Thread.sleep(timeStep);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		iteration++;
	}


	private double forceAttractive(double d, double k) {
		varD.setValue(d);
		varK.setValue(k);
		return forceAttractive.evaluate();
	}

	private double forceRepulsive(double d, double k) {
		varD.setValue(d);
		varK.setValue(k);
		return forceRepulsive.evaluate();
	}

	@Override
	public Integer call() throws Exception {
		return startSimulation();
	}

}
