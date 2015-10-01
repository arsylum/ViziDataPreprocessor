

public class GeoDat {
	private String 	location,
					subject;

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public GeoDat(String loc, String subj) {
		this.location = loc;
		this.subject = subj;
	}
	
}
