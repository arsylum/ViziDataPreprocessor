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
	
	private HashMap<Integer, Set<GeoDat>> data;
	private HashMap<Integer, Set<Double[]>>[] digest;
	
	private String		 					vxy;
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


	public HashMap<Integer, Set<GeoDat>> getData() {
		return data;
	}


	public void setData(HashMap<Integer, Set<GeoDat>> data) {
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
	
	public String getVxy() {
		return vxy;
	}


	public void setVxy(String vxy) {
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
		
		this.data = new HashMap<Integer,Set<GeoDat>>();
			
		this.length = 0;
		this.minz = Double.POSITIVE_INFINITY;
		this.maxz = Double.NEGATIVE_INFINITY;
	}
	
	
	/**
	 * take ItemIdValue from valuesnak
	 * (override default if neccessary)
	 * @return
	 */
	protected void crunshXYValue(Value value){
		if(value instanceof ItemIdValue) {
			this.vxy = ((ItemIdValue) value).getId();
		}
	}
	
	/**
	 * extract an Integer from Z value
	 * (override default if neccessary)
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
	 * (override if neccessary)
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
	 * (override if neccessary)
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
	 * can be overriden to eat something at the beginning of swallowItemDocument
	 * @param item
	 */
	protected void appetizer(ItemDocument item) {	}

	
	/**
	 * can be overriden to eat something at the end of swallowItemDocument
	 * @param item
	 */
	protected void dessert(ItemDocument item) { }
	
	
	/**
	 * ingest itemDocument to gain data
	 * @param itemDocument
	 */
	public void swallowItemDocument(ItemDocument itemDocument) {
		
		boolean wanted = false, search = true;
		vxy = null;
		vz = null;
		
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
			//ItemDataProcessor.processItemDocument();
			// TODO collect properties info?
		}
		
		// add the data if we found both values
		if(wanted && vxy != null && vz != null) {
			// insert in data list
			GeoDat g = new GeoDat(vxy, itemDocument.getItemId().getId());
			if(!data.containsKey(vz)) {
				data.put(vz, new HashSet<GeoDat>());
			}
			data.get(vz).add(g);
			// adjust dataset stats
			this.length++;
			if(minz > vz) { minz = vz; }
			if(maxz < vz) { maxz = vz; }
			
			// build the properties mappings
			if(!this.group.getPropMap().containsKey(itemDocument.getItemId().getId())) {
				HashMap<Integer, String[]> propList = this.group.getPropList();
				Integer propI = propList.size();
				propList.put(propI, new String[]{itemDocument.getItemId().getId()}); // fill in properties (still just id..)
				this.group.getPropMap().put(itemDocument.getItemId().getId(), propI);
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
	
	
	public void excreteFile(int dataSetIndex, HashMap<String, GlobeCoordinatesValue> coords, String filename) {
		digestData(coords);
		
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
						for(Double d : geoEntry) {
							dsg.writeNumber(d);
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
	
	private void digestData(HashMap<String, GlobeCoordinatesValue> coords) {
		int tileWidth = (GEO_WMAX - GEO_WMIN) / GEO_TILE_COUNT;
		Set<GeoDat> geodats;
		GlobeCoordinatesValue cv;
		Integer propI;
		digest = new HashMap[GEO_TILE_COUNT];
		for(int i = 0; i < GEO_TILE_COUNT; i++) {
			digest[i] = new HashMap<Integer, Set<Double[]>>();
		}
		for(Integer k : data.keySet()) {
			geodats = data.get(k);
			for(GeoDat gd : geodats) {
				cv = coords.get(gd.getLocation());
				if(cv != null) {
					propI = this.group.getPropMap().get(gd.getSubject());
					
					int tile = 0;
					double lon = cv.getLongitude()/(double)GlobeCoordinatesValue.PREC_DEGREE;
					double lat = cv.getLatitude()/(double)GlobeCoordinatesValue.PREC_DEGREE;
					while(lon > (GEO_WMIN+(tile+1)*tileWidth)) { tile++; }
					
					// safety check
					if(tile >= GEO_TILE_COUNT) {
						System.out.println("### STRANGE VALUE: ");
						System.out.println("### "+ gd.getSubject() + " lon: " + lon + " lat: "+ lat);
						tile = GEO_TILE_COUNT - 1;
					}
					
					if(!digest[tile].containsKey(k)) {
						digest[tile].put(k, new HashSet<Double[]>());
					}
					digest[tile].get(k).add(new Double[]{lon,lat,propI.doubleValue()});
				} else {
					//System.out.println("No coords for item "+ gd.getSubject().getId()+ " at location "+gd.getLocation().getId());
				}
			}
		}
		data = null; // remove data reference from our digestive system so the gc can flush it down
	}
	
}