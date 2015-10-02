

public class ItemIntValue {
	
	/**
	 * Memory efficient ItemIdValue using Integer
	 */
	
	private Integer id;

	public String getId() {
		return "Q" + id.toString();
	}

	public Integer getInt() {
		return id;
	}
	
	public void setId(Integer id) {
		this.id = id;
	}
	
	public ItemIntValue(String id) {
		this.id = Integer.valueOf(id.substring(1));
	}
	
	public ItemIntValue(Integer id) {
		this.id = id;
	}
	
	@Override
    public int hashCode() {
        return (int) id; //new HashCodeBuilder(17, 47).append(id).toHashCode();
    }
	
	@Override
    public boolean equals(Object obj) {
        if (obj == this) { return true; }
        if (obj == null) { return false; }
        if (!(obj instanceof ItemIntValue)) { return false; }
        return this.getInt().equals(((ItemIntValue) obj).getInt());
    }
}
