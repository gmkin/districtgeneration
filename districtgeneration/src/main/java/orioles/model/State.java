package orioles.model;

import orioles.constants.Party;
import orioles.constants.Race;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class State implements Cloneable {
    
        private List<CongressionalDistrict> congressionalDistricts;
        private String name;
        private double goodness;
	private boolean hasUpdated;
	private Stats stat;
        
        public State(){
            congressionalDistricts = new ArrayList<>();
            name = "";
            goodness = 0;
            hasUpdated = false;
            stat = new Stats();
        }

	public List<CongressionalDistrict> getCongressionalDistricts() {
		return congressionalDistricts;
	}

	public void setCongressionalDistricts(List<CongressionalDistrict> congressionalDistricts) {
		this.congressionalDistricts = congressionalDistricts;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getNumPrecincts() {
		return congressionalDistricts.size();
	}

	public void addDistrict(CongressionalDistrict cd) {
		congressionalDistricts.add(cd);
	}
    
    public Stats summarize() {
		if (!hasUpdated)
			return stat;

        Map<Race, Long> conDistRace = new HashMap<>();
        Map<Party, Long> conDistParty = new HashMap<>();
		stat = new Stats(conDistRace, conDistParty, 0);

        congressionalDistricts.stream()
                .map(CongressionalDistrict::summarize)
                .forEach(cdStat -> Summary.summarize(conDistRace, conDistParty, cdStat, stat));

        hasUpdated = true;
		return stat;
    }
    
    public CongressionalDistrict getDistrictByID (int districtID){
        return congressionalDistricts.stream()
                .filter(district -> districtID == district.getID())
                .findFirst().orElse(null);
    }

//    public CongressionalDistrict getDistrictByNumber(int districtNumber){
//		return congressionalDistricts.stream()
//				.filter(precinct -> precinct.getIdentifier() == districtNumber)
//				.findFirst().orElse(null);
//    }
    
    public List<CongressionalDistrict> getGerrymanderedDistricts(){
        return null;
    }

    public void setDistrictGoodness(Map<Measure, Double> measures){
        for(int i=0; i<congressionalDistricts.size(); i++){
            ArrayList<Double> goodnessVals = new ArrayList<>();
            for ( Measure key : measures.keySet()) {
                    goodnessVals.add(key.calculateGoodness(congressionalDistricts.get(i))*measures.get(key));
            }
            double districtGoodness = 0;
            for(int j=0; j<goodnessVals.size();j++){
                    districtGoodness+=goodnessVals.get(j);
            }
            districtGoodness = districtGoodness/goodnessVals.size();
            congressionalDistricts.get(i).setOldGoodness(districtGoodness);
        }
    }
    
    public void calculateGoodness(Map<Measure, Double> measures){
        setDistrictGoodness(measures);
        double average = 0;
        for (CongressionalDistrict congressionalDistrict : congressionalDistricts) {
            average+=congressionalDistrict.getOldGoodness();
        }
        average = average/congressionalDistricts.size();
        this.goodness = average;
    }
    
    public double getGoodness(){
        return this.goodness;
    }
    
    public void setGoodness(double newGoodness){
        this.goodness = newGoodness;
    }
    
     public CongressionalDistrict getStartingDistrict(){
        return congressionalDistricts.get((int)(Math.random()*congressionalDistricts.size()));
    }
}
