package com.uh.fdl;


import java.util.ArrayList;
import java.util.List;



import javax.vecmath.Vector2d;


import org.jgrapht.Graph;
import org.jgrapht.generate.GnmRandomGraphGenerator;

import com.esri.arcgisruntime.geometry.Point;
import com.esri.arcgisruntime.geometry.PointCollection;
import com.esri.arcgisruntime.geometry.Polyline;
import com.esri.arcgisruntime.geometry.SpatialReferences;
import com.esri.arcgisruntime.mapping.ArcGISScene;
import com.esri.arcgisruntime.mapping.ArcGISTiledElevationSource;
import com.esri.arcgisruntime.mapping.Basemap;
import com.esri.arcgisruntime.mapping.Surface;
import com.esri.arcgisruntime.mapping.view.Camera;
import com.esri.arcgisruntime.mapping.view.Graphic;
import com.esri.arcgisruntime.mapping.view.GraphicsOverlay;
import com.esri.arcgisruntime.mapping.view.SceneView;
import com.esri.arcgisruntime.mapping.view.LayerSceneProperties.SurfacePlacement;
import com.esri.arcgisruntime.symbology.PictureMarkerSymbol;
import com.esri.arcgisruntime.symbology.SimpleLineSymbol;

import com.esri.arcgisruntime.util.ListenableList;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Point2D;
import javafx.scene.Scene;

import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;


public class BaseMap extends Application {
	
	private List<Graph<Vertex, Edge>> graphs = new ArrayList<>();
	
	int graphSizeValue = 15;
	
	public static final int VERTEX_WIDTH = 100;
	
	static Vertex selectedVertex = null;
	static int locationUpdated = -1;
	
	
	
	private int frameWidth = 800;
	private int frameHeight = 700;
	
	
	public static SceneView sceneView;
	public static Surface surface;
	public static GraphicsOverlay graphicsOverlay;
	private static final String ELEVATION_IMAGE_SERVICE =
		      "http://elevation3d.arcgis.com/arcgis/rest/services/WorldElevation3D/Terrain3D/ImageServer";
	

	
	private AnimationTimer animation = new AnimationTimer() {
		@Override
		public void handle(long now) {
			
				ListenableList<Graphic> graphicList= graphicsOverlay.getGraphics();
				graphicList.clear();
				for (Graph<Vertex, Edge> g : graphs) {
					drawGraph(g);
				}
                
            if(locationUpdated>0) {
            	locationUpdated--;
            }
            if(locationUpdated==0) {
            	locationUpdated=-1;
            	selectedVertex=null;
            }
		}
	};
	
	private void drawGraph(Graph<Vertex, Edge> g) {
		for (Edge e : g.edgeSet()) {
			drawEdge(e.getU(), e.getV());
		}
		for (Vertex v : g.vertexSet()) {
			drawPoint(v);
		}
	}
	
	public static void drawPoint(Vertex v) {
		Point point = BaseMap.sceneView.screenToBaseSurface(new Point2D(v.getPos().x, v.getPos().y));
		if(point!=null) {
		PictureMarkerSymbol pointSymbol;
		if (graphicsOverlay != null) {
			if(selectedVertex!=null) {
				if(v.equals(selectedVertex)) {
					pointSymbol = makeBuildingSymbol(true);
				}else {
					pointSymbol = makeBuildingSymbol(false);
				}
			}else {
				pointSymbol = makeBuildingSymbol(false);
			}
		    Graphic pointGraphic = new Graphic(point, pointSymbol);
		    graphicsOverlay.getGraphics().add(pointGraphic);
		}
		}
	}
	
	private static PictureMarkerSymbol makeBuildingSymbol(Boolean selected) {
		PictureMarkerSymbol symbol;
		if(selected) {
			symbol = new PictureMarkerSymbol("building_green.png");
		}else {
			symbol = new PictureMarkerSymbol("building1.png");
		}
		
	    return symbol;
	}

	public static void drawEdge(Vertex v, Vertex u) {
		Point uPoint = BaseMap.sceneView.screenToBaseSurface(new Point2D(u.getPos().x, u.getPos().y));
		Point vPoint = BaseMap.sceneView.screenToBaseSurface(new Point2D(v.getPos().x, v.getPos().y));
		
		if(uPoint!=null&&vPoint!=null) {
			PointCollection polylinePoints = new PointCollection(SpatialReferences.getWgs84());
			polylinePoints.add(uPoint);
			polylinePoints.add(vPoint);
			  
		    if (graphicsOverlay != null) {
			    Polyline polyline = new Polyline(polylinePoints);
			    Graphic polylineGraphic = new Graphic(polyline, new SimpleLineSymbol(SimpleLineSymbol.Style.SOLID, 0xFF00FFFF, 1));
			    graphicsOverlay.getGraphics().add(polylineGraphic);
		    }
		}
	}
	
