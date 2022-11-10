package mars.tools;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.MouseInfo;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.font.FontRenderContext;
import java.awt.font.TextLayout;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Observable;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.Timer;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import mars.Globals;
import mars.ProgramStatement;
import mars.mips.hardware.AccessNotice;
import mars.mips.hardware.AddressErrorException;
import mars.mips.hardware.Memory;
import mars.mips.hardware.MemoryAccessNotice;
import mars.mips.instructions.BasicInstruction;
import mars.venus.RunAssembleAction;
import mars.venus.RunBackstepAction;
import mars.venus.RunStepAction;
import mars.venus.VenusUI;

public class MipsXray extends AbstractMarsToolAndApplication {

	private static final long serialVersionUID = -1L;
	private static String heading = "MIPS X-Ray - Animation of MIPS Datapath";
	private static String version = " Version 2.0";

	protected Graphics g;
	protected int lastAddress = -1; //address of instruction in memory
	protected JLabel label;
	private final Container painel = getContentPane();
	private DatapathAnimation datapathAnimation;   //class panel that runs datapath animation.

	private GraphicsConfiguration gc;
	private BufferedImage datapath;
	private String instructionBinary;

	//Components to add menu bar in the plugin window.
	private JButton Assemble, Step, runBackStep;
	private Action runAssembleAction, runStepAction, runBackstepAction;

	private VenusUI mainUI;
	private JToolBar toolbar;
	private Timer time;

	public MipsXray(final String title, final String heading) {
		super(title, heading);
	}

	/**
	 * Simple constructor, likely used by the MipsXray menu mechanism
	 */
	public MipsXray() {
		super(heading + ", " + version, heading);
	}

	/**
	 * Required method to return Tool name.
	 *
	 * @return Tool name. MARS will display this in menu item.
	 */
	@Override
	public String getName() { return "MIPS X-Ray"; }

	/**
	 * Overrides default method, to provide a Help button for this tool/app.
	 */
	@Override
	protected JComponent getHelpComponent() {
		final String helpContent = "This plugin is used to visualizate the behavior of mips processor using the default datapath. \n"
				+ "It reads the source code instruction and generates an animation representing the inputs and \n"
				+ "outputs of functional blocks and the interconnection between them.  The basic signals \n"
				+ "represented are, control signals, opcode bits and data of functional blocks.\n" + "\n"
				+ "Besides the datapath representation, information for each instruction is displayed below\n"
				+ "the datapath. That display includes opcode value, with the correspondent colors used to\n"
				+ "represent the signals in datapath, mnemonic of the instruction processed at the moment, registers\n"
				+ "used in the instruction and a label that indicates the color code used to represent control signals\n"
				+ "\n" + "To see the datapath of register bank and control units click inside the functional unit.\n\n"
				+ "Version 2.0\n"
				+ "Developed by M�rcio Roberto, Guilherme Sales, Fabr�cio Vivas, Fl�vio Cardeal and F�bio L�cio\n"
				+ "Contact Marcio Roberto at marcio.rdaraujo@gmail.com with questions or comments.\n";
		final JButton help = new JButton("Help");
		help.addActionListener(e -> JOptionPane.showMessageDialog(theWindow, helpContent));
		return help;
	}

	/**
	 * Implementation of the inherited abstract method to build the main display
	 * area of the GUI. It will be placed in the CENTER area of a BorderLayout. The
	 * title is in the NORTH area, and the controls are in the SOUTH area.
	 */
	protected JComponent buildAnimationSequence() {
		final JPanel image = new JPanel(new GridBagLayout());
		return image;
	}

