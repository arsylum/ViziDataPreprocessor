
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.MonolingualTextValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
import org.wikidata.wdtk.datamodel.interfaces.QuantityValue;
import org.wikidata.wdtk.datamodel.interfaces.Statement;
import org.wikidata.wdtk.datamodel.interfaces.StatementGroup;
import org.wikidata.wdtk.datamodel.interfaces.TimeValue;
import org.wikidata.wdtk.datamodel.interfaces.Value;
import org.wikidata.wdtk.datamodel.interfaces.ValueSnak;
import org.wikidata.wdtk.dumpfiles.DumpProcessingController;
import org.wikidata.wdtk.dumpfiles.MwRevision;
import org.wikidata.wdtk.dumpfiles.MwRevisionProcessor;
import org.wikidata.wdtk.dumpfiles.StatisticsMwRevisionProcessor;

/**
 * This program creates the JSON files for the ViziData Web Application
 * 
 * based on DumpProcessingExample by Markus Kroetzsch
 * 
 * @version 0.2
 * @author Georg Wild
 * 
 */
public class DataProcessor {

	public static void main(String[] args) throws IOException {

		// Define where log messages go
		Helper.configureLogging();

		// Print information about this program
		printDocumentation();

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
		
		edpItemStats.finishProcessingEntityDocuments();
	}

