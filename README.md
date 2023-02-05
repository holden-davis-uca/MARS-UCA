# MARS-UCA

## About
Modification of the [MARS](https://courses.missouristate.edu/kenvollmar/mars/) program originally written by Kenneth Vollmar and Pete Sanderson at Missouri State University.
 * DISCLAIMER: *Any and all rights are reserved by the aforementioned entities. I do not own this program, nor am I involved in its development. These modifications are provided solely for the benefit of its users.*

This version of MARS is specifically modified for usage in CSCI 2340 - Assembly Language Programming, offered by the University of Central Arkansas.

## Requirements
* Java SE 8 (JDK 8 / JRE 8) or later

## Usage

To use the program itself, download the `MARS-UCA.jar` file in the top-level directory. To run it you can either:
  * Double-click the file
  * Run `java -jar MARS-UCA.jar` in the file's directory

## Modification

This repository is structured as an IntelliJ IDEA Project and can be opened as such; the default build and run configurations run `Mars.java` which is the main execution point for the program. Additionally, the standard artifact build setting is configured to build and output a standalone runnable `.jar` file akin to the one present in this repository. Both the built project source files and built jar file are output to the `out` directory.

 If you don't want to use IntelliJ IDEA, the `src` folder contains all of the source files retrieved from decompiling the original JAR file, as well as the necessary libraries and code needed to support the latest changes.

## Changes
List of modifications from the original version currently include:
* Added a Decimal column to the registers window
* Replaced JFileChooser with FileDialog as the default file opening choice
* Replaced JFileChooser with FileDialog as the default file saving choice
* Added a file browser tab to the top pane - Basic implementation credit goes to Ian Darwin, sourced from [here](http://www.java2s.com/Code/Java/Swing-JFC/DisplayafilesysteminaJTreeview.htm)
* Added the ability to load files for editing directly from the file browser
  * Double-click a file to open it in the main window
* Added FlatLAF theming options courtesy of [aeris170](https://github.com/aeris170/MARS-Theme-Engine)
  * Select "Settings" from the menu bar and then "Themes..." from the dropdown menu to change themes
