# MARS-UCA - v1.7.0 - [Download](https://raw.githubusercontent.com/holden-davis-uca/MARS-UCA/main/MARS-UCA.jar)
![image](https://user-images.githubusercontent.com/59069546/217987334-849338e3-199e-48c3-b24b-714b06524b68.png)

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

## Changelog
List of modifications from the original version currently include:

### [v1.7.0](https://github.com/holden-davis-uca/MARS-UCA/releases/tag/v1.7.0)
* **Program Changes**:
  * Added the ability to close an opened file by clicking on the x icon on the file tab - Basic implementation credit goes to [Oracle Forums User 843804](https://forums.oracle.com/ords/apexds/post/jtabbedpane-with-close-icons-4030)
  * Added the ability to use Windows snap layouts like most programs
* *Project Changes*:
  * Removed unnecessary inclusions to bring jar file size down by >20%
  * Added GitHub workflows to build and test against multiple different Java versions, deploy jar to github packages and create relase

### [v1.6.0](https://github.com/holden-davis-uca/MARS-UCA/releases/tag/v1.6.0)
* *Project Changes*:
  * Separated source and resource files to match typical Java project structure
  * Added Maven build system to project

### [v1.5.0](https://github.com/holden-davis-uca/MARS-UCA/releases/tag/v1.5.0)
* *Project Changes*:
  * Restructured repository to work as an IntelliJ IDEA project
  * Changed build settings to run program in IDEA and export runnable jar without requiring a .bat script
  * Recompiled attached .jar file to Java 8

### [v1.4.0](https://github.com/holden-davis-uca/MARS-UCA/releases/tag/v1.4.0)
* *Project Changes*:
  * Added a OpenJDK version of the program which should run natively on all UCA computers

### [v1.3.0](https://github.com/holden-davis-uca/MARS-UCA/releases/tag/v1.3.0)
* **Program Changes**:
  * File browser moved to top panel alongside edit and execute tabs
  * Added the ability to load files for editing directly from the file browser
    * Double-click a file to open it in the main window
  * Files now save with proper name instead of mips1.asm when using "Save As" option
  * Added FlatLAF theming options courtesy of [aeris170](https://github.com/aeris170/MARS-Theme-Engine)
    * Select "Settings" from the menu bar and then "Themes..." from the dropdown menu to change themes

### [v1.2.0](https://github.com/holden-davis-uca/MARS-UCA/releases/tag/v1.2.0)
* **Program Changes**:
  * Implemented file browser tab in messages pane  - Basic implementation credit goes to [Ian Darwin](http://www.java2s.com/Code/Java/Swing-JFC/DisplayafilesysteminaJTreeview.htm)

### [v1.1.0](https://github.com/holden-davis-uca/MARS-UCA/releases/tag/v1.1.0)
* **Program Changes**:
  * Replaced JFileChooser with FileDialog as the default file opening choice
  * Replaced JFileChooser with FileDialog as the default file saving choice

### [v1.0.0](https://github.com/holden-davis-uca/MARS-UCA/releases/tag/v1.0.0)
* **Program Changes**:
  * Added a Decimal column to the registers window
