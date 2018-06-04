package com.orioles.controller;

import com.google.gson.Gson;
import com.orioles.constants.Constants;
import com.orioles.exceptions.NoSuchStateException;
import com.orioles.helper_model.Polygon;
import com.orioles.model.*;
import com.orioles.persistence.PDemoRepository;
import com.orioles.persistence.PrecinctRepository;
import com.orioles.persistence.UsermovesRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.stream.Collectors;

@RestController
public class StateController {
    private Map<String, FeatureCollection> stateCache;
    @Autowired
    private Gson gson;
    @Autowired
    private PrecinctRepository precinctRepository;
    @Autowired
    private UsermovesRepository userMovesRepository;
    @Autowired
    private PDemoRepository pDemoRepository;
    @Autowired
    private Environment environment;
    @Autowired
    private HttpSession httpSession;
    private Map<String, State> states;
    private Map<Integer, Precinct> allPrecincts;

    @GetMapping("/state/{name}")
    public State getState(@PathVariable("name") String stateName) {
        if (!Constants.ALL_STATES.contains(stateName.toLowerCase())) {
            throw new NoSuchStateException(environment.getProperty(Constants.NO_MATCH));
        }
        return getStateByName(stateName.toLowerCase());
    }

    private FeatureCollection getFromCache(String stateName) {
        if (stateCache == null) {
            stateCache = new HashMap<>();
        }
        return stateCache.getOrDefault(stateName, null);
    }

    @GetMapping("/precincts/{state}")
    public FeatureCollection getPrecincts(@PathVariable(Constants.STATE) String stateName) {
        stateName = stateName.toLowerCase();
        FeatureCollection result = getFromCache(stateName);
        if (result != null) {
            return result;
        }

        List<Precinct> precincts = precinctRepository.findByIdState(stateName);
        if (precincts.isEmpty()) {
            throw new NoSuchStateException(environment.getProperty(Constants.NO_MATCH));
        }

        result = new FeatureCollection(precincts.stream()
                .map(p -> gson.fromJson(p.getGeojson(), Map.class)).collect(Collectors.toList()));
        stateCache.put(stateName, result);
        return result;
    }

    @PostMapping("/getMaps")
    public List<Integer> getMaps(String user) {
        return userMovesRepository.findByUsername(user).stream()
				.map(Usermoves::getUselessid).collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
	@PostMapping("/loadMap")
	public List<Move> loadMap (String currState, int mapID) {
		Algorithm algo = (Algorithm) httpSession.getAttribute("algo");
		algo.loadOldRedistricting(getStateByName(currState.toLowerCase()),
				(List<Move>) gson.fromJson(userMovesRepository.findByUselessid(mapID).getMoves(), List.class));
		httpSession.setAttribute("algo", algo);
		return algo.getCurrMoves();
	}

	private State checkCache(String stateName) {
    	if (states == null)
    		states = new HashMap<>();
    	if (allPrecincts == null)
    		allPrecincts = new HashMap<>();
    	return states.containsKey(stateName) ? (State) states.get(stateName).clone() : null;
	}

	@GetMapping("/getStateByName")
	public State getStateByName(String stateName) {
		State fromCache = checkCache(stateName.toLowerCase());
    	if (fromCache != null) {
			httpSession.setAttribute(Constants.STATE, fromCache);
			return fromCache;
		}

		long time = System.nanoTime();
		List<Precinct> precinctList = precinctRepository.findByIdState(stateName);
		Map<Integer, List<Precinct>> p1 = precinctList.stream()
				.collect(Collectors.groupingBy(Precinct::getCD, Collectors.toList()));
		List<CongressionalDistrict> cds = p1.keySet().parallelStream()
				.map(distID -> new CongressionalDistrict(p1.get(distID), distID)).collect(Collectors.toList());

		System.out.printf("Partition: %d", System.nanoTime() - time);
		precinctList.forEach(this::setupPrecinct);
		System.out.printf("Precinct setup: %d", System.nanoTime() - time);
		precinctList.forEach(this::processAdj);
		System.out.printf("Process Adj: %d", System.nanoTime() - time);
		State s = new State(cds, stateName);
		states.put(stateName, s);
		httpSession.setAttribute(Constants.STATE, s);
		System.out.printf("End: %d", System.nanoTime() - time);
		return s;
	}

	@SuppressWarnings("unchecked")
	private List<Polygon> parseCoordinates(Object coords) {		// Assumes coords is a multi-polygon
		List<List<List<List<Double>>>> coordinates = (List<List<List<List<Double>>>>) coords;
		return coordinates.stream().map(Polygon::new).collect(Collectors.toList());
	}

	private void setupPrecinct(Precinct p) {
		p.setStats(pDemoRepository.findByPid(p.getIdentifier()).makeStat());
		Map json = gson.fromJson(p.getGeojson(), Map.class);
		p.setCoordinates(parseCoordinates(((Map)json.get(Constants.GEOMETRY)).get(Constants.COORDINATES)));
		allPrecincts.put(p.getIdentifier(), p);
	}

	private void processAdj(Precinct p) {
		Map json = gson.fromJson(p.getGeojson(), Map.class);
		p.setAdjacentPrecincts(parseAdjacent(((Map)json.get(Constants.PROPERTIES)).get(Constants.NEIGHBORS)));
	}

	@SuppressWarnings("unchecked")
	private List<Precinct> parseAdjacent(Object adj) {
		if (adj == null)		// ie: islands
			return Collections.emptyList();
		return Arrays.stream(((String) adj).split(","))
				.map(e -> allPrecincts.get(Integer.parseInt(e))).collect(Collectors.toList());
	}

	@GetMapping("/getCDInfo")
	public Stats getCDInfo(int cdID) {
		return ((State) httpSession.getAttribute(Constants.STATE)).getDistrictByID(cdID).summarize();
	}

	@GetMapping("/getStateInfo")
	public Stats getStateInfo() {
		return ((State) httpSession.getAttribute(Constants.STATE)).summarize();
	}

	@PostMapping("/getDiff")
	public Map<Integer, Double> getDiff() {			// get differences between curr and gen
		State s = (State) httpSession.getAttribute(Constants.STATE);
		State orig = getStateByName(s.getName());

		Map<Integer, Double> res = new HashMap<>();
		s.getCongressionalDistricts()
				.forEach(cd -> res.put(cd.getID(), cd.getGoodness() - orig.getDistrictByID(cd.getID()).getGoodness()));
    	return res;
	}

	@PostMapping("/goodnessPanel")
	public Map<Integer, Double> goodnessPanel() {		// CD -> goodness
    	State s = (State) httpSession.getAttribute(Constants.STATE);
    	Map<Integer, Double> res = new HashMap<>();
    	s.getCongressionalDistricts().forEach(cd -> res.put(cd.getID(), cd.getGoodness()));
    	return res;
	}

	@GetMapping("/lockPrecinct/{pid}")
	public String lock(@PathVariable("pid") int pid) {
		State s = (State) httpSession.getAttribute(Constants.STATE);
		List<Precinct> ps = s.getAllPrecincts().stream()
				.filter(p -> p.getIdentifier() == pid).collect(Collectors.toList());

		if (ps.isEmpty())
			return "Error";

		ps.get(0).setLocked(true);
		return "OK";
	}
}
