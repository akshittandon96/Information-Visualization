package com.uh.fdl;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jgrapht.Graph;
import org.jgrapht.generate.GraphGenerator;
import org.jgrapht.graph.SimpleGraph;

import com.esri.arcgisruntime.internal.jni.gr;


public class GraphConfiguration {
	private GraphGenerator<Vertex, Edge, ?> generator;
	private Parameter param;
	
	public GraphConfiguration(GraphGenerator<Vertex, Edge, ?> generator, Parameter param) {
		this.generator = generator;
		this.setParameter(param);
	}

	
	public Graph<Vertex, Edge> generateGraph() {
		Graph<Vertex, Edge> graph = new SimpleGraph<>(new EdgeFactory());
		
		FileInputStream fis = null;
		try {
			fis = new FileInputStream("course_correlations.csv");
		} catch (FileNotFoundException e) {
			System.out.println("Error reading csv");
			e.printStackTrace();
		}
		DataInputStream myInput = new DataInputStream(fis);
		String thisLine;
		List<String[]> lines = new ArrayList<String[]>();
		try {
			while ((thisLine = myInput.readLine()) != null) {
				lines.add(thisLine.split(","));
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println("Error parsing csv");
			e.printStackTrace();
		}
		
		ArrayList<Vertex> vlist = new ArrayList<>();
		
		for(int i=0; i<lines.size();i++) {
			Vertex vertex = new Vertex();
			vertex.randomPos(param.getFrameWidth(), param.getFrameHeight());
			vlist.add(vertex);
			graph.addVertex(vertex);
		}
		
		for(int i=0; i<lines.size();i++) {
			for(int j=i+1; j<lines.get(0).length;j++) {
				if(lines.get(i)[j].equals("1")) {
					graph.addEdge(vlist.get(i), vlist.get(j));
				}
			}
		}
		
		return graph;
	}

	public Parameter getParameter() {
		return param;
	}

	public void setParameter(Parameter param) {
		this.param = param;
	}
}