	// Insert image in the panel and configure the parameters to run animation.
	@Override
	protected JComponent buildMainDisplayArea() {
		mainUI = Globals.getGui();
		createActionObjects();
		toolbar = setUpToolBar();

		//   	   JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
		try {
			final BufferedImage im = ImageIO.read(getClass().getResource(Globals.imagesPath + "datapath.png"));

			final int transparency = im.getColorModel().getTransparency();
			datapath = gc.createCompatibleImage(im.getWidth(), im.getHeight(), transparency);

			final Graphics2D g2d = datapath.createGraphics();  // graphics context
			g2d.drawImage(im, 0, 0, null);
			g2d.dispose();

		} catch (final IOException e) {
			System.out.println("Load Image error for " + getClass().getResource(Globals.imagesPath + "datapath.png")
					+ ":\n" + e);
			e.printStackTrace();
		}
		System.setProperty("sun.java2d.translaccel", "true");
		ImageIcon icon = new ImageIcon(getClass().getResource(Globals.imagesPath + "datapath.png"));
		final Image im = icon.getImage();
		icon = new ImageIcon(im);

		final JLabel label = new JLabel(icon);
		painel.add(label, BorderLayout.WEST);
		painel.add(toolbar, BorderLayout.NORTH);
		setResizable(false);
		return (JComponent) painel;
	}

	protected JComponent buildMainDisplayArea(final String figure) {
		mainUI = Globals.getGui();
		createActionObjects();
		toolbar = setUpToolBar();

		//      	   JPanel jp = new JPanel(new FlowLayout(FlowLayout.LEFT));
		final GraphicsEnvironment ge = GraphicsEnvironment.getLocalGraphicsEnvironment();
		gc = ge.getDefaultScreenDevice().getDefaultConfiguration();
		try {
			final BufferedImage im = ImageIO.read(getClass().getResource(Globals.imagesPath + figure));

			final int transparency = im.getColorModel().getTransparency();
			datapath = gc.createCompatibleImage(im.getWidth(), im.getHeight(), transparency);

			final Graphics2D g2d = datapath.createGraphics();  // graphics context
			g2d.drawImage(im, 0, 0, null);
			g2d.dispose();

		} catch (final IOException e) {
			System.out.println("Load Image error for " + getClass().getResource(Globals.imagesPath + figure) + ":\n"
					+ e);
			e.printStackTrace();
		}
		System.setProperty("sun.java2d.translaccel", "true");
		ImageIcon icon = new ImageIcon(getClass().getResource(Globals.imagesPath + figure));
		final Image im = icon.getImage();
		icon = new ImageIcon(im);

		final JLabel label = new JLabel(icon);
		painel.add(label, BorderLayout.WEST);
		painel.add(toolbar, BorderLayout.NORTH);
		setResizable(false);
		return (JComponent) painel;
	}

	@Override
	protected void addAsObserver() {
		addAsObserver(Memory.textBaseAddress, Memory.textLimitAddress);
	}

	//Function that gets the current instruction in memory and start animation with the selected instruction.
	@Override
	protected void processMIPSUpdate(final Observable resource, final AccessNotice notice) {

		if (!notice.accessIsFromMIPS()) { return; }
		if (notice.getAccessType() != AccessNotice.READ) { return; }
		final MemoryAccessNotice man = (MemoryAccessNotice) notice;
		final int currentAdress = man.getAddress();

		if (currentAdress == lastAddress) { return; }
		lastAddress = currentAdress;
		ProgramStatement stmt;

		try {
			BasicInstruction instr = null;
			stmt = Memory.getInstance().getStatement(currentAdress);
			if (stmt == null) { return; }

			instr = (BasicInstruction) stmt.getInstruction();
			instructionBinary = stmt.getMachineStatement();
			instr.getInstructionFormat();

			painel.removeAll();
			datapathAnimation = new DatapathAnimation(instructionBinary);
			createActionObjects();
			toolbar = setUpToolBar();
			painel.add(toolbar, BorderLayout.NORTH);
			painel.add(datapathAnimation, BorderLayout.WEST);
			datapathAnimation.startAnimation(instructionBinary);

		} catch (final AddressErrorException e) {
			e.printStackTrace();
		}

	}

	@Override
	public void updateDisplay() {
		this.repaint();
	}

