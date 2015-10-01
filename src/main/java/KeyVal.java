import java.util.HashSet;
import java.util.Set;

public class KeyVal {
	private String 	key;
	private Object  val;

	private Set<KeyVal>  keyvals;
	
	public KeyVal(String key, String val) {
		this.key = key;
		this.val = val;
	}
	
	public KeyVal(String key, Integer val) {
		this.key = key;
		this.val = val;
	}
	
	public KeyVal(String key) {
		this.key = key;
		this.keyvals = new HashSet<KeyVal>();
		//this.keyvals.add(kv);
	}

	public KeyVal add(KeyVal kv) {
		this.keyvals.add(kv);
		return this;
	}
	
	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public Object getVal() {
		return val;
	}

	public void setVal(String val) {
		this.val = val;
	}

	public Set<KeyVal> getKeyvals() {
		return keyvals;
	}

//	public void setKeyvals(KeyVal keyval) {
//		this.keyvals = keyval;
//	}
}
