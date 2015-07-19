import java.util.HashMap;

import org.json.JSONObject;


/**
 * very basic dataset configuration class
 * everything public for lack of proper good practices
 * @author glux
 *
 */
public class DataSet {
	
	public String 		id,
						file,
						geoProp,
						timeProp;
	
	public JSONObject 	strings,
						options;
						//colorScale;
	
	public HashMap<String,String> geo;
	public HashMap<String,Long> time;
	
	public DataGroup 	group;
	
	public double		miny,
						maxy,
						maxEventCount;
	
	public Integer		length;
	
	public DataSet(){
		this.geo = new HashMap<String,String>();
		this.time = new HashMap<String,Long>();
	}
}
