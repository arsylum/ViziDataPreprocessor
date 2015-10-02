

public class GeoDat {
	private ItemIntValue 	location,
							subject;
	private Integer			key;

	public Integer getKey() {
		return key;
	}

	public void setKey(Integer key) {
		this.key = key;
	}

	public ItemIntValue getLocation() {
		return location;
	}

	public void setLocation(ItemIntValue location) {
		this.location = location;
	}

	public ItemIntValue getSubject() {
		return subject;
	}

	public void setSubject(ItemIntValue subject) {
		this.subject = subject;
	}
	
	public GeoDat(ItemIntValue loc, ItemIntValue subj, Integer key) {
		this.location = loc;
		this.subject = subj;
		this.key = key;
	}
	
}
