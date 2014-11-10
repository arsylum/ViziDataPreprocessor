
import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.json.JSONArray;
import org.json.JSONObject;
import org.wikidata.wdtk.datamodel.interfaces.EntityDocumentProcessor;
import org.wikidata.wdtk.datamodel.interfaces.GlobeCoordinatesValue;
import org.wikidata.wdtk.datamodel.interfaces.ItemDocument;
import org.wikidata.wdtk.datamodel.interfaces.ItemIdValue;
import org.wikidata.wdtk.datamodel.interfaces.PropertyDocument;
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
 * @version 0.1
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
		System.out.println("*** ViziData Dump Data Extractor V0.1");
		System.out.println("***");
		System.out.println("*** This program will chew on Wikidatas data and create");
		System.out.println("*** the JSON files that can be feed to the ViziData web application.");
		System.out.println("***");
		System.out.println("*** It's currently very basic and poorly written (much hardcode)");
		System.out.println("*** and gets the birth and death data of all humans.");
		System.out.println("*** Just be patient.");
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
			GEO_TILE_COUNT = 36, // number of slices for geodata grouping
			GEO_WMIN = -180,
			GEO_WMAX = 180;
		
		private static final JSONObject DEFAULT_COLOR_SCALE = new JSONObject()
			.put("min", new JSONArray()
				.put(134).put(205).put(215))
			.put("max", new JSONArray()
				.put(0).put(0).put(0));
		
		private int itemCount = 0;
		
		// all coordinate values
		HashMap<String,GlobeCoordinatesValue> coords = new HashMap<String,GlobeCoordinatesValue>(); // coordinates of items
		
		// for data gathering, should be made more dynamic at some point
		Set<String> humans = new HashSet<String>();
		HashMap<String,String> h_placeOfBirth = new HashMap<String, String>();
		HashMap<String,String> h_placeOfDeath = new HashMap<String, String>();
		HashMap<String,Long> h_dateOfBirth = new HashMap<String, Long>();
		HashMap<String,Long> h_dateOfDeath = new HashMap<String, Long>();	
		
		/**
		 * TODO this function is almost completely hard coded
		 * better approach would be to define a static configuration object
		 * about which datagroups and datasets to build and work it all from there
		 */
		@Override
		public void processItemDocument(ItemDocument itemDocument) {
			
			ItemIdValue subj = itemDocument.getItemId();	// this item
			Value value = null; 							// all purpose value obj
			boolean is_human = false;						// flag
			
			ItemIdValue birthplace = null,					// place of birth
						deathplace = null;					// place of death
			TimeValue	birthdate = null,					// date of birth
						deathdate = null;					// date of death

			for (StatementGroup sg : itemDocument.getStatementGroups()) {
				
				// TARGET = humans
				if(sg.getProperty().getId().equals("P31")) { // instance of
					for(Statement s : sg.getStatements()) {
						if(s.getClaim().getMainSnak() instanceof ValueSnak) {
							value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
							if(value instanceof ItemIdValue) {
								if("Q5".equals(((ItemIdValue) value).getId())) { // human
									is_human = true;
								}
							}
						}
					}
				}

				///// GATHER INTEL
				// place of birth
				if(sg.getProperty().getId().equals("P19")) { // place of birth
					for(Statement s : sg.getStatements()){
						if(s.getClaim().getMainSnak() instanceof ValueSnak) {
							value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
							if(value instanceof ItemIdValue) {
								birthplace = (ItemIdValue) value;
							}
						}
					}
				}
				// place of death
				if(sg.getProperty().getId().equals("P20")) { // place of death
					for(Statement s : sg.getStatements()){
						if(s.getClaim().getMainSnak() instanceof ValueSnak) {
							value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
							if(value instanceof ItemIdValue) {
								deathplace = (ItemIdValue) value;
							}
						}
					}
				}
				// date of birth
				if(sg.getProperty().getId().equals("P569")) { // date of birth
					for(Statement s : sg.getStatements()){
						if(s.getClaim().getMainSnak() instanceof ValueSnak) {
							value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
							if(value instanceof TimeValue) {
								birthdate = (TimeValue) value;
							}
						}
					}
				}
				// date of death
				if(sg.getProperty().getId().equals("P570")) { // date of death
					for(Statement s : sg.getStatements()){
						if(s.getClaim().getMainSnak() instanceof ValueSnak) {
							value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
							if(value instanceof TimeValue) {
								deathdate = (TimeValue) value;
							}
						}
					}
				}
				/////
				
				///// general information
				// save coordinates if we find any
				if(sg.getProperty().getId().equals("P625")) { // coordinate location
					for(Statement s : sg.getStatements()){
						if(s.getClaim().getMainSnak() instanceof ValueSnak) {
							value = ((ValueSnak) s.getClaim().getMainSnak()).getValue();
							if(value instanceof GlobeCoordinatesValue) {
								coords.put(subj.getId(), (GlobeCoordinatesValue) value);
							}
						}
					}
				}
				/////
			}
			
			// EXTERMINA...erm..SAVE!
			if(is_human) {
				humans.add(subj.getId());
				if(birthplace != null) h_placeOfBirth.put(subj.getId(), birthplace.getId());
				if(deathplace != null) h_placeOfDeath.put(subj.getId(), deathplace.getId());
				if(birthdate != null) h_dateOfBirth.put(subj.getId(), birthdate.getYear());
				if(deathdate != null) h_dateOfDeath.put(subj.getId(), deathdate.getYear());
			}
			
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
			System.out.println("...got "+humans.size()+" humans");
			System.out.println("...got "+(h_dateOfBirth.size()+h_dateOfDeath.size())+" time values");
			System.out.println("...got "+(h_placeOfBirth.size()+h_placeOfDeath.size())+" locations");
			System.out.println("...(also got "+coords.size()+" location-coordinate mappings)");
			System.out.println("");
		}

		//@Override
		public void finishProcessingEntityDocuments() {
			
			System.out.println("*** Finished the dump!");
			System.out.println("*** Let me build the files for you real quick...");	
			buildData();
			System.out.println("*** There you go!");
			
		}

		/**
		 * TODO this should be made much more dynamic 
		 * to automate generation of various datasets 
		 * as much as possible
		 * (still much hard code)
		 */
		private void buildData() {
			GlobeCoordinatesValue cv;
			Long tv;
			
			int geo_tile_width = (GEO_WMAX - GEO_WMIN)/GEO_TILE_COUNT;
			
			HashMap<String,Integer> propMap = new HashMap<String,Integer>();
			JSONArray jp_members = new JSONArray();
			
			HashMap<String, JSONObject> datasetdata = new HashMap<String, JSONObject>();
			JSONArray jd_datasets = new JSONArray();
			

			//////////////////
			/// dataset config
			/// (note: should be moved at the top of class at some point)
			Integer filecount = 0;
			DataSet d;
			Collection<DataSet> sm = new ArrayList<DataSet>();
			
			// births
			d = new DataSet();
				d.id = "birth";
				d.file = "humans_d"+String.format("%02d", filecount++)+".json";
				d.geo = h_placeOfBirth;
				d.time = h_dateOfBirth;
				d.strings = new JSONObject()
					.put("label", "births")
					.put("desc", "Place and time of birth")
					.put("term", "were born");		
				d.colorScale = DEFAULT_COLOR_SCALE;
			sm.add(d);
			
			// deaths
			d = new DataSet();
				d.id = "death";
				d.file = "humans_d"+String.format("%02d", filecount++)+".json";
				d.geo = h_placeOfDeath;
				d.time = h_dateOfDeath;
				d.strings = new JSONObject()
					.put("label", "deaths")
					.put("desc", "Place and time of death")
					.put("term", "have died");		
				d.colorScale = new JSONObject()
					.put("min", new JSONArray()
						.put(255).put(201).put(201))
					.put("max", new JSONArray()
						.put(10).put(0).put(0));
			sm.add(d);
			/// dataset config
			//////////////////
				
			
			
			for(DataSet ds : sm) {
				
				double 	min = Double.POSITIVE_INFINITY,
						max = Double.NEGATIVE_INFINITY;
				JSONObject jd = new JSONObject();
			
				for(String s : humans) {
					
					cv = coords.get(ds.geo.get(s));
					tv = ds.time.get(s);				
					
					if(cv != null && tv != null) { // is usable for viz
						
						long y = tv;
						if(min>y) { min = y; }
						if(max<y) { max = y; }
						
						int tile = 0;
						int propI;
						double lon = cv.getLongitude()/1000000000D;
						double lat = cv.getLatitude()/1000000000D;
						while(lon > (GEO_WMIN+(tile+1)*geo_tile_width)) {
							tile++;
						}
						
						// Insert in props, if no exists
						if(propMap.containsKey(s)) {
							propI = propMap.get(s);
						} else {
							jp_members.put(new JSONArray()
									.put(s)
									// TODO put all the properties we have (currently none)
							);
							propI = jp_members.length();
							propMap.put(s, propI);
						}
						
						// Insert in data, check for already existing objects
						String key = Long.toString((y));
						if(!jd.has(key)) { // key no exists, create new
							jd.put(key, new JSONArray()
								.put(tile,new JSONArray()
									.put(new JSONArray()
										.put(lon).put(lat).put(propI)
									)
								)
							);
						} else { // key exists
							JSONArray a = (JSONArray)jd.get(key);
							if(a.isNull(tile)) { // tile is empty
								a.put(tile,new JSONArray().put(new JSONArray()
										.put(lon).put(lat).put(propI)
									)
								);
							} else { // tile haz points
								((JSONArray)a.get(tile)).put(new JSONArray()
									.put(lon).put(lat).put(propI));
							}
						}
					}
				}
				
				jd_datasets.put(new JSONObject()
					.put("id", ds.id)
					.put("file", ds.file)
					.put("min", min)
					.put("max", max)
					.put("strings", ds.strings)
					.put("colorScale", ds.colorScale));
				datasetdata.put(ds.file, jd);
			}
			
			JSONObject jp = new JSONObject()	// the properties file
					.put("properties", new JSONArray()
							.put("id"))
					.put("members", jp_members);
			
			JSONObject jm = new JSONObject()	// the meta file
				.put("id", "humans")
				.put("title", "Humans")
				.put("label", "humans")
				.put("properties", "humans_p.json")
				.put("tile_width", geo_tile_width)
				//.put("tile_count", GEO_TILE_COUNT)
				.put("datasets", jd_datasets);

			writeJsonToFile("humans.json", jm);
			writeJsonToFile("humans_p.json", jp);
			for(String k : datasetdata.keySet()) {
				writeJsonToFile(k, datasetdata.get(k));
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
		
		/*private boolean writeJsonToFile(String filename, JSONArray j) {
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
			   } catch (Exception ex) {
				   ex.printStackTrace();
			   }
			}
			return true;
		}*/
	}
}
