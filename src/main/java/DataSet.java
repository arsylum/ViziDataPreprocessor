import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONObject;
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
 * very basic dataset configuration class
 * everything public for lack of proper good practices
 * @author glux
 * @param <T>
 *
 */
public class DataSet {
	
	/*public static class Strings {
		private	String  label,
						zprop,
						timelineToolTip,
						desc,
						term;

		public String getLabel() { return label; }
		public void setLabel(String label) { this.label = label; }

		public String getZprop() { return zprop; }
		public void setZprop(String zprop) { this.zprop = zprop; }

		public String getTimelineToolTip() { return timelineToolTip;}
		public void setTimelineToolTip(String timelineToolTip) {this.timelineToolTip = timelineToolTip;}

		public String getDesc() {return desc;}
		public void setDesc(String desc) {this.desc = desc;}

		public String getTerm() {return term;}
		public void setTerm(String term) {this.term = term;}
	}*/
	
	////////////////////////////////
	// possible feature additions
	//////////////
	// colorScale
	// dumpDate
	
	
	private static final int 	GEO_TILE_COUNT = 36, // number of slices for geodata tiling
								GEO_WMIN = -180,
								GEO_WMAX = 180;
	
	private String 			id,
							xyProp,
							zProp;
//	private final Class<?>	xyType,
//							zType;
						
	
	private HashMap<String,String> 	strings;
	private Set<KeyVal>				options;
	
	private HashMap<Integer, Set<GeoDat>> data;
	private HashMap<Integer, Set<Double[]>>[] digest;
	
	private ItemIdValue 					vxy;
	private Integer 						vz;
	
//	public HashMap<String,String> geo;
//	public HashMap<String,Long> time;
	
	private DataGroup 	group;
	
	private double		miny,
						maxy;
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


	public double getMiny() {
		return miny;
	}


	public void setMiny(double miny) {
		this.miny = miny;
	}


	public double getMaxy() {
		return maxy;
	}


	public void setMaxy(double maxy) {
		this.maxy = maxy;
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


	/**
	 * 
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
		
//		this.data = new HashMap[GEO_TILE_COUNT-1];
//		for(int i = 0; i < GEO_TILE_COUNT; ++i) {
			this.data = new HashMap<Integer,Set<GeoDat>>();
		//};
		
//		this.geo = new HashMap<String,String>();
//		this.time = new HashMap<String,Long>();
			
		this.length = 0;
	}
	
	
	/**
	 * take ItemIdValue from valuesnak
	 * (override default if neccessary)
	 * @return
	 */
	private void crunshXYValue(Value value){
		if(value instanceof ItemIdValue) {
			this.vxy = (ItemIdValue) value;
		}
	}
	
	/**
	 * extract an Integer from Z value
	 * (override default if neccessary)
	 * default case is full year from time value
	 * @param value
	 */
	private void crunshZValue(Value value) {
		if(value instanceof TimeValue) {
			this.vz = ((Long)((TimeValue) value).getYear()).intValue();
		}
	}
	
	/**
	 * search for our data in an itemDocument
	 * @param itemDocument
	 */
	public void swallowItemDocument(ItemDocument itemDocument) {
		
		boolean wanted = false, search = true;
		vxy = null;
		vz = null;
		
		if(group.getInstanceOf() == null) {
			search = false;
			wanted = true;
		}
		
		for(StatementGroup sg : itemDocument.getStatementGroups()) {
			if(search && !wanted) {
				wanted = checkInstanceOf(sg);
			}
			chewXY(sg);
			chewZ(sg);
			//ItemDataProcessor.processItemDocument();
			// TODO collect properties info?
		}
		
		// add the data if we found both values
		if(wanted && vxy != null && vz != null) {
			GeoDat g = new GeoDat(vxy, itemDocument.getItemId());
			if(!data.containsKey(vz)) {
				data.put(vz, new HashSet<GeoDat>());
			}
			data.get(vz).add(g);
			this.length++;
			
			// build the properties mappings
			if(!this.group.getPropMap().containsKey(itemDocument.getItemId())) {
				HashMap<Integer, String[]> propList = this.group.getPropList();
				Integer propI = propList.size();
				propList.put(propI, new String[]{itemDocument.getItemId().getId()}); // fill in properties (still just id..)
				this.group.getPropMap().put(itemDocument.getItemId(), propI);
			}
		}
	}
	
	private boolean checkInstanceOf(StatementGroup sg) {
		Value value;
		if(sg.getProperty().getId().equals("P31")) { // instance of
			for(Statement s : sg.getStatements()) {
				if(s.getClaim().getMainSnak() instanceof ValueSnak) {
					value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
					if(value instanceof ItemIdValue) {
						if(this.group.getInstanceOf().equals(((ItemIdValue) value).getId())) {
							return true;
		}	}	}	}	}
		return false;
	}
	
	/**
	 * look for the geo data
	 * @param sg
	 * @return
	 */
	private void chewXY(StatementGroup sg) {
		Value value;
		Class xy = null;
		if(sg.getProperty().getId().equals(this.xyProp)) {
			for(Statement s : sg.getStatements()){
				if(s.getClaim().getMainSnak() instanceof ValueSnak) {
					value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
					crunshXYValue(value);
				}
			}
		}
	}
	
	
	/**
	 * look for the z axis data
	 * @param sg
	 * @return
	 */
	private void chewZ(StatementGroup sg) {
		Value value = null;
		//ItemIdValue z;
		if(sg.getProperty().getId().equals(this.zProp)) {
			for(Statement s : sg.getStatements()){
				if(s.getClaim().getMainSnak() instanceof ValueSnak) {
					value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
					crunshZValue(value);
				}
			}
		}
	}
	
	public void excreteFile(int dataSetIndex, HashMap<String, GlobeCoordinatesValue> coords, String filename) {
		digestData(coords);
		
		JsonFactory f = new JsonFactory();
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
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			digest = null; // can cut the reference to our excrete now. feel lighter!
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
		//Iterator<Integer> iter data.keySet().iterator();
		for(Integer k : data.keySet()) {
			//geodats = data.remove(k);
			geodats = data.get(k);
			for(GeoDat gd : geodats) {
				cv = coords.get(gd.getLocation().getId());
				if(cv != null) {
					propI = this.group.getPropMap().get(gd.getSubject());
					
					int tile = 0;
					double lon = cv.getLongitude()/(double)GlobeCoordinatesValue.PREC_DEGREE;
					double lat = cv.getLatitude()/(double)GlobeCoordinatesValue.PREC_DEGREE;
					while(lon > (GEO_WMIN+(tile+1)*tileWidth)) {
						tile++;
					}
					
					// safety check
					if(tile >= GEO_TILE_COUNT) {
						System.out.println("### STRANGE VALUE: ");
						System.out.println("### "+ gd.getSubject().getId() + " lon: " + lon + " lat: "+ lat);
						tile = GEO_TILE_COUNT - 1;
					}
					
					if(!digest[tile].containsKey(k)) {
						HashSet<Double[]> dat = new HashSet<Double[]>();
						dat.add(new Double[]{lon,lat,propI.doubleValue()});
						digest[tile].put(k, dat);
					}
				} else {
					System.out.println("No coords for item "+ gd.getSubject().getId()+ " at location "+gd.getLocation().getId());
				}
			}
		}
		data = null; // remove data reference from our digestive system so the gc can flush it down
	}
	
}