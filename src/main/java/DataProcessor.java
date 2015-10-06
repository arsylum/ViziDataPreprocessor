
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.Set;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;
import org.wikidata.wdtk.dumpfiles.DumpContentType;
import org.wikidata.wdtk.dumpfiles.DumpProcessingController;
import org.wikidata.wdtk.dumpfiles.MwRevision;
import org.wikidata.wdtk.dumpfiles.MwRevisionProcessor;
import org.wikidata.wdtk.dumpfiles.StatisticsMwRevisionProcessor;

import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;

/**
 * This program creates the JSON files for the ViziData Web Application
 * 
 * based on DumpProcessingExample by Markus Kroetzsch
 * 
 * @version 0.3
 * @author Georg Wild
 * 
 */
public class DataProcessor {

	public static void main(String[] args) throws IOException {

		// Define where log messages go
		Helper.configureLogging();

		// Print information about this program
		printDocumentation();
		
//		Set<ItemIntValue> testSet = new HashSet<ItemIntValue>();
		
//		for(Integer i = 0; i < 100000000; i++) {
//			testSet.add(new ItemIntValue("Q"+i.toString()));
//			if(i%1000000 == 0) {
//				System.out.println("made "+i+" ItemIntValues");
//			}
//		}
//		
//		System.out.println("generated useless testset");

		// Controller object for processing dumps:
		DumpProcessingController dumpProcessingController = new DumpProcessingController(
				"wikidatawiki");
		
		// // Use any download directory:
		// dumpProcessingController.setDownloadDirectory(System.getProperty("user.dir"));

		// Our local data processor class
		ItemDataProcessor edpItemStats = new ItemDataProcessor();
		// Subscribe to the most recent entity documents of type wikibase item:
		dumpProcessingController.registerEntityDocumentProcessor(edpItemStats,
				MwRevision.MODEL_WIKIBASE_ITEM, true);
		
		// General statistics and time keeping:
		MwRevisionProcessor rpRevisionStats = new StatisticsMwRevisionProcessor(
				"revision processing statistics", 10000);
		// Subscribe to all current revisions (null = no filter):
		dumpProcessingController.registerMwRevisionProcessor(rpRevisionStats,
				null, true);

		// Start processing (may trigger downloads where needed):
		// Process all recent dumps (including daily dumps as far as avaiable)
		dumpProcessingController.processMostRecentJsonDump();
		
		
		
		edpItemStats.finishProcessingEntityDocuments(
				dumpProcessingController.getMostRecentDump(DumpContentType.JSON).getDateStamp());
	}

	/**
	 * Print some basic documentation about this program.
	 */
	private static void printDocumentation() {
		
		System.out.println("********************************************************************");
		System.out.println("*** ViziData Dump Data Extractor V0.3");
		System.out.println("***");
		System.out.println("*** This program will chew on Wikidatas data and create");
		System.out.println("*** the JSON files that can be fed to the ViziData web application.");
		System.out.println("***");
		System.out.println("*** The data to produce is defined in the private constructor of");
		System.out.println("*** ItemDataProcessors by instances of (sub)class DataSet.");
		System.out.println("***");
		System.out.println("*** The program makes an effort to minimize memory usage but still");
		System.out.println("*** it can fill up rather quickly with all this data.");
		System.out.println("*** Giving at least ~500MB heap space per DataSet should be considered.");
		System.out.println("***");
		System.out.println("*** If you can afford, grant a bigger heapsize to give");
		System.out.println("*** the garbage collector some space to relax. (e.g. -Xmx4096M)");
		System.out.println("********************************************************************");
	}

	/**
	 * This class handles the actual processing of EntityDocuments and
	 * collects the information which at the end is used to build the
	 * output files
	 * 
	 * @author Georg Wild
	 * 
	 */
	static class ItemDataProcessor implements EntityDocumentProcessor {
				
		private int itemCount = 0;
		
		private Set<DataGroup> groups = new HashSet<DataGroup>();		
		// all coordinate values
		private HashMap<ItemIntValue,CoordinatesValue> coords = new HashMap<ItemIntValue,CoordinatesValue>(); // coordinates of items
		// all located in administrative territorial entity
		private HashMap<ItemIntValue, ItemIntValue> administratives = new HashMap<ItemIntValue, ItemIntValue>();
		
		// collect language codes
		//Set<String> langcodes = new TreeSet<String>();
		
