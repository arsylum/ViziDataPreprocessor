import java.util.HashMap;

import org.json.JSONObject;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;


/**
 * very basic dataset configuration class
 * everything public for lack of proper good practices
 * @author glux
 *
 */
public class DataSet {
	
	public String 		id,
						file;
	
	public JSONObject 	strings,
						colorScale;
	
	public HashMap<String,String> geo;
	public HashMap<String,Long> time;
	
	
	public DataSet(){
		
	}
}
