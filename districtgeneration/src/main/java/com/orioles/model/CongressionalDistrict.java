package com.orioles.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.orioles.districtgeneration.Edge;
import com.orioles.helper_model.Polygon;

import java.util.*;
import java.util.stream.Collectors;

public class CongressionalDistrict implements Cloneable {
	private int ID;
	private List<Precinct> precincts;
	@JsonIgnore
	private List<Precinct> pBorders;
	@JsonIgnore
	private double goodness;
	@JsonIgnore
	private boolean isDirty;
	@JsonIgnore
	private Stats stat;
	@JsonIgnore
	private double area;

	public CongressionalDistrict() {}

	private CongressionalDistrict(int ID) {
		this.ID = ID;
		this.goodness = -1;
		this.area = 0;
		this.isDirty = true;
	}

	public CongressionalDistrict(List<Precinct> ps, int ID) {
		this(ID);
		this.precincts = ps;
		ps.forEach(p -> p.setDistrict(this));
	}

	List<Precinct> getPrecincts() {
		return precincts;
	}

	public void setPrecincts(List<Precinct> precincts) {
		this.precincts = precincts;
	}

	public int getNumPrecincts() {
		return this.precincts.size();
	}

	public List<Precinct> getpBorders() {
		return pBorders;
	}

	public void setpBorders(List<Precinct> pBorders) {
		this.pBorders = pBorders;
	}

	public int getID() {
		return ID;
	}

	public void setID(int ID) {
		this.ID = ID;
	}

	public double getGoodness() {
		return goodness;
	}

	void setGoodness(double goodness) {
		this.goodness = goodness;
	}

	public Stats summarize() {
		if (!isDirty)
			return stat;

		stat = new Stats();
		precincts.stream().map(Precinct::getStats).forEach(precinctStat -> Stats.summarize(precinctStat, stat));
		isDirty = false;
		return stat;
	}

	void removeFromDistrict(Precinct precinct) {
		isDirty = true;
		precincts.remove(precinct);
	}

	void addToDistrict(Precinct precinct) {
		isDirty = true;
		precincts.add(precinct);
	}

	public Precinct getPrecinctById(int precinctId) {
		return precincts.stream().filter(precinct -> precinct.getIdentifier() == precinctId)
				.findAny().orElse(null);
	}

	private Precinct getRandomPrecinct() {
		return precincts.get((int) (Math.random() * precincts.size()));
	}

	public double getArea() {
		return this.area = precincts.stream().mapToDouble(Precinct::getArea).sum();
	}

	Precinct getMovingPrecinct() {
		Precinct movingPrecinct;
		boolean isBorderPrecinct;
		boolean locked;
		do {
			movingPrecinct = getRandomPrecinct();
			isBorderPrecinct = movingPrecinct.getBorder();
			locked = movingPrecinct.isLocked();
		} while (!isBorderPrecinct || locked);
		return movingPrecinct;
	}

	@JsonIgnore
	public double getPerimeter() {
		Set<Edge> allEdges = new HashSet<>();
		precincts.parallelStream()
				.map(Precinct::getCoordinates)
				.flatMap(Collection::stream)
				.map(Polygon::getAllEdges)
				.flatMap(Collection::stream)
				.filter(edge -> !allEdges.add(edge))
				.forEach(allEdges::remove);
		return allEdges.parallelStream().mapToDouble(Edge::calculateDistance).sum();
	}

	public boolean isContigious() {
		Queue<Precinct> pQueue = new LinkedList<>();
		Set<Precinct> pSet = new HashSet<>();
		pQueue.add(precincts.get(0));

		for (Precinct p = pQueue.poll(); pQueue.isEmpty(); p = pQueue.poll())
			if (p != null && pSet.add(p)) {
				Precinct finalP = p;
				p.getAdjacentPrecincts().stream()
						.filter(adj -> !pSet.contains(adj) && adj.getCD() == finalP.getCD())
						.forEach(pQueue::add);
			}
		return pSet.size() == precincts.size();
	}

	@Override
	public Object clone() {
		CongressionalDistrict newCD = new CongressionalDistrict();
		newCD.setID(this.ID);
		newCD.setGoodness(this.goodness);
		newCD.isDirty = true;
		newCD.stat = this.stat;
		newCD.area = this.area;

		List<Precinct> newPrecincts = precincts.stream().map(p -> (Precinct) p.clone()).collect(Collectors.toList());
		newPrecincts.forEach(p -> p.setDistrict(newCD));
		newCD.setPrecincts(newPrecincts);

		Set<Integer> pSet = newPrecincts.stream().map(Precinct::getIdentifier).collect(Collectors.toSet());
		newCD.setpBorders(newPrecincts.stream().filter(p -> pSet.contains(p.getIdentifier())).collect(Collectors.toList()));
		return newCD;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null || getClass() != o.getClass())
			return false;
		CongressionalDistrict otherDistrict = (CongressionalDistrict) o;
		return this.ID == otherDistrict.ID ;
	}

	@Override
	public String toString() {
		return String.format("CD(%d -> <%f, %s>, %s)", ID, getArea(), summarize(), precincts);
	}
}
