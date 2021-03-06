import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;




/**
 * DataSet Class
 * manages dataset construction
 * @author glux
 *
 */
public class DataSet {
	
	////////////////////////////////
	// possible feature additions
	//////////////
	//
	// dumpDate
	
	
	private static final int 	GEO_TILE_COUNT = 36, // number of slices for geodata tiling
								GEO_WMIN = -180,
								GEO_WMAX = 180;
	
	private String 			id,
							xyProp,
							zProp;
						
	private HashMap<String,String> 	strings;
	private Set<KeyVal>				options;
	
	private HashSet<GeoDat> data;
	private HashMap<Integer, Set<Double[]>>[] digest;
	
	private ItemIntValue 					vxy;
	private Integer 						vz;
	
	private DataGroup 	group;
	
	private double		minz,
						maxz;
						//maxEventCount;
	
	private Integer		length;
	
	
	public HashMap<String, String> getStrings() {
		return strings;
	}


	public DataSet setStrings(HashMap<String, String> strings) {
		this.strings = strings;
		return this;
	}


	public Set<KeyVal> getOptions() {
		return options;
	}


	public DataSet setOptions(Set<KeyVal> options) {
		this.options = options;
		return this;
	}


	public HashSet<GeoDat> getData() {
		return data;
	}


	public void setData(HashSet<GeoDat> data) {
		this.data = data;
	}


	public double getMinz() {
		return minz;
	}


	public void setMinz(double miny) {
		this.minz = miny;
	}


	public double getMaxz() {
		return maxz;
	}


	public void setMaxz(double maxy) {
		this.maxz = maxy;
	}


	public Integer getLength() {
		return length;
	}


	public void setLength(Integer length) {
		this.length = length;
	}


	public static int getGeoTileCount() {
		return GEO_TILE_COUNT;
	}


	public static int getGeoWmin() {
		return GEO_WMIN;
	}


	public static int getGeoWmax() {
		return GEO_WMAX;
	}


	public String getId() {
		return id;
	}


	public String getXyProp() {
		return xyProp;
	}


	public String getzProp() {
		return zProp;
	}


	public DataGroup getGroup() {
		return group;
	}
	
	public ItemIntValue getVxy() {
		return vxy;
	}


	public void setVxy(ItemIntValue vxy) {
		this.vxy = vxy;
	}


	public Integer getVz() {
		return vz;
	}


	public void setVz(Integer vz) {
		this.vz = vz;
	}



	/**
	 * @param id
	 * @param group 
	 * @param zProp z axis numeric value property (time/other)
	 * @param xyProp xy axis geo coorinates value property
	 */
	public DataSet(String id, DataGroup group, String zProp, String xyProp){
		
		this.id = id;
		this.group = group;
		this.zProp = zProp;
		this.xyProp = xyProp;
		
		this.data = new HashSet<GeoDat>();
			
		this.length = 0;
		this.minz = Double.POSITIVE_INFINITY;
		this.maxz = Double.NEGATIVE_INFINITY;
	}
	
	
	/**
	 * take ItemIdValue from value
	 * (override default if necessary)
	 * @return
	 */
	protected void crunshXYValue(Value value){
		if(value instanceof ItemIdValue) {
			this.vxy =new ItemIntValue(((ItemIdValue) value).getId());
		}
	}
	
	/**
	 * extract an Integer from Z value
	 * (override default if necessary)
	 * default case is full year from time value
	 * @param value
	 */
	protected void crunshZValue(Value value) {
		if(value instanceof TimeValue) {
			this.vz = ((Long)((TimeValue) value).getYear()).intValue();
		}
	}
	
	/**
	 * look for the geo data
	 * (override if necessary)
	 * @param sg
	 * @return
	 */
	protected void chewXY(StatementGroup sg) {
		Value value;
		for(Statement s : sg.getStatements()){
			if(s.getClaim().getMainSnak() instanceof ValueSnak) {
				value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
				crunshXYValue(value);
		}	}
	}
	
	
	/**
	 * look for the z axis data
	 * (override if necessary)
	 * @param sg
	 * @return
	 */
	protected void chewZ(StatementGroup sg) {
		Value value = null;
		for(Statement s : sg.getStatements()){
			if(s.getClaim().getMainSnak() instanceof ValueSnak) {
				value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
				crunshZValue(value);
		}	}
	}
	
	
	/**
	 * can be overridden to eat something at the beginning of swallowItemDocument
	 * @param item
	 */
	protected void appetizer(ItemDocument item) {	}

	
	/**
	 * can be overridden to eat something at the end of swallowItemDocument
	 * @param item
	 */
	protected void dessert(ItemDocument item) { }
	
	
	/**
	 * Ingest itemDocument to gain data
	 * @param itemDocument
	 */
	public void swallowItemDocument(ItemDocument itemDocument) {
		
		boolean wanted = false, search = true;
		vxy = null;
		vz = null;
		ItemIntValue id = new ItemIntValue(itemDocument.getItemId().getId());
		
		HashMap<ItemIntValue, Integer[]> propList = this.group.getPropList();
		boolean needProps = !propList.containsKey(id);
		PropSel[] pss = this.group.getPROPCOL();
		Integer[] propVals = new Integer[pss.length+1];
		
		appetizer(itemDocument);
		
		if(group.getInstanceOf() == null) {
			search = false;
			wanted = true;
		}
		
		for(StatementGroup sg : itemDocument.getStatementGroups()) {
			if(search && !wanted && sg.getProperty().getId().equals("P31")) {
				wanted = checkInstanceOf(sg);
			} else if (sg.getProperty().getId().equals(this.xyProp)) {
				chewXY(sg);
			} else if (sg.getProperty().getId().equals(this.zProp)) {
				chewZ(sg);
			}
			
			if(needProps) { // collect wanted properties
				int i = pss.length;
				while(i>0) {
					if(sg.getProperty().getId().equals(pss[--i].getProperty())) {
						propVals[i+1] = pss[i].consume(sg);
			} 	}	}
		}
		
		// add the data if we found both values
		if(wanted && vxy != null && vz != null) {
			// insert in data list
			data.add(new GeoDat(vxy, id, vz));
			
			// build the properties mappings
			if(needProps) {
				propVals[0] = id.getInt();
				propList.put(id, propVals);
			}
		}
		dessert(itemDocument);
	}
	
