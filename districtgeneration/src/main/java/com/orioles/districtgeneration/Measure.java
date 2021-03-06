package com.orioles.districtgeneration;

import com.orioles.model.CongressionalDistrict;
import com.orioles.model.State;
import java.util.List;
import java.util.OptionalDouble;

public interface Measure {
    default double calculateGoodness(State state) {
		List<CongressionalDistrict> districts = state.getCongressionalDistricts();
		OptionalDouble avgGoodness = districts.stream().mapToDouble(cd -> this.calculateGoodness(cd, state)).average();
		return avgGoodness.isPresent() ? avgGoodness.getAsDouble() : -1;
    }

    double calculateGoodness(CongressionalDistrict district, State state);
}
