package com.uh.fdl;

import java.util.concurrent.Executors;

import org.jgrapht.Graph;

import parsii.tokenizer.ParseException;

public class ForceDirectedPlacement {
	
	public static void simulate(Graph<Vertex, Edge> graph, Parameter parameter) throws ParseException {
		Executors.newSingleThreadExecutor().submit(new Simulation(graph, parameter));
	}
}