		/**
		 * construct the DataSets
		 * (declare what the data processor shall extract)
		 * WARNING: expect an average of 500MB heap size per DataSet
		 */
		private ItemDataProcessor() {
		
			// DataGroups
			DataGroup g;
			DataSet s;
			HashMap<String, String> strings;
			Set<KeyVal> options;
			
			//////////
			// humans
			g = new DataGroup("humans","Humans","humans", "Q5");
			// births
			s = new DataSet("birth", g, "P569", "P19");
			strings =  new HashMap<String, String>();
			strings.put("label", "births");
			strings.put("zprop", "Year");
			strings.put("timelineToolTip", "%l in %x: %v");
			strings.put("desc", "Place and time of birth");
			strings.put("term", "between %l and %h");
			options = new HashSet<KeyVal>();
			options.add(new KeyVal("initSelection")
					.add(new KeyVal("min", 1700))
					.add(new KeyVal("max", 2015)));
			s.setStrings(strings).setOptions(options);
			g.getDatasets().add(s);
			
			// deaths
			s = new DataSet("death", g, "P570", "P20");
			strings =  new HashMap<String, String>();
			strings.put("label", "deaths");
			strings.put("zprop", "Year");
			strings.put("timelineToolTip", "%l in %x: %v");
			strings.put("desc", "Place and time of death");
			strings.put("term", "between %l and %h");
			options = new HashSet<KeyVal>();
			options.add(new KeyVal("initSelection")
					.add(new KeyVal("min", 1700))
					.add(new KeyVal("max", 2015)));
			s.setStrings(strings).setOptions(options);
			g.getDatasets().add(s);
			
			groups.add(g);
			
			/////////
			// Items
			g = new DataGroup("items", "Items", "items");
			// itemSet
			s = new DataSet("item", g, "(sitelinks)", "P625") {
				@Override
				protected void appetizer(ItemDocument item) {
					setVz(item.getSiteLinks().size());
				}
				@Override
				protected void chewXY(StatementGroup sg) {
					for(Statement s : sg.getStatements()){
						if(s.getClaim().getMainSnak() instanceof ValueSnak) {
							if(((ValueSnak) s.getClaim().getMainSnak()).getValue() instanceof GlobeCoordinatesValue) {
								setVxy(new ItemIntValue(sg.getSubject().getId()));
								return;
				}	}	}	}
			};
			strings =  new HashMap<String, String>();
			strings.put("label", "items");
			strings.put("zprop", "Sitelinks");
			strings.put("timelineToolTip", "%l with %x sitelinks: %v");
			strings.put("desc", "Any item with a coordinate location by number of interwiki links.");
			strings.put("term", "that <em>have a geographic location</em> and between %l and %h sitelinks.");
			options = new HashSet<KeyVal>();
			options.add(new KeyVal("initSelection")
					.add(new KeyVal("min", 0))
					.add(new KeyVal("max", 338)));
			s.setStrings(strings).setOptions(options);
			g.getDatasets().add(s);
			// placeSet
			s = new DataSet("habitats", g, "P1082", "P625") {
				@Override
				protected void crunshZValue(Value value){
					if(value instanceof QuantityValue) {
						setVz(((QuantityValue) value).getNumericValue().intValue());
					}
				}
				@Override
				protected void chewXY(StatementGroup sg) {
					for(Statement s : sg.getStatements()){
						if(s.getClaim().getMainSnak() instanceof ValueSnak) {
							if(((ValueSnak) s.getClaim().getMainSnak()).getValue() instanceof GlobeCoordinatesValue) {
								setVxy(new ItemIntValue(sg.getSubject().getId()));
								return;
				}	}	}	}
			};
			strings =  new HashMap<String, String>();
			strings.put("label", "places");
			strings.put("zprop", "Population");
			strings.put("timelineToolTip", "%l with %x inhabitants: %v");
			strings.put("desc", "Inhabited places by population.");
			strings.put("term", "that have between %l and %h residents.");
			options = new HashSet<KeyVal>();
			options.add(new KeyVal("initSelection")
					.add(new KeyVal("min", 0))
					.add(new KeyVal("max", 1000000)));
			s.setStrings(strings).setOptions(options);
			g.getDatasets().add(s);
			
			groups.add(g);
		}
		
