# MARS-UCA - v1.6 - [Download](https://raw.githubusercontent.com/holden-davis-uca/MARS-UCA/main/MARS-UCA.jar)
![image](https://user-images.githubusercontent.com/59069546/217703453-f62f54df-b256-47b3-b661-c08ccd80de5c.png)

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

This repository is structured as an IntelliJ IDEA Maven project and can be used as such. Additionally, if IDEA is not the desired environment, the attached `jar` file can be extracted into the original source files.

## Changes
List of modifications from the original version currently include:
* Added a Decimal column to the registers window
* Replaced JFileChooser with FileDialog as the default file opening choice
* Replaced JFileChooser with FileDialog as the default file saving choice
* Added a file browser tab to the top pane - Basic implementation credit goes to [Ian Darwin](http://www.java2s.com/Code/Java/Swing-JFC/DisplayafilesysteminaJTreeview.htm)
* Added the ability to load files for editing directly from the file browser
  * Double-click a file to open it in the main window
* Added FlatLAF theming options courtesy of [aeris170](https://github.com/aeris170/MARS-Theme-Engine)
  * Select "Settings" from the menu bar and then "Themes..." from the dropdown menu to change themes
