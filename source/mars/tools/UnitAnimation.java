package mars.tools;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import mars.Globals;

class UnitAnimation extends JPanel implements ActionListener {

	/**
	 *
	 */
	private static final long serialVersionUID = -2681757800180958534L;

	//config variables
	private final int PERIOD = 8;    // velocity of frames in ms
	private static final int PWIDTH = 1000;     // size of this panel
	private static final int PHEIGHT = 574;
	private final GraphicsConfiguration gc;
	private final GraphicsDevice gd;   	// for reporting accl. memory usage
	private final int accelMemory;
	private final DecimalFormat df;

	private int counter;			//verify then remove.
	private boolean justStarted; 	//flag to start movement

	private int indexX;	//counter of screen position
	private int indexY;
	private boolean xIsMoving, yIsMoving; 		//flag for mouse movement.

	// private Vertex[][] inputGraph;
	private Vector<Vector<Vertex>> outputGraph;
	private final ArrayList<Vertex> vertexList;
	private ArrayList<Vertex> vertexTraversed;
	//Screen Label variables

	private final HashMap<String, String> registerEquivalenceTable;

	private String instructionCode;

	private final int countRegLabel;
	private final int countALULabel;
	private final int countPCLabel;

	private final int register = 1;
	private final int control = 2;
	private final int aluControl = 3;
	private final int alu = 4;
	private final int datapatTypeUsed;

	private final Boolean cursorInIM, cursorInALU, cursorInDataMem;

	private Boolean cursorInReg;

	private Graphics2D g2d;

	private BufferedImage datapath;

	class Vertex {

		private int numIndex;
		private int init;
		private int end;
		private int current;
		private String name;
		public static final int movingUpside = 1;
		public static final int movingDownside = 2;
		public static final int movingLeft = 3;
		public static final int movingRight = 4;
		public int direction;
		public int oppositeAxis;
		private boolean isMovingXaxis;
		private Color color;
		private boolean first_interaction;
		private boolean active;
		private final boolean isText;
		private final ArrayList<Integer> targetVertex;

		public Vertex(final int index, final int init, final int end, final String name, final int oppositeAxis,
				final boolean isMovingXaxis, final String listOfColors, final String listTargetVertex,
				final boolean isText) {
			numIndex = index;
			this.init = init;
			current = this.init;
			this.end = end;
			this.name = name;
			this.oppositeAxis = oppositeAxis;
			this.isMovingXaxis = isMovingXaxis;
			first_interaction = true;
			active = false;
			this.isText = isText;
			color = new Color(0, 153, 0);
			if (isMovingXaxis == true) {
				if (init < end) {
					direction = movingLeft;
				} else {
					direction = movingRight;
				}

			} else {
				if (init < end) {
					direction = movingUpside;
				} else {
					direction = movingDownside;
				}
			}
			final String[] list = listTargetVertex.split("#");
			targetVertex = new ArrayList<>();
			for (int i = 0; i < list.length; i++) {
				targetVertex.add(Integer.parseInt(list[i]));
				//	System.out.println("Adding " + i + " " +  Integer.parseInt(list[i])+ " in target");
			}
			final String[] listColor = listOfColors.split("#");
			color = new Color(Integer.parseInt(listColor[0]), Integer.parseInt(listColor[1]), Integer.parseInt(
					listColor[2]));
		}

		public int getDirection() { return direction; }

		public boolean isText() { return isText; }

		public ArrayList<Integer> getTargetVertex() { return targetVertex; }

		public int getNumIndex() { return numIndex; }

		public void setNumIndex(final int numIndex) { this.numIndex = numIndex; }

		public int getInit() { return init; }

		public void setInit(final int init) { this.init = init; }

		public int getEnd() { return end; }

		public void setEnd(final int end) { this.end = end; }

		public int getCurrent() { return current; }

		public void setCurrent(final int current) { this.current = current; }

		public String getName() { return name; }

		public void setName(final String name) { this.name = name; }

		public int getOppositeAxis() { return oppositeAxis; }