	//set the tool bar that controls the step in a time instruction running.
	private JToolBar setUpToolBar() {
		final JToolBar toolBar = new JToolBar();
		Assemble = new JButton(runAssembleAction);
		Assemble.setText("");
		runBackStep = new JButton(runBackstepAction);
		runBackStep.setText("");

		Step = new JButton(runStepAction);
		Step.setText("");
		toolBar.add(Assemble);
		toolBar.add(Step);

		return toolBar;
	}

	//set action in the menu bar.
	private void createActionObjects() {
		final Toolkit tk = Toolkit.getDefaultToolkit();
		final Class cs = this.getClass();
		try {
			runAssembleAction = new RunAssembleAction("Assemble", new ImageIcon(tk.getImage(cs.getResource(
					Globals.imagesPath + "Assemble22.png"))), "Assemble the current file and clear breakpoints",
					new Integer(KeyEvent.VK_A), KeyStroke.getKeyStroke(KeyEvent.VK_F3, 0), mainUI);

			runStepAction = new RunStepAction("Step", new ImageIcon(tk.getImage(cs.getResource(Globals.imagesPath
					+ "StepForward22.png"))), "Run one step at a time", new Integer(KeyEvent.VK_T), KeyStroke
							.getKeyStroke(KeyEvent.VK_F7, 0), mainUI);
			runBackstepAction = new RunBackstepAction("Backstep", new ImageIcon(tk.getImage(cs.getResource(
					Globals.imagesPath + "StepBack22.png"))), "Undo the last step", new Integer(KeyEvent.VK_B),
					KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), mainUI);
		} catch (final Exception e) {
			System.out.println(
					"Internal Error: images folder not found, or other null pointer exception while creating Action objects");
			e.printStackTrace();
			System.exit(0);
		}
	}

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

	//Internal class that set the parameters value, control the basic behavior of the animation , and execute the animation of the
	//selected instruction in memory.
	class DatapathAnimation extends JPanel implements ActionListener, MouseListener {

		/**
		 *
		 */
		private static final long serialVersionUID = -2681757800180958534L;

		//config variables
		private final int PERIOD = 5;    // velocity of frames in ms
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

		//	 private Vertex[][] inputGraph;
		private Vector<Vector<Vertex>> outputGraph;
		private final ArrayList<Vertex> vertexList;
		private ArrayList<Vertex> vertexTraversed;
		//Screen Label variables

		private final HashMap<String, String> opcodeEquivalenceTable;
		private final HashMap<String, String> functionEquivalenceTable;
		private final HashMap<String, String> registerEquivalenceTable;

		private String instructionCode;

		private final int countRegLabel;
		private final int countALULabel;
		private final int countPCLabel;

		//Colors variables
		private final Color green1 = new Color(0, 153, 0);
		private final Color green2 = new Color(0, 77, 0);
		private final Color yellow2 = new Color(185, 182, 42);
		private final Color orange1 = new Color(255, 102, 0);
		private final Color orange = new Color(119, 34, 34);
		private final Color blue2 = new Color(0, 153, 255);

		private final int register = 1;
		private final int control = 2;
		private final int aluControl = 3;
		private final int alu = 4;
		private int currentUnit;
		private Graphics2D g2d;

		private BufferedImage datapath;

		@Override
		public void mousePressed(final MouseEvent e) {
			MouseInfo.getPointerInfo();
		}

		public DatapathAnimation(final String instructionBinary) {
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
			opcodeEquivalenceTable = new HashMap<>();
			functionEquivalenceTable = new HashMap<>();
			registerEquivalenceTable = new HashMap<>();

			countRegLabel = 400;
			countALULabel = 380;
			countPCLabel = 380;
			loadHashMapValues();
			addMouseListener(this);

		} // end of ImagesTests()

		//set the binnary opcode value of the basic instructions of MIPS instruction set
		public void loadHashMapValues() {
			importXmlStringData("/MipsXRayOpcode.xml", opcodeEquivalenceTable, "equivalence", "bits", "mnemonic");
			importXmlStringData("/MipsXRayOpcode.xml", functionEquivalenceTable, "function_equivalence", "bits",
					"mnemonic");
			importXmlStringData("/MipsXRayOpcode.xml", registerEquivalenceTable, "register_equivalence", "bits",
					"mnemonic");
			importXmlDatapathMap("/MipsXRayOpcode.xml", "datapath_map");
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
				NodeList index_vertex, name, init, end, color, other_axis, isMovingXaxis, targetVertex;
				NodeList isText;
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
								.parseInt(init.item(j).getTextContent()), Integer.parseInt(end.item(j)
										.getTextContent()), name.item(j).getTextContent(), Integer.parseInt(other_axis
												.item(j).getTextContent()), Boolean.parseBoolean(isMovingXaxis.item(j)
														.getTextContent()), color.item(j).getTextContent(), targetVertex
																.item(j).getTextContent(), Boolean.parseBoolean(isText
																		.item(j).getTextContent()));
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

		//Set up the information showed in the screen of the current instruction.
		public void setUpInstructionInfo(final Graphics2D g2d) {

			final FontRenderContext frc = g2d.getFontRenderContext();
			final Font font = new Font("Digital-7", Font.PLAIN, 15);
			final Font fontTitle = new Font("Verdana", Font.PLAIN, 10);

			TextLayout textVariable;
			if (instructionCode.substring(0, 6).equals("000000")) {  //R-type instructions description on screen definition.		 
				textVariable = new TextLayout("REGISTER TYPE INSTRUCTION", new Font("Arial", Font.BOLD, 25), frc);
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 280, 30);
				//opcode label
				textVariable = new TextLayout("opcode", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 25, 530);

				//initialize of opcode
				textVariable = new TextLayout(instructionCode.substring(0, 6), font, frc);
				g2d.setColor(Color.magenta);
				textVariable.draw(g2d, 25, 550);

				//rs label
				textVariable = new TextLayout("rs", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 90, 530);

				//initialize of rs
				textVariable = new TextLayout(instructionCode.substring(6, 11), font, frc);
				g2d.setColor(Color.green);
				textVariable.draw(g2d, 90, 550);

				//rt label
				textVariable = new TextLayout("rt", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 150, 530);

				//initialize of rt
				textVariable = new TextLayout(instructionCode.substring(11, 16), font, frc);
				g2d.setColor(Color.blue);
				textVariable.draw(g2d, 150, 550);

				// rd label
				textVariable = new TextLayout("rd", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 210, 530);

				//initialize of rd
				textVariable = new TextLayout(instructionCode.substring(16, 21), font, frc);
				g2d.setColor(Color.cyan);
				textVariable.draw(g2d, 210, 550);

				//shamt label
				textVariable = new TextLayout("shamt", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 270, 530);

				//initialize of shamt
				textVariable = new TextLayout(instructionCode.substring(21, 26), font, frc);
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 270, 550);

				//function label
				textVariable = new TextLayout("function", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 330, 530);

				//initialize of function
				textVariable = new TextLayout(instructionCode.substring(26, 32), font, frc);
				g2d.setColor(orange1);
				textVariable.draw(g2d, 330, 550);

				//instruction mnemonic
				textVariable = new TextLayout("Instruction", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 25, 480);

				//instruction name
				textVariable = new TextLayout(functionEquivalenceTable.get(instructionCode.substring(26, 32)), font,
						frc);
				g2d.setColor(Color.BLACK);
				textVariable.draw(g2d, 25, 500);

				//register in RS
				textVariable = new TextLayout(registerEquivalenceTable.get(instructionCode.substring(6, 11)), font,
						frc);
				g2d.setColor(Color.BLACK);
				textVariable.draw(g2d, 65, 500);

				//register in RT
				textVariable = new TextLayout(registerEquivalenceTable.get(instructionCode.substring(16, 21)), font,
						frc);
				g2d.setColor(Color.BLACK);
				textVariable.draw(g2d, 105, 500);

				//register in RD
				textVariable = new TextLayout(registerEquivalenceTable.get(instructionCode.substring(11, 16)), font,
						frc);
				g2d.setColor(Color.BLACK);
				textVariable.draw(g2d, 145, 500);
			}

			else if (instructionCode.substring(0, 6).matches("00001[0-1]")) { //jump intructions
				textVariable = new TextLayout("JUMP TYPE INSTRUCTION", new Font("Verdana", Font.BOLD, 25), frc); //description of instruction code type for jump.
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 280, 30);

				// label opcode
				textVariable = new TextLayout("opcode", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 25, 530);

				//initialize of opcode
				textVariable = new TextLayout(instructionCode.substring(0, 6), font, frc);
				g2d.setColor(Color.magenta);
				textVariable.draw(g2d, 25, 550);

				//label address
				textVariable = new TextLayout("address", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 95, 530);

				textVariable = new TextLayout("Instruction", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 25, 480);

				//initialize of adress
				textVariable = new TextLayout(instructionCode.substring(6, 32), font, frc);
				g2d.setColor(Color.orange);
				textVariable.draw(g2d, 95, 550);

				//instruction mnemonic
				textVariable = new TextLayout(opcodeEquivalenceTable.get(instructionCode.substring(0, 6)), font, frc);
				g2d.setColor(Color.cyan);
				textVariable.draw(g2d, 65, 500);

				//instruction immediate
				textVariable = new TextLayout("LABEL", font, frc);
				g2d.setColor(Color.cyan);
				textVariable.draw(g2d, 105, 500);
			}

			else if (instructionCode.substring(0, 6).matches("100[0-1][0-1][0-1]")) {//load instruction
				textVariable = new TextLayout("LOAD TYPE INSTRUCTION", new Font("Verdana", Font.BOLD, 25), frc); //description of instruction code type for load.
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 280, 30);
				//opcode label
				textVariable = new TextLayout("opcode", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 25, 530);

				//initialize of opcode
				textVariable = new TextLayout(instructionCode.substring(0, 6), font, frc);
				g2d.setColor(Color.magenta);
				textVariable.draw(g2d, 25, 550);

				//rs label
				textVariable = new TextLayout("rs", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 90, 530);

				//initialize of rs
				textVariable = new TextLayout(instructionCode.substring(6, 11), font, frc);
				g2d.setColor(Color.green);
				textVariable.draw(g2d, 90, 550);

				//rt label
				textVariable = new TextLayout("rt", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 145, 530);

				//initialize of rt
				textVariable = new TextLayout(instructionCode.substring(11, 16), font, frc);
				g2d.setColor(Color.blue);
				textVariable.draw(g2d, 145, 550);

				// rd label
				textVariable = new TextLayout("Immediate", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 200, 530);

				//initialize of rd
				textVariable = new TextLayout(instructionCode.substring(16, 32), font, frc);
				g2d.setColor(orange1);
				textVariable.draw(g2d, 200, 550);

				//instruction mnemonic
				textVariable = new TextLayout("Instruction", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 25, 480);

				textVariable = new TextLayout(opcodeEquivalenceTable.get(instructionCode.substring(0, 6)), font, frc);
				g2d.setColor(Color.BLACK);
				textVariable.draw(g2d, 25, 500);

				textVariable = new TextLayout(registerEquivalenceTable.get(instructionCode.substring(6, 11)), font,
						frc);
				g2d.setColor(Color.BLACK);
				textVariable.draw(g2d, 65, 500);

				textVariable = new TextLayout("M[ " + registerEquivalenceTable.get(instructionCode.substring(16, 21))
						+ " + " + parseBinToInt(instructionCode.substring(6, 32)) + " ]", font, frc);
				g2d.setColor(Color.BLACK);
				textVariable.draw(g2d, 105, 500);

				//implement co-processors instruction
			}

			else if (instructionCode.substring(0, 6).matches("101[0-1][0-1][0-1]")) {//store instruction
				textVariable = new TextLayout("STORE TYPE INSTRUCTION", new Font("Verdana", Font.BOLD, 25), frc);
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 280, 30);
				//opcode label
				textVariable = new TextLayout("opcode", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 25, 530);

				//initialize of opcode
				textVariable = new TextLayout(instructionCode.substring(0, 6), font, frc);
				g2d.setColor(Color.magenta);
				textVariable.draw(g2d, 25, 550);

				//rs label
				textVariable = new TextLayout("rs", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 90, 530);

				//initialize of rs
				textVariable = new TextLayout(instructionCode.substring(6, 11), font, frc);
				g2d.setColor(Color.green);
				textVariable.draw(g2d, 90, 550);

				//rt label
				textVariable = new TextLayout("rt", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 145, 530);

				//initialize of rt
				textVariable = new TextLayout(instructionCode.substring(11, 16), font, frc);
				g2d.setColor(Color.blue);
				textVariable.draw(g2d, 145, 550);

				// rd label
				textVariable = new TextLayout("Immediate", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 200, 530);

				//initialize of rd
				textVariable = new TextLayout(instructionCode.substring(16, 32), font, frc);
				g2d.setColor(orange1);
				textVariable.draw(g2d, 200, 550);

				//instruction mnemonic
				textVariable = new TextLayout("Instruction", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 25, 480);

				textVariable = new TextLayout(opcodeEquivalenceTable.get(instructionCode.substring(0, 6)), font, frc);
				g2d.setColor(Color.BLACK);
				textVariable.draw(g2d, 25, 500);

				textVariable = new TextLayout(registerEquivalenceTable.get(instructionCode.substring(6, 11)), font,
						frc);
				g2d.setColor(Color.BLACK);
				textVariable.draw(g2d, 65, 500);

				textVariable = new TextLayout("M[ " + registerEquivalenceTable.get(instructionCode.substring(16, 21))
						+ " + " + parseBinToInt(instructionCode.substring(6, 32)) + " ]", font, frc);
				g2d.setColor(Color.BLACK);
				textVariable.draw(g2d, 105, 500);

			}

			else if (instructionCode.substring(0, 6).matches("0100[0-1][0-1]")) {
				//implement co-processors instruction
			}

			else if (instructionCode.substring(0, 6).matches("0001[0-1][0-1]")) { //branch instruction
				textVariable = new TextLayout("BRANCH TYPE INSTRUCTION", new Font("Verdana", Font.BOLD, 25), frc);
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 250, 30);

				//label opcode
				textVariable = new TextLayout("opcode", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 25, 440);

				textVariable = new TextLayout("opcode", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 25, 530);

				//initialize of opcode
				textVariable = new TextLayout(instructionCode.substring(0, 6), font, frc);
				g2d.setColor(Color.magenta);
				textVariable.draw(g2d, 25, 550);

				//rs label
				textVariable = new TextLayout("rs", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 90, 530);

				//initialize of rs
				textVariable = new TextLayout(instructionCode.substring(6, 11), font, frc);
				g2d.setColor(Color.green);
				textVariable.draw(g2d, 90, 550);

				//rt label
				textVariable = new TextLayout("rt", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 145, 530);

				//initialize of rt
				textVariable = new TextLayout(instructionCode.substring(11, 16), font, frc);
				g2d.setColor(Color.blue);
				textVariable.draw(g2d, 145, 550);

				// rd label
				textVariable = new TextLayout("Immediate", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 200, 530);

				//initialize of immediate
				textVariable = new TextLayout(instructionCode.substring(16, 32), font, frc);
				g2d.setColor(Color.cyan);
				textVariable.draw(g2d, 200, 550);

				//instruction mnemonic
				textVariable = new TextLayout("Instruction", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 25, 480);

				textVariable = new TextLayout(opcodeEquivalenceTable.get(instructionCode.substring(0, 6)), font, frc);
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 25, 500);

				textVariable = new TextLayout(registerEquivalenceTable.get(instructionCode.substring(6, 11)), font,
						frc);
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 105, 500);

				textVariable = new TextLayout(registerEquivalenceTable.get(instructionCode.substring(11, 16)), font,
						frc);
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 65, 500);

				textVariable = new TextLayout(parseBinToInt(instructionCode.substring(16, 32)), font, frc);
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 155, 500);
			} else { //imediate instructions
				textVariable = new TextLayout("IMMEDIATE TYPE INSTRUCTION", new Font("Verdana", Font.BOLD, 25), frc);
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 250, 30);

				//label opcode
				textVariable = new TextLayout("opcode", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 25, 530);

				//initialize of opcode
				textVariable = new TextLayout(instructionCode.substring(0, 6), font, frc);
				g2d.setColor(Color.magenta);
				textVariable.draw(g2d, 25, 550);

				//rs label
				textVariable = new TextLayout("rs", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 90, 530);

				//initialize of rs
				textVariable = new TextLayout(instructionCode.substring(6, 11), font, frc);
				g2d.setColor(Color.green);
				textVariable.draw(g2d, 90, 550);

				//rt label
				textVariable = new TextLayout("rt", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 145, 530);

				//initialize of rt
				textVariable = new TextLayout(instructionCode.substring(11, 16), font, frc);
				g2d.setColor(Color.blue);
				textVariable.draw(g2d, 145, 550);

				// rd label
				textVariable = new TextLayout("Immediate", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 200, 530);

				//initialize of immediate
				textVariable = new TextLayout(instructionCode.substring(16, 32), font, frc);
				g2d.setColor(Color.cyan);
				textVariable.draw(g2d, 200, 550);

				//instruction mnemonic
				textVariable = new TextLayout("Instruction", fontTitle, frc);
				g2d.setColor(Color.red);
				textVariable.draw(g2d, 25, 480);
				textVariable = new TextLayout(opcodeEquivalenceTable.get(instructionCode.substring(0, 6)), font, frc);
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 25, 500);

				textVariable = new TextLayout(registerEquivalenceTable.get(instructionCode.substring(6, 11)), font,
						frc);
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 105, 500);

				textVariable = new TextLayout(registerEquivalenceTable.get(instructionCode.substring(11, 16)), font,
						frc);
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 65, 500);

				textVariable = new TextLayout(parseBinToInt(instructionCode.substring(16, 32)), font, frc);
				g2d.setColor(Color.black);
				textVariable.draw(g2d, 155, 500);
			}

			//Type of control signal labels
			textVariable = new TextLayout("Control Signals", fontTitle, frc);
			g2d.setColor(Color.red);
			textVariable.draw(g2d, 25, 440);

			textVariable = new TextLayout("Active", font, frc);
			g2d.setColor(Color.red);
			textVariable.draw(g2d, 25, 455);

			textVariable = new TextLayout("Inactive", font, frc);
			g2d.setColor(Color.gray);
			textVariable.draw(g2d, 75, 455);

			textVariable = new TextLayout(
					"To see details of control units and register bank click inside the functional block", font, frc);
			g2d.setColor(Color.black);
			textVariable.draw(g2d, 400, 550);
		}
		//end of instruction subtitle...

		//set the initial state of the variables that controls the animation, and start the timer that triggers the animation.
		public void startAnimation(final String codeInstruction) {
			instructionCode = codeInstruction;
			time = new Timer(PERIOD, this);    // start timer
			time.start();
			// 	this.repaint();
		}

		//initialize the image of datapath.
		private void initImages() {
			try {
				final BufferedImage im = ImageIO.read(getClass().getResource(Globals.imagesPath + "datapath.png"));

				final int transparency = im.getColorModel().getTransparency();
				datapath = gc.createCompatibleImage(im.getWidth(), im.getHeight(), transparency);
				g2d = datapath.createGraphics();
				g2d.drawImage(im, 0, 0, null);
				g2d.dispose();
			} catch (final IOException e) {
				System.out.println("Load Image error for " + getClass().getResource(Globals.imagesPath + "datapath.png")
						+ ":\n" + e);
			}
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

		public void printTextDtoU(final Vertex v) {
			final FontRenderContext frc = g2d.getFontRenderContext();

			TextLayout actionInFunctionalBlock = new TextLayout(v.getName(), new Font("Verdana", Font.BOLD, 13), frc);
			g2d.setColor(Color.RED);

			if (instructionCode.substring(0, 6).matches("101[0-1][0-1][0-1]") && !instructionCode.substring(0, 6)
					.matches("0001[0-1][0-1]") && !instructionCode.substring(0, 6).matches("00001[0-1]")) {//load instruction
				actionInFunctionalBlock = new TextLayout(" ", new Font("Verdana", Font.BOLD, 13), frc);
			}
			if (v.getName().equals("ALUVALUE")) {
				if (instructionCode.substring(0, 6).equals("000000")) {
					actionInFunctionalBlock = new TextLayout(functionEquivalenceTable.get(instructionCode.substring(26,
							32)), new Font("Verdana", Font.BOLD, 13), frc);
				} else {
					actionInFunctionalBlock = new TextLayout(opcodeEquivalenceTable.get(instructionCode.substring(0,
							6)), new Font("Verdana", Font.BOLD, 13), frc);
				}
			}

			if (instructionCode.substring(0, 6).matches("0001[0-1][0-1]") && v.getName().equals("CP+4")) {
				actionInFunctionalBlock = new TextLayout("PC+OFFSET", new Font("Verdana", Font.BOLD, 13), frc);
			}

			if (v.getName().equals("WRITING")) {
				if (!instructionCode.substring(0, 6).matches("100[0-1][0-1][0-1]")) {
					actionInFunctionalBlock = new TextLayout(" ", new Font("Verdana", Font.BOLD, 13), frc);
				}
			}
			if (v.isActive() == true) {
				v.setFirst_interaction(false);
				actionInFunctionalBlock.draw(g2d, v.getOppositeAxis(), v.getCurrent());
				if (v.getCurrent() == v.getEnd()) { v.setActive(false); }
				v.setCurrent(v.getCurrent() - 1);
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
			setUpInstructionInfo(g2d);
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
							printTextDtoU(vert);
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

		@Override
		public void mouseClicked(final MouseEvent e) {

			MouseInfo.getPointerInfo();

			if (e.getPoint().getX() > 425 && e.getPoint().getX() < 520 && e.getPoint().getY() > 300 && e.getPoint()
					.getY() < 425) {
				buildMainDisplayArea("register.png");
				final FunctionUnitVisualization fu = new FunctionUnitVisualization(instructionBinary, register);
				fu.run();
			}

			if (e.getPoint().getX() > 355 && e.getPoint().getX() < 415 && e.getPoint().getY() > 180 && e.getPoint()
					.getY() < 280) {
				buildMainDisplayArea("control.png");
				final FunctionUnitVisualization fu = new FunctionUnitVisualization(instructionBinary, control);
				fu.run();
			}

			if (e.getPoint().getX() > 560 && e.getPoint().getX() < 620 && e.getPoint().getY() > 450 && e.getPoint()
					.getY() < 520) {
				buildMainDisplayArea("ALUcontrol.png");
				final FunctionUnitVisualization fu = new FunctionUnitVisualization(instructionBinary, aluControl);
				fu.run();
			}

		}

		@Override
		public void mouseEntered(final MouseEvent e) {}

		@Override
		public void mouseExited(final MouseEvent e) {}

		@Override
		public void mouseReleased(final MouseEvent e) {}
	}
}
