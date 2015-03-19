public class DataGroup {
	public String 	id,
					title,
					label,
					instanceOf;
	
	public DataGroup(String id, String title, String label) {
		this.id = id;
		this.title = title;
		this.label = label;
		this.instanceOf = null;
	}
	
	public DataGroup(String id, String title, String label, String instanceOf) {
		this.id = id;
		this.title = title;
		this.label = label;
		this.instanceOf = instanceOf;
	}
}