		public void setOppositeAxis(final int oppositeAxis) { this.oppositeAxis = oppositeAxis; }

		public boolean isMovingXaxis() { return isMovingXaxis; }

		public void setMovingXaxis(final boolean isMovingXaxis) { this.isMovingXaxis = isMovingXaxis; }

		public Color getColor() { return color; }

		public void setColor(final Color color) { this.color = color; }

		public boolean isFirst_interaction() { return first_interaction; }

		public void setFirst_interaction(final boolean first_interaction) {
			this.first_interaction = first_interaction;
		}

		public boolean isActive() { return active; }

		public void setActive(final boolean active) { this.active = active; }
	}

	public UnitAnimation(final String instructionBinary, final int datapathType) {
		datapatTypeUsed = datapathType;
		cursorInIM = false;
		cursorInALU = false;
		cursorInDataMem = false;
		df = new DecimalFormat("0.0");  // 1 dp
		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		gd = ge.getDefaultScreenDevice();
		gc = ge.getDefaultScreenDevice().getDefaultConfiguration();

		accelMemory = gd.getAvailableAcceleratedMemory();  // in bytes
		setBackground(Color.white);
		setPreferredSize(new Dimension(PWIDTH, PHEIGHT));

		// load and initialise the images
		initImages();

		vertexList = new ArrayList<>();
		counter = 0;
		justStarted = true;
		instructionCode = instructionBinary;

		//declaration of labels definition.
		registerEquivalenceTable = new HashMap<>();

		countRegLabel = 400;
		countALULabel = 380;
		countPCLabel = 380;
		loadHashMapValues();

	} // end of ImagesTests()

	//set the binnary opcode value of the basic instructions of MIPS instruction set
	public void loadHashMapValues() {
		if (datapatTypeUsed == register) {
			importXmlStringData("/registerDatapath.xml", registerEquivalenceTable, "register_equivalence", "bits",
					"mnemonic");
			importXmlDatapathMap("/registerDatapath.xml", "datapath_map");
		} else if (datapatTypeUsed == control) {
			importXmlStringData("/controlDatapath.xml", registerEquivalenceTable, "register_equivalence", "bits",
					"mnemonic");
			importXmlDatapathMap("/controlDatapath.xml", "datapath_map");
		}

		else if (datapatTypeUsed == aluControl) {
			importXmlStringData("/ALUcontrolDatapath.xml", registerEquivalenceTable, "register_equivalence", "bits",
					"mnemonic");
			importXmlDatapathMapAluControl("/ALUcontrolDatapath.xml", "datapath_map");
		}
	}