	private boolean checkInstanceOf(StatementGroup sg) {
		Value value;
		for(Statement s : sg.getStatements()) {
			if(s.getClaim().getMainSnak() instanceof ValueSnak) {
				value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
				if(value instanceof ItemIdValue) {
					if(this.group.getInstanceOf().equals(((ItemIdValue) value).getId())) {
						return true;
		}	}	}	}
		return false;
	}
	
	
	public void excreteFile(int dataSetIndex, HashMap<ItemIntValue, CoordinatesValue> coords, HashMap<ItemIntValue,ItemIntValue> administratives, String filename) {
		digestData(coords, administratives);
		
		JsonFactory f = new JsonFactory();
		System.out.print("****** Starting to write file "+filename+" ...");
		try {
			JsonGenerator dsg = f.createGenerator(
					new File(filename),
					JsonEncoding.UTF8);
	
			dsg.writeStartArray();
			for(HashMap<Integer,Set<Double[]>> tile : digest) {
				dsg.writeStartObject();
				for(Integer i : tile.keySet()) {
					dsg.writeArrayFieldStart(i.toString());
					for(Double[] geoEntry : tile.get(i)){
						dsg.writeStartArray();
						for(int j = 0; j < geoEntry.length; j++) {
							if(j < geoEntry.length -1 ) { dsg.writeNumber(geoEntry[j]);	} 
							else { dsg.writeNumber(geoEntry[j].intValue()); }
						}
						dsg.writeEndArray();
					}
					dsg.writeEndArray();
				}
				dsg.writeEndObject();
			}
			dsg.writeEndArray();
			dsg.close();
		}  catch (IOException e) {
			e.printStackTrace();
		} finally {
			digest = null; // can cut the reference to our excrete now. feel lighter!
			System.out.println("done!");
		}
	}
	
	private void digestData(HashMap<ItemIntValue, CoordinatesValue> coords, HashMap<ItemIntValue,ItemIntValue> administratives) {
		int tileWidth = (GEO_WMAX - GEO_WMIN) / GEO_TILE_COUNT;
		CoordinatesValue cv;
		Integer propI;
		ItemIntValue id;
		digest = new HashMap[GEO_TILE_COUNT];
		for(int i = 0; i < GEO_TILE_COUNT; i++) {
			digest[i] = new HashMap<Integer, Set<Double[]>>();
		}

		for(GeoDat gd : data) {
			id = gd.getLocation();
			do {
				cv = coords.get(id);
				id = administratives.get(id);
			} while (cv == null && id != null);
			if(cv != null) {
				propI = this.group.getPropMap().get(gd.getSubject());
				
				int tile = 0;
				double lon = cv.getLon()/(double)GlobeCoordinatesValue.PREC_DEGREE;
				double lat = cv.getLat()/(double)GlobeCoordinatesValue.PREC_DEGREE;
				while(lon > (GEO_WMIN+(tile+1)*tileWidth)) { tile++; }
				
				// safety check
				if(tile >= GEO_TILE_COUNT) {
					System.out.println("###########################################");
					System.out.println("### WARN dropping value outside of scope: ");
					System.out.println("### "+ gd.getSubject().getId() + ", lon: " + lon + ", lat: "+ lat);
					System.out.println("###########################################");
				} else {
					// adjust dataset stats
					this.length++;
					if(minz > gd.getKey()) { minz = gd.getKey(); }
					if(maxz < gd.getKey()) { maxz = gd.getKey(); }
					
					// ad to digest map
					if(!digest[tile].containsKey(gd.getKey())) {
						digest[tile].put(gd.getKey(), new HashSet<Double[]>());
					}
					digest[tile].get(gd.getKey()).add(new Double[]{lon,lat,propI.doubleValue()});
				}
			} else {
				//System.out.println("No coords for item "+ gd.getSubject().getId()+ " at location "+gd.getLocation().getId());
			}
		}
		data = null; // remove data reference from our digestive system so the gc can flush it down
	}
	
}