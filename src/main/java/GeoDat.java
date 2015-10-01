import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;

public class GeoDat {
	private ItemIdValue location,
						subject;

	public ItemIdValue getLocation() {
		return location;
	}

	public void setLocation(ItemIdValue location) {
		this.location = location;
	}

	public ItemIdValue getSubject() {
		return subject;
	}

	public void setSubject(ItemIdValue subject) {
		this.subject = subject;
	}
	
	public GeoDat(ItemIdValue loc, ItemIdValue subj) {
		this.location = loc;
		this.subject = subj;
	}
	
}
