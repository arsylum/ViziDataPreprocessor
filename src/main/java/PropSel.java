import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;

/**
 * class for defining a filter property
 * @author psifi
 *
 */
public class PropSel {
	
	private String 	id,
					property;
	
	//private String[] possibleValues;
	
	public static enum TYPE {
			ID,
			INTEGER,
			STRING,
			MULTI
	};
	
	private TYPE type;

	//private TreeSet<ItemIntValue> multiValues;
	
	private ItemIntValue[] multiValues;
	
	// n Qvalues
	// map qvalue -> index
	// map index -> string, qvalue
	
	public PropSel(String id, String property, TYPE type) {
		this.id = id;
		this.property = property;
		this.type = type;
		

		if(type.equals(TYPE.MULTI)) {
			//multiValues = new TreeSet<ItemIntValue>();
		}
	}
	
	public String getId() {
		return id;
	}

	public String getProperty() {
		return property;
	}

	public ItemIntValue[] getMultiValues() {
		return multiValues;
	}
	
	public PropSel setMultiValues(ItemIntValue[] mv) {
		this.multiValues = mv;
		return this;
	}
	
	public PropSel addMultiValue(ItemIntValue id) {
		//multiValues.add(id);
		return this;
	}
	
	public Integer consume(StatementGroup sg) {
		if(this.type.equals(TYPE.MULTI)) {
			Value value;
			for(Statement s : sg.getStatements()){
				if(s.getClaim().getMainSnak() instanceof ValueSnak) {
					value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
					if(value instanceof ItemIdValue) {
						int i = multiValues.length; //-1;
						while(i>0) {
							if(((ItemIdValue) value).getId().equals(multiValues[--i].getId())) {
								return i;
			}	}	}	}	}
		} // end if multi
		
		return null;
	}
	
	public String getPropListString() {
		
		return null;
	}
	
}	
