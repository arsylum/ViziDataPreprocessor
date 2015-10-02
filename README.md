ViziDataPreprocessor 0.3
========================

Generates the data files for [ViziData](https://github.com/gordelwig/ViziData).

## Requirements

This program uses Wikidata-Toolkit (0.5.0). If you set it up as a Maven project it will pull it's dependencies automatically. [See here](http://www.mediawiki.org/wiki/Wikidata_Toolkit) for wdtk information and setup instructions.

There have been efforts to reduce memory usage of this programm but you will need to increase your JVM heap size. Consider a minimum of roughly +500MB per extracted DataSet. If you can afford, give an extra GB to reduce garbage collection load. (e.g. -Xmx3072 for extracting 4 Datasets).