	/**
	 * Print some basic documentation about this program.
	 */
	private static void printDocumentation() {
		
		System.out.println("********************************************************************");
		System.out.println("*** ViziData Dump Data Extractor V0.2");
		System.out.println("***");
		System.out.println("*** This program will chew on Wikidatas data and create");
		System.out.println("*** the JSON files that can be feed to the ViziData web application.");
		System.out.println("***");
		System.out.println("*** The data to extract is declared in configuration objects inside");
		System.out.println("*** the ItemDataProcessors private constructor.");
		System.out.println("*** The programm is far from performance optimized and memory usage");
		System.out.println("*** can be huge, especially with multiple DataSets.");
		System.out.println("*** Heapsize of 2GB is currently recommended. (-Xmx2048M)");
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
		

		private static final int 
			GEO_TILE_COUNT = 36, // number of slices for geodata tiling
			GEO_WMIN = -180,
			GEO_WMAX = 180;
		
		/*private static final JSONObject DEFAULT_COLOR_SCALE = new JSONObject()
			.put("min", new JSONArray()
				.put(134).put(205).put(215))
			.put("max", new JSONArray()
				.put(0).put(0).put(0));*/
		
		private int itemCount = 0;
		
		private DataGroup[] groups = new DataGroup[2];			// configuration
		private Set<DataSet> dataSets = new HashSet<DataSet>(); // objects
		
		
		DataSet itemSet = new DataSet();
		DataSet popuSet = new DataSet();
				
		// all coordinate values
		private HashMap<String,GlobeCoordinatesValue> coords = new HashMap<String,GlobeCoordinatesValue>(); // coordinates of items
		
		// for data gathering, should be made more dynamic at some point
		//Set<String> humans = new HashSet<String>();
		
		// collect language codes
		Set<String> langcodes = new TreeSet<String>();
		
		/**
		 * the configuration objects
		 * (declare what the data processor shall extract)
		 * WARNING: too many DataSets at once will cause excessive memory usage!
		 */
		private ItemDataProcessor() {
			DataSet s;
			
			// DataGroups
			groups[0] = new DataGroup("humans","Humans","humans", "Q5");
			groups[1] = new DataGroup("items", "Items", "items");
			
			// DataSets
			s = new DataSet();
				s.id = "birth";
				s.group = groups[0];
				s.geoProp = "P19";
				s.timeProp = "P569";
				s.strings = new JSONObject()
					.put("label", "births")
					.put("zprop", "Year")
					.put("timelineToolTip", "%l in %x: %v")
					.put("desc", "Place and time of birth")
					.put("term", "between %l and %h");
				s.options = new JSONObject()
					.put("initSelection", new JSONObject()
							.put("min", 1700)
							.put("max", 2015));
				//s.colorScale = DEFAULT_COLOR_SCALE; // TODO ditch color scale?
			dataSets.add(s);
				
			s = new DataSet();
				s.id = "death";
				s.group = groups[0];
				s.geoProp = "P20";
				s.timeProp = "P570";
				s.strings = new JSONObject()
					.put("label", "deaths")
					.put("zprop", "Year")
					.put("timelineToolTip", "%l in %x: %v")
					.put("desc", "Place and time of death")
					.put("term", "between %l and %h");
				s.options = new JSONObject()
					.put("initSelection", new JSONObject()
						.put("min", 1700)
						.put("max", 2015));
				/*s.colorScale = new JSONObject()
					.put("min", new JSONArray()
						.put(255).put(201).put(201))
					.put("max", new JSONArray()
						.put(10).put(0).put(0));*/
			dataSets.add(s);
			
			/*s = new DataSet();
				s.id = "pubs";
				s.group = groups[1];
				s.geoProp = "P291";
				s.timeProp = "P577";
				s.strings = new JSONObject()
					.put("label", "publications")
					.put("desc", "Place and time of publication")
					.put("term", "were published");		
				/*s.colorScale = new JSONObject()
					.put("min", new JSONArray()
						.put(255).put(201).put(201))
					.put("max", new JSONArray()
						.put(10).put(0).put(0));* /
			dataSets.add(s);*/
			
			// itemSet is calculated semi hardcode
			itemSet.id = "items";
			itemSet.strings = new JSONObject()
				.put("label", "items")
				.put("zprop", "Sitelinks")
				.put("timelineToolTip", "%l with %x sitelinks: %v")
				.put("desc", "Any item with a coordinate location by number of interwiki links.")
				.put("term", "that <em>have a geographic location</em> and between %l and %h sitelinks.");
			itemSet.options = new JSONObject()
				.put("initSelection", new JSONObject()
					.put("min", 0)
					.put("max", 336));
			
			// same with popuset
			popuSet.id = "habitats";
			popuSet.strings = new JSONObject()
				.put("label", "places")
				.put("zprop", "population")
				.put("timelineToolTip", "%l with about %x inhabitants: %v")
				.put("desc", "Inhabited places by approximate population.")
				.put("term",  "that have between %l and %h residents.");
			popuSet.options = new JSONObject()
				.put("initSelection", new JSONObject()
					.put("min", 0)
					.put("max", 1000000));
		}
		
		/**
		 * processes an Item
		 * cycle over each defined DataSet and tries to collect the related data
		 */
		@Override
		public void processItemDocument(ItemDocument itemDocument) {
			
			ItemIdValue subj = itemDocument.getItemId();	// this item
			Value value = null; 							// all purpose value obj
			boolean matching_target = false;				// instanceOf flag
			ItemIdValue geoVal;								
			TimeValue	timeVal;
			
			
			Map<String,MonolingualTextValue> labels = itemDocument.getLabels();
			for(String s : labels.keySet()) {
				if(!langcodes.contains(s)) {
					langcodes.add(s);
				}
			}
			
			for(DataSet d : dataSets) {
				// init vars
				matching_target = (d.group.instanceOf == null);
				geoVal = null;
				timeVal = null;
				
				// cycle over statements
				for (StatementGroup sg : itemDocument.getStatementGroups()) {
					
					// check if we have item of the right class
					if(!matching_target) {
						if(sg.getProperty().getId().equals("P31")) { // instance of
							for(Statement s : sg.getStatements()) {
								if(s.getClaim().getMainSnak() instanceof ValueSnak) {
									value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
									if(value instanceof ItemIdValue) {
										if(d.group.instanceOf.equals(((ItemIdValue) value).getId())) {
											matching_target = true;
										}
									}
								}
							}
						}
					}
					// save the geo property data
					if(sg.getProperty().getId().equals(d.geoProp)) {
						for(Statement s : sg.getStatements()){
							if(s.getClaim().getMainSnak() instanceof ValueSnak) {
								value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
								if(value instanceof ItemIdValue) {
									geoVal = (ItemIdValue) value;
								}
							}
						}
					}
					// save the time property data
					if(sg.getProperty().getId().equals(d.timeProp)) {
						for(Statement s : sg.getStatements()){
							if(s.getClaim().getMainSnak() instanceof ValueSnak) {
								value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
								if(value instanceof TimeValue) {
									timeVal = (TimeValue) value;
								}
							}
						}
					}
					
					// save any coordinate value that we don't have
					if(!coords.containsKey(subj.getId())) {
						if(sg.getProperty().getId().equals("P625")) { // coordinate location
							for(Statement s : sg.getStatements()){
								if(s.getClaim().getMainSnak() instanceof ValueSnak) {
									value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
									if(value instanceof GlobeCoordinatesValue) {
										coords.put(subj.getId(), (GlobeCoordinatesValue) value);
										itemSet.time.put(subj.getId(), (long)itemDocument.getSiteLinks().size());
									}
								}
							}
						}
					}
					
					if(!popuSet.time.containsKey(subj.getId())) {
						if(sg.getProperty().getId().equals("P1082")) { // population
							for(Statement s : sg.getStatements()) {
								if(s.getClaim().getMainSnak() instanceof ValueSnak) {
									value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
									if(value instanceof QuantityValue) {
										popuSet.time.put(subj.getId(), approximatePopulation(((QuantityValue)value).getNumericValue().longValue()));
									}
								}
							}
						}
					}
				}
				
				
				if(matching_target && geoVal != null && timeVal != null) {
					// TODO so what about collecting other information?
					d.geo.put(subj.getId(), geoVal.getId());
					d.time.put(subj.getId(), timeVal.getYear());
				}
				
				itemCount++;
				if(itemCount%100000 == 0) {
					printStatus();
				}
				
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
			// TODO some informative output may be nice
			//System.out.println("...got "+humans.size()+" humans");
			//System.out.println("...got "+(h_dateOfBirth.size()+h_dateOfDeath.size())+" time values");
			//System.out.println("...got "+(h_placeOfBirth.size()+h_placeOfDeath.size())+" locations");
			System.out.println("...(also got "+coords.size()+" location-coordinate mappings)");
			System.out.println("");
		}

		//@Override
		public void finishProcessingEntityDocuments() {
			
			// make itemSet ready
			// very dirty workaround
			for(String s : coords.keySet()) { 
				itemSet.geo.put(s,s);	
				popuSet.geo.put(s,s);
				
			}
			itemSet.group = groups[1];
			dataSets.add(itemSet);
			
			popuSet.group = groups[1];
			dataSets.add(popuSet);
			
			
			System.out.println("*** Finished the dump!");
			System.out.println("*** List of all discovered languace codes:");
			for(String s : langcodes) {
				System.out.println(s);
			}
			System.out.println("*** ");
			System.out.println("*** Let me build the files for you real quick...");	
			buildData();
			System.out.println("*** There you go!");
			
		}

		/**
		 * builds the collected data into JSONObjects and writes them to the disk
		 */
		private void buildData() {
			GlobeCoordinatesValue cv;
			Long tv;
			
			Integer dataset_length;
			
			int geo_tile_width = (GEO_WMAX - GEO_WMIN)/GEO_TILE_COUNT;
						
			// used fill up the properties during processing
			HashMap<String, HashMap<String,Integer>> gPropMaps = new HashMap<String, HashMap<String,Integer>>();
			HashMap<String, JSONArray> gProps = new HashMap<String, JSONArray>();
			
			// map of the groups of all the data sets
			HashMap<String, HashMap<DataSet, JSONArray>> groupMap = new HashMap<String, HashMap<DataSet, JSONArray>>();

			for(DataSet s : dataSets) {
				// initializing things
				String gid = s.group.id;
				
				if(!gPropMaps.containsKey(gid)) {
					gPropMaps.put(gid, new HashMap<String,Integer>());
				}
				if(!gProps.containsKey(gid)) {
					gProps.put(gid, new JSONArray());
				}
				HashMap<String,Integer> propMap = gPropMaps.get(gid);
				JSONArray props = gProps.get(gid);
				
				double 	miny = Double.POSITIVE_INFINITY,
						maxy = Double.NEGATIVE_INFINITY;
				HashMap<Long,Long> yearEventCount = new HashMap<Long,Long>();

				// create and initialize dataset array
				JSONArray jd = new JSONArray();
				for(int i = 0; i < GEO_TILE_COUNT; i++) {
					jd.put(i, new JSONObject());
				}
				
							
				for(String h : s.time.keySet()) {
					cv = coords.get(s.geo.get(h));
					tv = s.time.get(h);
					
					if(cv != null && tv != null && cv.getGlobe().equals(GlobeCoordinatesValue.GLOBE_EARTH)) { // TODO handle "unrealistic" values in the future?
						if(miny > tv) { miny = tv; }
						if(maxy < tv) { maxy = tv; }
						yearEventCount.put(tv, yearEventCount.containsKey(tv) ? yearEventCount.get(tv)+1L : 1L);						
						
						int tile = 0;
						int propI;
						double lon = cv.getLongitude()/(double)GlobeCoordinatesValue.PREC_DEGREE;///1000000000D;
						double lat = cv.getLatitude()/(double)GlobeCoordinatesValue.PREC_DEGREE;///1000000000D;
						while(lon > (GEO_WMIN+(tile+1)*geo_tile_width)) {
							tile++;
						}
						
						if(tile >= GEO_TILE_COUNT) {
							System.out.println("### STRANGE VALUE: ");
							System.out.println("### "+ h + " lon: " + lon + " lat: "+ lat);
							tile = GEO_TILE_COUNT - 1;
						}
						
						// Insert in props, if no exists
						if(propMap.containsKey(h)) {
							propI = propMap.get(h);
						} else {
							propI = props.length();
							propMap.put(h, propI);
							props.put(new JSONArray()
									.put(h)
									// TODO put all the properties we have (currently none)
							);
						}
						
						// Insert in data, check for already existing objects
						String key = Long.toString(tv);
						JSONArray geodata = new JSONArray()
							.put(lon).put(lat).put(propI);
						
						if(!jd.getJSONObject(tile).has(key)) { // !key exists
							jd.getJSONObject(tile).put(key, new JSONArray()
								.put(geodata)
							);
						} else {
							((JSONArray)jd.getJSONObject(tile).get(key))
								.put(geodata);
						}
						
					}
				}
				
				s.length = s.time.size();
				
				s.maxy = maxy;
				s.miny = miny;
				s.maxEventCount = 0;
				for(Long c : yearEventCount.values()) {
					if(c.doubleValue() > s.maxEventCount) {
						s.maxEventCount = c.doubleValue();
					}
				}
				
				if(!groupMap.containsKey(gid)) {
					groupMap.put(gid, new HashMap<DataSet,JSONArray>());
				}
				groupMap.get(gid).put(s, jd);
			}
			
			// generate the output
			for(Entry<String, HashMap<DataSet, JSONArray>> ge : groupMap.entrySet()) {
				DataGroup g = null;
				JSONObject jg = new JSONObject(); // the meta file
				JSONArray jgds = new JSONArray(); // the datasets array
				int setcount = 0;
				
				for(Entry<DataSet, JSONArray> se : ge.getValue().entrySet()) {
					DataSet d = se.getKey();
					JSONArray data = se.getValue();
					if(g == null) {
						g = d.group;
					}
					//d.file = "humans_d"+String.format("%02d", filecount++)+".json";
					String dataFileName = g.id+"_d"+String.format("%02d", setcount++)+".json";
					
					JSONObject ds = new JSONObject()
						.put("id", d.id)
						.put("total_points", d.length)
						.put("min", d.miny)
						.put("max", d.maxy)
						.put("maxEventCount", d.maxEventCount)
						.put("strings", d.strings)
						.put("options", d.options)
						.put("file", dataFileName);
						//.put("colorScale", d.colorScale));
					jgds.put(ds);
					writeJsonToFile(dataFileName, data);
				}
					
				String groupFileName = g.id+".json";
				String groupPropsFileName = g.id+"_p.json";
				jg.put("id", g.id)
					.put("title", g.title)
					.put("label", g.label)
					.put("properties", groupPropsFileName)
					.put("tile_width", geo_tile_width)
					//.put("tile_count", GEO_TILE_COUNT)
					.put("datasets", jgds);
				
				JSONObject props = new JSONObject()
					.put("properties", new JSONArray()
						.put("id")) // TODO someday we will actually collect something else! maybe
					.put("members", gProps.get(g.id));
				
				writeJsonToFile(groupPropsFileName, props);
				writeJsonToFile(groupFileName, jg);
				
			}
			return;
		}
		
		
		/**
		 * writes given json object to file
		 * @param filename
		 * @param j
		 * @return !always true
		 */
		private boolean writeJsonToFile(String filename, JSONObject j) {
			System.out.print("*** writing file "+filename+" to disk...");
			Writer writer = null;
			try {
			    writer = new BufferedWriter(new OutputStreamWriter(
			          new FileOutputStream(filename), "utf-8"));
			    j.write(writer);
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
			   try {
				   writer.close();
				   System.out.println("done.");
			   } catch (Exception ex) {
				   ex.printStackTrace();
			   }
			}
			return true;
		}
		
		private boolean writeJsonToFile(String filename, JSONArray j) {
			System.out.print("*** writing file "+filename+" to disk...");
			Writer writer = null;
			try {
			    writer = new BufferedWriter(new OutputStreamWriter(
			          new FileOutputStream(filename), "utf-8"));
			    j.write(writer);
			} catch (IOException ex) {
				ex.printStackTrace();
			} finally {
			   try {
				   writer.close();
				   System.out.println("done.");
			   } catch (Exception ex) {
				   ex.printStackTrace();
			   }
			}
			return true;
		}
		
		private long approximatePopulation(long pop) {
			long ret = 0, split;
			
			long[] aggregates = new long[]{
					1,2,5,10,20,50,100,200,500,1000,2000,5000,10000,20000,50000,
					100000,200000,500000,1000000,2000000,5000000,10000000,50000000,100000000,1000000000};
			for(long v : aggregates) {
				split = (long)Math.ceil(ret + (v-ret/2));
				if(pop >= split) { ret = v; }
			}
			return ret;
		}
	}
}
