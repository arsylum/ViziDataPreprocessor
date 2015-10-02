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
	
	// TODO make editable for property filter ability
	public final String[] PROPCOL = new String[]{
			"id"
	};
	
	private Set<DataSet>	datasets;
	
	
	//private HashMap<Integer, String[]> propList;	// ref int -> properties[]
	//private Set test = new LinkedHashMap();
	private HashMap<ItemIntValue, String[]> propList;
	private HashMap<ItemIntValue, Integer> propMap;	// itemID  -> ref int
	
	
	public HashMap<ItemIntValue, Integer> getPropMap() {
		return propMap;
	}

	public HashMap<ItemIntValue, String[]> getPropList() {
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
		initDatasets();
	}
	
	public DataGroup(String id, String title, String label, String instanceOf) {
		this.id = id;
		this.title = title;
		this.label = label;
		this.instanceOf = instanceOf;
		initDatasets();
	}
	
	private void initDatasets() {
		datasets = new HashSet<DataSet>();
		//propMap = new HashMap<ItemIntValue, Integer>();
		propList = new HashMap<ItemIntValue, String[]>();
	}

	public Set<DataSet> getDatasets() {
		return datasets;
	}

	public void setDatasets(Set<DataSet> datasets) {
		this.datasets = datasets;
	}
	
	public void excretePropertiesFile(){
		int i; //length = this.propList.size();
		propMap = new HashMap<ItemIntValue, Integer>();
		Entry<ItemIntValue, String[]> entry;
		Iterator<HashMap.Entry<ItemIntValue, String[]>> iter = this.propList.entrySet().iterator();
		
		String filename = this.id + "_p.json";
		System.out.print("****** Starting to write file " +  filename + "...");
		JsonFactory f = new JsonFactory();
		try {
			JsonGenerator g = f.createGenerator(new File(filename), JsonEncoding.UTF8);
			g.writeStartObject();
			g.writeArrayFieldStart("properties");
				g.writeString("id");
			g.writeEndArray();
			i = -1;
			g.writeArrayFieldStart("members");
			while(iter.hasNext()) {
				
			//for(i=0; i<length; ++i) {
				entry = iter.next();
				
				g.writeStartArray();
				for(String s : entry.getValue()) { // this.getPropList().get(i)) {
					g.writeString(s);
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
