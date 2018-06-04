package com.orioles.helper_model;

import com.orioles.districtgeneration.Coordinate;
import com.orioles.districtgeneration.Edge;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Polygon {
	private List<List<Edge>> edges;		// edges.get(0) = bounding region; subsequent elements are holes

	public Polygon(List<List<List<Double>>> edges) {
		this.edges = edges.stream().map(this::obtainEdges).collect(Collectors.toList());
	}

	public List<List<Edge>> getEdges() {
		return edges;
	}

	public List<Edge> getAllEdges() {
		return edges.stream().flatMap(Collection::stream).collect(Collectors.toList());
	}

	public List<Coordinate> getCoordinates() {
		return edges.stream().flatMap(Collection::stream).map(Edge::getP1).collect(Collectors.toList());
	}

	private List<Edge> obtainEdges(List<List<Double>> rawEdges) {
		List<Edge> edges = new ArrayList<>();
		final List<Coordinate> coordList = rawEdges.stream()
				.map(pt -> new Coordinate(pt.get(0), pt.get(1))).collect(Collectors.toList());
		for (int i = 0; i < coordList.size(); i++)
			edges.add(new Edge(coordList.get(i), coordList.get((i + 1) % coordList.size())));
		return edges;
	}
}