	//import the list of opcodes of mips set of instructions
	public void importXmlStringData(final String xmlName, final HashMap table, final String elementTree,
			final String tagId, final String tagData) {
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		DocumentBuilder docBuilder;
		try {
			//System.out.println();
			docBuilder = dbf.newDocumentBuilder();
			final Document doc = docBuilder.parse(getClass().getResource(xmlName).toString());
			final Element root = doc.getDocumentElement();
			Element equivalenceItem;
			NodeList bitsList, mnemonic;
			final NodeList equivalenceList = root.getElementsByTagName(elementTree);
			for (int i = 0; i < equivalenceList.getLength(); i++) {
				equivalenceItem = (Element) equivalenceList.item(i);
				bitsList = equivalenceItem.getElementsByTagName(tagId);
				mnemonic = equivalenceItem.getElementsByTagName(tagData);
				for (int j = 0; j < bitsList.getLength(); j++) {
					table.put(bitsList.item(j).getTextContent(), mnemonic.item(j).getTextContent());
				}
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
	}

	//import the parameters of the animation on datapath
	public void importXmlDatapathMap(final String xmlName, final String elementTree) {
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		DocumentBuilder docBuilder;
		try {
			docBuilder = dbf.newDocumentBuilder();
			final Document doc = docBuilder.parse(getClass().getResource(xmlName).toString());
			final Element root = doc.getDocumentElement();
			Element datapath_mapItem;
			NodeList index_vertex, name, init, end, color, other_axis, isMovingXaxis, targetVertex, isText;
			final NodeList datapath_mapList = root.getElementsByTagName(elementTree);
			for (int i = 0; i < datapath_mapList.getLength(); i++) { //extract the vertex of the xml input and encapsulate into the vertex object
				datapath_mapItem = (Element) datapath_mapList.item(i);
				index_vertex = datapath_mapItem.getElementsByTagName("num_vertex");
				name = datapath_mapItem.getElementsByTagName("name");
				init = datapath_mapItem.getElementsByTagName("init");
				end = datapath_mapItem.getElementsByTagName("end");
				//definition of colors line

				if (instructionCode.substring(0, 6).equals("000000")) {//R-type instructions
					color = datapath_mapItem.getElementsByTagName("color_Rtype");
					//System.out.println("rtype");
				} else if (instructionCode.substring(0, 6).matches("00001[0-1]")) { //J-type instructions
					color = datapath_mapItem.getElementsByTagName("color_Jtype");
					//System.out.println("jtype");
				} else if (instructionCode.substring(0, 6).matches("100[0-1][0-1][0-1]")) { //LOAD type instructions
					color = datapath_mapItem.getElementsByTagName("color_LOADtype");
					//System.out.println("load type");
				} else if (instructionCode.substring(0, 6).matches("101[0-1][0-1][0-1]")) { //LOAD type instructions
					color = datapath_mapItem.getElementsByTagName("color_STOREtype");
					//System.out.println("store type");
				} else if (instructionCode.substring(0, 6).matches("0001[0-1][0-1]")) { //BRANCH type instructions
					color = datapath_mapItem.getElementsByTagName("color_BRANCHtype");
					//System.out.println("branch type");
				} else { //BRANCH type instructions
					color = datapath_mapItem.getElementsByTagName("color_Itype");
					//System.out.println("immediate type");
				}

				other_axis = datapath_mapItem.getElementsByTagName("other_axis");
				isMovingXaxis = datapath_mapItem.getElementsByTagName("isMovingXaxis");
				targetVertex = datapath_mapItem.getElementsByTagName("target_vertex");
				isText = datapath_mapItem.getElementsByTagName("is_text");

				for (int j = 0; j < index_vertex.getLength(); j++) {
					final Vertex vert = new Vertex(Integer.parseInt(index_vertex.item(j).getTextContent()), Integer
							.parseInt(init.item(j).getTextContent()), Integer.parseInt(end.item(j).getTextContent()),
							name.item(j).getTextContent(), Integer.parseInt(other_axis.item(j).getTextContent()),
							Boolean.parseBoolean(isMovingXaxis.item(j).getTextContent()), color.item(j)
									.getTextContent(), targetVertex.item(j).getTextContent(), Boolean.parseBoolean(
											isText.item(j).getTextContent()));
					vertexList.add(vert);
				}
			}
			//loading matrix of control of vertex.
			outputGraph = new Vector<>();
			vertexTraversed = new ArrayList<>();
			vertexList.size();
			Vertex vertex;
			ArrayList<Integer> targetList;
			for (int i = 0; i < vertexList.size(); i++) {
				vertex = vertexList.get(i);
				targetList = vertex.getTargetVertex();
				final Vector<Vertex> vertexOfTargets = new Vector<>();
				for (int k = 0; k < targetList.size(); k++) {
					vertexOfTargets.add(vertexList.get(targetList.get(k)));
				}
				outputGraph.add(vertexOfTargets);
			}
			for (int i = 0; i < outputGraph.size(); i++) {
				outputGraph.get(i);
			}

			vertexList.get(0).setActive(true);
			vertexTraversed.add(vertexList.get(0));
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	public void importXmlDatapathMapAluControl(final String xmlName, final String elementTree) {
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setNamespaceAware(false);
		DocumentBuilder docBuilder;
		try {
			docBuilder = dbf.newDocumentBuilder();
			final Document doc = docBuilder.parse(getClass().getResource(xmlName).toString());
			final Element root = doc.getDocumentElement();
			Element datapath_mapItem;
			NodeList index_vertex, name, init, end, color, other_axis, isMovingXaxis, targetVertex, isText;
			final NodeList datapath_mapList = root.getElementsByTagName(elementTree);
			for (int i = 0; i < datapath_mapList.getLength(); i++) { //extract the vertex of the xml input and encapsulate into the vertex object
				datapath_mapItem = (Element) datapath_mapList.item(i);
				index_vertex = datapath_mapItem.getElementsByTagName("num_vertex");
				name = datapath_mapItem.getElementsByTagName("name");
				init = datapath_mapItem.getElementsByTagName("init");
				end = datapath_mapItem.getElementsByTagName("end");
				//definition of colors line

				if (instructionCode.substring(0, 6).equals("000000")) {//R-type instructions
					if (instructionCode.substring(28, 32).matches("0000")) { //BRANCH type instructions
						color = datapath_mapItem.getElementsByTagName("ALU_out010");
						System.out.println("ALU_out010 type " + instructionCode.substring(28, 32));
					} else if (instructionCode.substring(28, 32).matches("0010")) { //BRANCH type instructions
						color = datapath_mapItem.getElementsByTagName("ALU_out110");
						System.out.println("ALU_out110 type " + instructionCode.substring(28, 32));
					} else if (instructionCode.substring(28, 32).matches("0100")) { //BRANCH type instructions
						color = datapath_mapItem.getElementsByTagName("ALU_out000");
						System.out.println("ALU_out000 type " + instructionCode.substring(28, 32));
					} else if (instructionCode.substring(28, 32).matches("0101")) { //BRANCH type instructions
						color = datapath_mapItem.getElementsByTagName("ALU_out001");
						System.out.println("ALU_out001 type " + instructionCode.substring(28, 32));
					} else { //BRANCH type instructions
						color = datapath_mapItem.getElementsByTagName("ALU_out111");
						System.out.println("ALU_out111 type " + instructionCode.substring(28, 32));
					}
				} else if (instructionCode.substring(0, 6).matches("00001[0-1]")) { //J-type instructions
					color = datapath_mapItem.getElementsByTagName("color_Jtype");
					System.out.println("jtype");
				} else if (instructionCode.substring(0, 6).matches("100[0-1][0-1][0-1]")) { //LOAD type instructions
					color = datapath_mapItem.getElementsByTagName("color_LOADtype");
					System.out.println("load type");
				} else if (instructionCode.substring(0, 6).matches("101[0-1][0-1][0-1]")) { //LOAD type instructions
					color = datapath_mapItem.getElementsByTagName("color_STOREtype");
					System.out.println("store type");
				} else if (instructionCode.substring(0, 6).matches("0001[0-1][0-1]")) { //BRANCH type instructions
					color = datapath_mapItem.getElementsByTagName("color_BRANCHtype");
					System.out.println("branch type");
				} else {
					color = datapath_mapItem.getElementsByTagName("color_Itype");
					System.out.println("immediate type");
				}

				other_axis = datapath_mapItem.getElementsByTagName("other_axis");
				isMovingXaxis = datapath_mapItem.getElementsByTagName("isMovingXaxis");
				targetVertex = datapath_mapItem.getElementsByTagName("target_vertex");
				isText = datapath_mapItem.getElementsByTagName("is_text");

				for (int j = 0; j < index_vertex.getLength(); j++) {
					final Vertex vert = new Vertex(Integer.parseInt(index_vertex.item(j).getTextContent()), Integer
							.parseInt(init.item(j).getTextContent()), Integer.parseInt(end.item(j).getTextContent()),
							name.item(j).getTextContent(), Integer.parseInt(other_axis.item(j).getTextContent()),
							Boolean.parseBoolean(isMovingXaxis.item(j).getTextContent()), color.item(j)
									.getTextContent(), targetVertex.item(j).getTextContent(), Boolean.parseBoolean(
											isText.item(j).getTextContent()));
					vertexList.add(vert);
				}
			}
			//loading matrix of control of vertex.
			outputGraph = new Vector<>();
			vertexTraversed = new ArrayList<>();
			vertexList.size();
			Vertex vertex;
			ArrayList<Integer> targetList;
			for (int i = 0; i < vertexList.size(); i++) {
				vertex = vertexList.get(i);
				targetList = vertex.getTargetVertex();
				final Vector<Vertex> vertexOfTargets = new Vector<>();
				for (int k = 0; k < targetList.size(); k++) {
					vertexOfTargets.add(vertexList.get(targetList.get(k)));
				}
				outputGraph.add(vertexOfTargets);
			}
			for (int i = 0; i < outputGraph.size(); i++) {
				outputGraph.get(i);
			}

			vertexList.get(0).setActive(true);
			vertexTraversed.add(vertexList.get(0));
		} catch (final Exception e) {
			e.printStackTrace();
		}

	}

	//set the initial state of the variables that controls the animation, and start the timer that triggers the animation.
	public void startAnimation(final String codeInstruction) {
		instructionCode = codeInstruction;
		new Timer(PERIOD, this).start();    // start timer
		this.repaint();
	}

	//initialize the image of datapath.
	private void initImages() {
		try {
			BufferedImage im;
			if (datapatTypeUsed == register) {
				im = ImageIO.read(getClass().getResource(Globals.imagesPath + "register.png"));
			} else if (datapatTypeUsed == control) {
				im = ImageIO.read(getClass().getResource(Globals.imagesPath + "control.png"));
			} else if (datapatTypeUsed == aluControl) {
				im = ImageIO.read(getClass().getResource(Globals.imagesPath + "ALUcontrol.png"));
			} else {
				im = ImageIO.read(getClass().getResource(Globals.imagesPath + "alu.png"));
			}

			final int transparency = im.getColorModel().getTransparency();
			datapath = gc.createCompatibleImage(im.getWidth(), im.getHeight(), transparency);
			g2d = datapath.createGraphics();
			g2d.drawImage(im, 0, 0, null);
			g2d.dispose();
		} catch (final IOException e) {
			System.out.println("Load Image error for " + getClass().getResource(Globals.imagesPath + "register.png")
					+ ":\n" + e);
		}
	}

	public void updateDisplay() {
		this.repaint();
	}

	@Override
	public void actionPerformed(final ActionEvent e)
	// triggered by the timer: update, repaint
	{
		if (justStarted) { justStarted = false; }
		if (xIsMoving) { indexX++; }
		if (yIsMoving) { indexY--; }
		repaint();
	}

	@Override
	public void paintComponent(final Graphics g) {
		super.paintComponent(g);
		g2d = (Graphics2D) g;
		// use antialiasing
		g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
		// smoother (and slower) image transformations  (e.g. for resizing)
		g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		g2d = (Graphics2D) g;
		drawImage(g2d, datapath, 0, 0, null);
		executeAnimation(g);
		counter = (counter + 1) % 100;
		g2d.dispose();

	}

	private void drawImage(final Graphics2D g2d, final BufferedImage im, final int x, final int y, final Color c) {
		if (im == null) {
			g2d.setColor(c);
			g2d.fillOval(x, y, 20, 20);
			g2d.setColor(Color.black);
			g2d.drawString("   ", x, y);
		} else {
			g2d.drawImage(im, x, y, this);
		}
	}

	//draw lines.
	//method to draw the lines that run from left to right.
	public void printTrackLtoR(final Vertex v) {
		int size;
		int[] track;
		size = v.getEnd() - v.getInit();
		track = new int[size];
		for (int i = 0; i < size; i++) {
			track[i] = v.getInit() + i;
		}
		if (v.isActive() == true) {
			v.setFirst_interaction(false);
			for (int i = 0; i < size; i++) {
				if (track[i] <= v.getCurrent()) {
					g2d.setColor(v.getColor());
					g2d.fillRect(track[i], v.getOppositeAxis(), 3, 3);
				}
			}
			if (v.getCurrent() == track[size - 1]) { v.setActive(false); }
			v.setCurrent(v.getCurrent() + 1);
		} else if (v.isFirst_interaction() == false) {
			for (int i = 0; i < size; i++) {
				g2d.setColor(v.getColor());
				g2d.fillRect(track[i], v.getOppositeAxis(), 3, 3);
			}
		}

	}

	//method to draw the lines that run from right to left.
	//public boolean printTrackRtoL(int init, int end ,int currentIndex, Graphics2D g2d, Color color, int otherAxis,
	//		 boolean active, boolean firstInteraction){
	public void printTrackRtoL(final Vertex v) {
		int size;
		int[] track;
		size = v.getInit() - v.getEnd();
		track = new int[size];

		for (int i = 0; i < size; i++) {
			track[i] = v.getInit() - i;
		}

		if (v.isActive() == true) {
			v.setFirst_interaction(false);
			for (int i = 0; i < size; i++) {
				if (track[i] >= v.getCurrent()) {
					g2d.setColor(v.getColor());
					g2d.fillRect(track[i], v.getOppositeAxis(), 3, 3);
				}
			}
			if (v.getCurrent() == track[size - 1]) { v.setActive(false); }

			v.setCurrent(v.getCurrent() - 1);
		} else if (v.isFirst_interaction() == false) {
			for (int i = 0; i < size; i++) {
				g2d.setColor(v.getColor());
				g2d.fillRect(track[i], v.getOppositeAxis(), 3, 3);
			}
		}
	}

	//method to draw the lines that run from down to top.
	// public boolean printTrackDtoU(int init, int end ,int currentIndex, Graphics2D g2d, Color color, int otherAxis,
	//		 boolean active, boolean firstInteraction){
	public void printTrackDtoU(final Vertex v) {
		int size;
		int[] track;

		if (v.getInit() > v.getEnd()) {
			size = v.getInit() - v.getEnd();
			track = new int[size];
			for (int i = 0; i < size; i++) {
				track[i] = v.getInit() - i;
			}
		} else {
			size = v.getEnd() - v.getInit();
			track = new int[size];
			for (int i = 0; i < size; i++) {
				track[i] = v.getInit() + i;
			}
		}

		if (v.isActive() == true) {
			v.setFirst_interaction(false);
			for (int i = 0; i < size; i++) {
				if (track[i] >= v.getCurrent()) {
					g2d.setColor(v.getColor());
					g2d.fillRect(v.getOppositeAxis(), track[i], 3, 3);
				}
			}
			if (v.getCurrent() == track[size - 1]) { v.setActive(false); }
			v.setCurrent(v.getCurrent() - 1);

		} else if (v.isFirst_interaction() == false) {
			for (int i = 0; i < size; i++) {
				g2d.setColor(v.getColor());
				g2d.fillRect(v.getOppositeAxis(), track[i], 3, 3);
			}
		}
	}

	//method to draw the lines that run from top to down.
	// public boolean printTrackUtoD(int init, int end ,int currentIndex, Graphics2D g2d, Color color, int otherAxis,
	//		 boolean active,  boolean firstInteraction){
	public void printTrackUtoD(final Vertex v) {

		int size;
		int[] track;
		size = v.getEnd() - v.getInit();
		track = new int[size];

		for (int i = 0; i < size; i++) {
			track[i] = v.getInit() + i;
		}

		if (v.isActive() == true) {
			v.setFirst_interaction(false);
			for (int i = 0; i < size; i++) {
				if (track[i] <= v.getCurrent()) {
					g2d.setColor(v.getColor());
					g2d.fillRect(v.getOppositeAxis(), track[i], 3, 3);
				}

			}
			if (v.getCurrent() == track[size - 1]) { v.setActive(false); }
			v.setCurrent(v.getCurrent() + 1);
		} else if (v.isFirst_interaction() == false) {
			for (int i = 0; i < size; i++) {
				g2d.setColor(v.getColor());
				g2d.fillRect(v.getOppositeAxis(), track[i], 3, 3);
			}
		}
	}

	//convert binnary value to integer.
	public String parseBinToInt(final String code) {
		int value = 0;

		for (int i = code.length() - 1; i >= 0; i--) {
			if ("1".equals(code.substring(i, i + 1))) { value = value + (int) Math.pow(2, code.length() - i - 1); }
		}

		return Integer.toString(value);
	}

	//set and execute the information about the current position of each line of information in the animation,
	//verifies the previous status of the animation and increment the position of each line that interconnect the unit function.
	private void executeAnimation(final Graphics g) {
		g2d = (Graphics2D) g;
		Vertex vert;
		for (int i = 0; i < vertexTraversed.size(); i++) {
			vert = vertexTraversed.get(i);
			if (vert.isMovingXaxis == true) {
				if (vert.getDirection() == Vertex.movingLeft) {
					printTrackLtoR(vert);
					if (vert.isActive() == false) {
						final int j = vert.getTargetVertex().size();
						Vertex tempVertex;
						for (int k = 0; k < j; k++) {
							tempVertex = outputGraph.get(vert.getNumIndex()).get(k);
							Boolean hasThisVertex = false;
							for (int m = 0; m < vertexTraversed.size(); m++) {
								if (tempVertex.getNumIndex() == vertexTraversed.get(m).getNumIndex()) {
									hasThisVertex = true;
								}
							}
							if (hasThisVertex == false) {
								outputGraph.get(vert.getNumIndex()).get(k).setActive(true);
								vertexTraversed.add(outputGraph.get(vert.getNumIndex()).get(k));
							}
						}
					}
				} else {
					printTrackRtoL(vert);
					if (vert.isActive() == false) {
						final int j = vert.getTargetVertex().size();
						Vertex tempVertex;
						for (int k = 0; k < j; k++) {
							tempVertex = outputGraph.get(vert.getNumIndex()).get(k);
							Boolean hasThisVertex = false;
							for (int m = 0; m < vertexTraversed.size(); m++) {
								if (tempVertex.getNumIndex() == vertexTraversed.get(m).getNumIndex()) {
									hasThisVertex = true;
								}
							}
							if (hasThisVertex == false) {
								outputGraph.get(vert.getNumIndex()).get(k).setActive(true);
								vertexTraversed.add(outputGraph.get(vert.getNumIndex()).get(k));
							}
						}
					}
				}
			} //end of condition of X axis
			else {
				if (vert.getDirection() == Vertex.movingDownside) {
					if (vert.isText == true) {
						;
					} else {
						printTrackDtoU(vert);
					}

					if (vert.isActive() == false) {
						final int j = vert.getTargetVertex().size();
						Vertex tempVertex;
						for (int k = 0; k < j; k++) {
							tempVertex = outputGraph.get(vert.getNumIndex()).get(k);
							Boolean hasThisVertex = false;
							for (int m = 0; m < vertexTraversed.size(); m++) {
								if (tempVertex.getNumIndex() == vertexTraversed.get(m).getNumIndex()) {
									hasThisVertex = true;
								}
							}
							if (hasThisVertex == false) {
								outputGraph.get(vert.getNumIndex()).get(k).setActive(true);
								vertexTraversed.add(outputGraph.get(vert.getNumIndex()).get(k));
							}
						}
					}

				} else {
					printTrackUtoD(vert);
					if (vert.isActive() == false) {
						final int j = vert.getTargetVertex().size();
						Vertex tempVertex;
						for (int k = 0; k < j; k++) {
							tempVertex = outputGraph.get(vert.getNumIndex()).get(k);
							Boolean hasThisVertex = false;
							for (int m = 0; m < vertexTraversed.size(); m++) {
								if (tempVertex.getNumIndex() == vertexTraversed.get(m).getNumIndex()) {
									hasThisVertex = true;
								}
							}
							if (hasThisVertex == false) {
								outputGraph.get(vert.getNumIndex()).get(k).setActive(true);
								vertexTraversed.add(outputGraph.get(vert.getNumIndex()).get(k));
							}
						}
					}
				}
			}
		}
	}

}