	@Override
	public void start(Stage stage) throws Exception {
		StackPane stackPane = new StackPane();
		Scene fxScene = new Scene(stackPane);
		stage.setTitle("Force-directed layout");
		stage.setWidth(frameWidth);
		stage.setHeight(frameHeight);
		stage.setScene(fxScene);
		stage.show();
		
		ArcGISScene scene = new ArcGISScene();
	    scene.setBasemap(Basemap.createImagery());
	   
	    sceneView = new SceneView();
	    sceneView.setArcGISScene(scene);
	    stackPane.getChildren().add(sceneView);

	    // add a camera and initial camera position
	    Camera camera = new Camera(27.0, 87.3, 8000, 0, 30.0, 0);
	    sceneView.setViewpointCamera(camera);
	   
		surface = new Surface();
	    surface.getElevationSources().add(new ArcGISTiledElevationSource(ELEVATION_IMAGE_SERVICE));
	    
	    scene.setBaseSurface(surface);
	    
	    sceneView.setOnKeyPressed(event -> {
	    	System.out.println("key pressed");
	    	if(event.getCode() == KeyCode.S) {
	    		for (GraphConfiguration config : getGraphConfigurations()) {
	    			Graph<Vertex, Edge> graph = config.generateGraph();
	    			try {
	    				ForceDirectedPlacement.simulate(graph, config.getParameter());
	    			} catch (Exception e1) {
	    				System.out.println("Parsing Error");
	    			}
	    			this.graphs.add(graph);
	    		}
	    	}else if(event.getCode() == KeyCode.SPACE) {
	    		System.out.println("space bar");
	    		animation.start();
	    		for (GraphConfiguration config : getGraphConfigurations()) {
	    			Graph<Vertex, Edge> graph = config.generateGraph();
	    			try {
	    				ForceDirectedPlacement.simulate(graph, config.getParameter());
	    			} catch (Exception e1) {
	    				System.out.println("Parsing Error");
	    			}
	    			this.graphs.add(graph);
	    		}
	    	}else if(event.getCode() == KeyCode.A) {
	    		animation.start();
	    	}else if(event.getCode() == KeyCode.P) {
	    		for (Vertex v : graphs.get(0).vertexSet()) {
	    			drawPoint(v);
	    		}
	    	}
	    });
	    
	    sceneView.setOnMouseClicked(event -> {
	        if (event.isStillSincePress() && event.getButton() == MouseButton.PRIMARY) {
	          
	        	  if(selectedVertex==null) {
	        		  highlightGraphic(new Point2D(event.getX(), event.getY()));
	        	  }else {
	        		  updateLocation(new Point2D(event.getX(), event.getY()));
	        	  }
	        }
	      }
	    );
	    
	    graphicsOverlay = new GraphicsOverlay();
		graphicsOverlay.getSceneProperties().setSurfacePlacement(SurfacePlacement.DRAPED);
	    sceneView.getGraphicsOverlays().add(graphicsOverlay);
	}
	
	private void updateLocation(Point2D point2d) {
		for (Graph<Vertex, Edge> g : graphs) {
			for (Vertex v : g.vertexSet()) {
			     if (v.equals(selectedVertex)) {
			    	 Vector2d vector2d = new Vector2d(new double[]{point2d.getX(), point2d.getY()});
			    	 v.setPos(vector2d);
			    	 locationUpdated = 25;
			    	 break;
			     }
			   }
		}
	}

	private void highlightGraphic(Point2D screenPoint) {
		double leastDist = Integer.MAX_VALUE;
		Vertex closestVertex = null;
		for (Graph<Vertex, Edge> g : graphs) {
			for (Vertex v : g.vertexSet()) {
				double dist = Math.sqrt((screenPoint.getX()-v.getPos().x)*(screenPoint.getX()-v.getPos().x)+(screenPoint.getY()-v.getPos().y)*(screenPoint.getY()-v.getPos().y));
				if(dist<leastDist) {
					closestVertex = v;
					leastDist=dist;
				}
			}
			for (Vertex v : g.vertexSet()) {
			     if (v.equals(closestVertex)) {
			    	 selectedVertex = v;
			    	 break;
			     }
			   }
		}
		
		//v.setPos(new veThreadLocalRandom.current().nextInt((VERTEX_WIDTH*2), frameWidth - (VERTEX_WIDTH*2) + 1));
	}

	private List<GraphConfiguration> getGraphConfigurations() {
		
		List<GraphConfiguration> graphConfigs = new ArrayList<>();
		
		//List<Pair<String, String>> forceFunctions = parseForceFunctions();
		this.graphs.clear();
		
		
		Parameter p = new Parameter();
		p.setFrameWidth((int)frameWidth);
		p.setFrameHeight((int) frameHeight);
		p.setEquilibriumCriterion(false);
		p.setAttractiveForce("(d * 10) / k");
		p.setRepulsiveForce("(k * k) / d");
		p.setCriterion(15);
		p.setCoolingRate(0.01);
		p.setFrameDelay(5);
		
		graphConfigs.add(new GraphConfiguration(new GnmRandomGraphGenerator<>(graphSizeValue, graphSizeValue), p));
		
		return graphConfigs;
	}
	
	@Override
	  public void stop() {
	    if (sceneView != null) {
	    	sceneView.dispose();
	    }
	  }
	
	public static void main(String[] args) {
	    Application.launch(args);
	 }
}