		/**
		 * processes an Item
		 * cycle over each defined DataSet and tries to collect the related data
		 */
		@Override
		public void processItemDocument(ItemDocument itemDocument) {
			
			
			//ItemIdValue subj = itemDocument.getItemId();	// this item
			ItemIntValue id = new ItemIntValue(itemDocument.getItemId().getId());
			Value value = null; 							// all purpose value obj
			
			// construct list of all language labels
			/*Map<String,MonolingualTextValue> labels = itemDocument.getLabels();
			for(String s : labels.keySet()) {
				if(!langcodes.contains(s)) {
					langcodes.add(s);
				}
			}*/
			
			// let each DataSet munch on the item
			for(DataGroup g : groups) {
				for(DataSet d : g.getDatasets()) {
					d.swallowItemDocument(itemDocument);
				}
			}
			
			// collect information for location associations
			for(StatementGroup sg : itemDocument.getStatementGroups()) {
				if(!coords.containsKey(id)) {
					if(sg.getProperty().getId().equals("P625")) { // coordinate location
						for(Statement s : sg.getStatements()){
							if(s.getClaim().getMainSnak() instanceof ValueSnak) {
								value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
								if(value instanceof GlobeCoordinatesValue) {
									if(((GlobeCoordinatesValue) value).getGlobe().equals(GlobeCoordinatesValue.GLOBE_EARTH)) {
										coords.put(id, new CoordinatesValue(
												((GlobeCoordinatesValue) value).getLatitude(),
												((GlobeCoordinatesValue) value).getLongitude()));
				}	}	}	}	}	}
				if(!administratives.containsKey(id)) {
					if(sg.getProperty().getId().equals("P131")) { // located in the administrative territorial entity
						for(Statement s : sg.getStatements()){
							if(s.getClaim().getMainSnak() instanceof ValueSnak) {
								value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
								if(value instanceof ItemIdValue) {
									administratives.put(id, new ItemIntValue(((ItemIdValue) value).getId()));
				}	}	}	}	}
			}
			/*if(!coords.containsKey(id) || !administratives.containsKey(id)) {
				sgloop:
				for(StatementGroup sg : itemDocument.getStatementGroups()) {
					// collect itemId -> coordinateValue associations
					if(sg.getProperty().getId().equals("P625")) { // coordinate location
						for(Statement s : sg.getStatements()){
							if(s.getClaim().getMainSnak() instanceof ValueSnak) {
								value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
								if(value instanceof GlobeCoordinatesValue) {
									coords.put(id, (GlobeCoordinatesValue) value);
									break sgloop;
				}	}	}	}	}
			}*/
			
			itemCount++;
			if(itemCount%100000 == 0) {
				printStatus();
			}
				
		}

		@Override
		public void processPropertyDocument(PropertyDocument propertyDocument) {
			// ignore properties
			// (in fact, the above code does not even register the processor for
			// receiving properties)
		}
		
		private void printStatus() {
			System.out.println("processed "+itemCount+" items so far...");
			// TODO some informative output may be nice?
			System.out.println("...(also got "+coords.size()+" location-coordinate mappings");
			System.out.println("    and "+administratives.size()+" P131 mappings)");
			System.out.println("");
		}

		//@Override
		public void finishProcessingEntityDocuments(String dateStamp) {
			
			System.out.println("*** Finished the dump!");
			
//			System.out.println("*** List of all discovered languace codes:");
//			for(String s : langcodes) {
//				System.out.println(s);
//			}
			
			System.out.println("*** ");
			System.out.println("*** Let's put everything together...");	
			buildData(dateStamp);
			System.out.println("*** There you go!");
		}

		/**
		 * builds the collected data into JSONObjects and writes them to the disk
		 */
		private void buildData(String dateStamp) {
			
			JsonFactory f = new JsonFactory();
			JsonGenerator g;
			int dataSetIndex;
			
			for(DataGroup dg : groups) {
				
				dataSetIndex = -1;
				String gfilename = dg.getId() + ".json";
				
				try {
					System.out.println("*** Processing DataGroup "+dg.getId()+"...");
					System.out.println("****** Starting to write file "+gfilename+"...");
					g = f.createGenerator(new File(gfilename), JsonEncoding.UTF8);
					
					g.writeStartObject();
					g.writeStringField("id", dg.getId());
					g.writeStringField("title", dg.getTitle());
					g.writeStringField("label", dg.getLabel());
					g.writeStringField("properties", dg.getId() + "_p.json");
					g.writeNumberField("tile_width", 10); // TODO move to dataset? (requires change in frontend)

					// drop the accumulated properties
					dg.excretePropertiesFile();
					
					g.writeArrayFieldStart("datasets");
					for(DataSet ds : dg.getDatasets()) {
						dataSetIndex++;
						String datFilename = dg.getId() + "_d"+String.format("%02d", dataSetIndex) + ".json";
						ds.excreteFile(dataSetIndex, coords, datFilename);
						
						// write metadata
						g.writeStartObject();
						g.writeStringField("id", ds.getId());
						g.writeNumberField("total_points", ds.getLength());
						g.writeNumberField("min", ds.getMinz());
						g.writeNumberField("max", ds.getMaxz());
						g.writeStringField("dump_date", dateStamp);
						//g.writeNumberField("maxEventCount", ds.get?);
						g.writeObjectFieldStart("strings");
						for(Entry<String,String> e : ds.getStrings().entrySet()) {
							g.writeStringField(e.getKey(), e.getValue());
						}
						g.writeEndObject();
						g.writeObjectFieldStart("options");
						for(KeyVal kv : ds.getOptions()) {
							// TODO flexible?
							g.writeObjectFieldStart(kv.getKey());
							for(KeyVal kkv : kv.getKeyvals()) {
								g.writeStringField(kkv.getKey(), kkv.getVal().toString());
							}
							g.writeEndObject();
						}
						g.writeEndObject();
						g.writeStringField("file", datFilename);
						g.writeEndObject();
					}
					g.writeEndArray();
					g.writeEndObject();
					g.close();
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					System.out.println("****** Finished writing file "+gfilename);
					System.out.println("*** Finished processing Datagroup "+dg.getId());
				}
			}
		}
	}
}
