import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Set;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

public class DataGroup {
	private String 	id,
					title,
					label,
					instanceOf;
	
	// for specifying additional property data to collect in addition to item id (which is always included)
	private PropSel[] PROPCOL;
	
	public PropSel[] getPROPCOL() {
		return PROPCOL;
	}
	
	public void setPROPCOL(PropSel[] ps) {
		this.PROPCOL = ps;
	}

	private Set<DataSet>	datasets;
	
	
	//private HashMap<Integer, String[]> propList;	// ref int -> properties[]
	//private Set test = new LinkedHashMap();
	private HashMap<ItemIntValue, Integer[]> propList;
	private HashMap<ItemIntValue, Integer> propMap;	// itemID  -> ref int
	
	
	public HashMap<ItemIntValue, Integer> getPropMap() {
		return propMap;
	}

	public HashMap<ItemIntValue, Integer[]> getPropList() {
		return propList;
	}
	
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getInstanceOf() {
		return instanceOf;
	}

	public void setInstanceOf(String instanceOf) {
		this.instanceOf = instanceOf;
	}

	
	public DataGroup(String id, String title, String label) {
		this.id = id;
		this.title = title;
		this.label = label;
		this.instanceOf = null;
		initSets();
	}
	
	public DataGroup(String id, String title, String label, String instanceOf) {
		this.id = id;
		this.title = title;
		this.label = label;
		this.instanceOf = instanceOf;
		initSets();
	}
	
	private void initSets() {
		datasets = new HashSet<DataSet>();
		//propMap = new HashMap<ItemIntValue, Integer>();
		propList = new HashMap<ItemIntValue, Integer[]>();
		PROPCOL = new PropSel[0];
		//PROPCOL = new TreeSet<PropSel>();
		//PROPCOL.add(new PropSel(null, PropSel.TYPE.ID)); // always collect id
	}

	public Set<DataSet> getDatasets() {
		return datasets;
	}

	public void setDatasets(Set<DataSet> datasets) {
		this.datasets = datasets;
	}
	
	public void excretePropertiesFile(){
		int i = -1; //length = this.propList.size();
		propMap = new HashMap<ItemIntValue, Integer>();
		Entry<ItemIntValue, Integer[]> entry;
		Iterator<HashMap.Entry<ItemIntValue, Integer[]>> iter = this.propList.entrySet().iterator();
		
		String filename = this.id + "_p.json";
		System.out.print("****** Starting to write file " +  filename + "...");
		JsonFactory f = new JsonFactory();
		try {
			JsonGenerator g = f.createGenerator(new File(filename), JsonEncoding.UTF8);
			g.writeStartObject();
			/// index lookup table part
			g.writeArrayFieldStart("properties");
				g.writeString("id");
				int propnum = this.PROPCOL.length, j = 0;
				while(j < propnum) {
					PropSel ps = this.PROPCOL[j++];
					g.writeStartArray();
						g.writeString(ps.getId());
						int n = ps.getMultiValues().length, k = 0;
						while(k < n) {
							g.writeString(ps.getMultiValues()[k++].getId());
						}
					g.writeEndArray();
				}
			g.writeEndArray();
			//i = -1;
			/// data part
			g.writeArrayFieldStart("members");
			while(iter.hasNext()) {
				
			//for(i=0; i<length; ++i) {
				entry = iter.next();
				
				g.writeStartArray();
				Integer[] objs = entry.getValue();
				int onum = objs.length, l = 0;
				while(l < onum) {
					g.writeNumber((objs[l] == null ? -1 : objs[l]));
					l++;
				}
				g.writeEndArray();
				propMap.put(entry.getKey(), ++i);
				iter.remove();
			}
			g.writeEndArray();
			g.writeEndObject();
			g.close();
		} catch(Exception e) {
			e.printStackTrace();
		} finally {
			this.propList = null; // drop the weight of propList
			System.out.println("done!");
		}
	}
}
