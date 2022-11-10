/*
 * Copyright 2020 FormDev Software GmbH
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package themeengine.include.com.formdev.flatlaf.ui;

import static java.awt.Cursor.*;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JComponent;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.UIManager;
import themeengine.include.com.formdev.flatlaf.util.UIScale;

/**
 * Resizes frames and dialogs.
 *
 * @author Karl Tauber
 */
public class FlatWindowResizer
	implements PropertyChangeListener, WindowStateListener, ComponentListener
{
	protected final static Integer WINDOW_RESIZER_LAYER = JLayeredPane.DRAG_LAYER + 1;

	protected final JRootPane rootPane;

	protected final int borderDragThickness = FlatUIUtils.getUIInt( "RootPane.borderDragThickness", 5 );
	protected final int cornerDragWidth = FlatUIUtils.getUIInt( "RootPane.cornerDragWidth", 16 );
	protected final boolean honorFrameMinimumSizeOnResize = UIManager.getBoolean( "RootPane.honorFrameMinimumSizeOnResize" );
	protected final boolean honorDialogMinimumSizeOnResize = UIManager.getBoolean( "RootPane.honorDialogMinimumSizeOnResize" );

	protected final JComponent north;
	protected final JComponent south;
	protected final JComponent west;
	protected final JComponent east;

	protected Window window;

	public FlatWindowResizer( JRootPane rootPane ) {
		this.rootPane = rootPane;

		north = createDragBorderComponent( NW_RESIZE_CURSOR, N_RESIZE_CURSOR, NE_RESIZE_CURSOR );
		south = createDragBorderComponent( SW_RESIZE_CURSOR, S_RESIZE_CURSOR, SE_RESIZE_CURSOR );
		west = createDragBorderComponent( NW_RESIZE_CURSOR, W_RESIZE_CURSOR, SW_RESIZE_CURSOR );
		east = createDragBorderComponent( NE_RESIZE_CURSOR, E_RESIZE_CURSOR, SE_RESIZE_CURSOR );

		JLayeredPane layeredPane = rootPane.getLayeredPane();
		layeredPane.add( north, WINDOW_RESIZER_LAYER );
		layeredPane.add( south, WINDOW_RESIZER_LAYER );
		layeredPane.add( west, WINDOW_RESIZER_LAYER );
		layeredPane.add( east, WINDOW_RESIZER_LAYER );

		rootPane.addComponentListener( this );
		rootPane.addPropertyChangeListener( "ancestor", this );

		if( rootPane.isDisplayable() )
			addNotify();
	}

	protected DragBorderComponent createDragBorderComponent( int leadingResizeDir, int centerResizeDir, int trailingResizeDir ) {
		return new DragBorderComponent( leadingResizeDir, centerResizeDir, trailingResizeDir );
	}

	public void uninstall() {
		removeNotify();

		rootPane.removeComponentListener( this );
		rootPane.removePropertyChangeListener( "ancestor", this );

		JLayeredPane layeredPane = rootPane.getLayeredPane();
		layeredPane.remove( north );
		layeredPane.remove( south );
		layeredPane.remove( west );
		layeredPane.remove( east );
	}

	public void doLayout() {
		if( !north.isVisible() )
			return;

		int x = 0;
		int y = 0;
		int width = rootPane.getWidth();
		int height = rootPane.getHeight();
		if( width == 0 || height == 0 )
			return;

		int thickness = UIScale.scale( borderDragThickness );
		int y2 = y + thickness;
		int height2 = height - (thickness * 2);

		north.setBounds( x, y, width, thickness );
		south.setBounds( x, y + height - thickness, width, thickness );
		west.setBounds( x, y2, thickness, height2 );
		east.setBounds( x + width - thickness, y2, thickness, height2 );
	}

	protected void addNotify() {
		Container parent = rootPane.getParent();
		window = (parent instanceof Window) ? (Window) parent : null;
		if( window instanceof Frame ) {
			window.addPropertyChangeListener( "resizable", this );
			window.addWindowStateListener( this );
		}

		updateVisibility();
	}

	protected void removeNotify() {
		if( window instanceof Frame ) {
			window.removePropertyChangeListener( "resizable", this );
			window.removeWindowStateListener( this );
		}
		window = null;

		updateVisibility();
	}

	protected void updateVisibility() {
		boolean visible = isWindowResizable();
		if( visible == north.isVisible() )
			return;

		north.setVisible( visible );
		south.setVisible( visible );
		west.setVisible( visible );

		// The east component is not hidden, instead its bounds are set to 0,0,1,1 and
		// it is disabled. This is necessary so that DragBorderComponent.paintComponent() is invoked.
		east.setEnabled( visible );
		if( visible ) {
			east.setVisible( true ); // necessary because it is initially invisible
			doLayout();
		} else
			east.setBounds( 0, 0, 1, 1 );
	}

	protected boolean isWindowResizable() {
		if( window instanceof Frame )
			return ((Frame)window).isResizable() && (((Frame)window).getExtendedState() & Frame.MAXIMIZED_BOTH) == 0;
		if( window instanceof Dialog )
			return ((Dialog)window).isResizable();
		return false;
	}

	@Override
	public void propertyChange( PropertyChangeEvent e ) {
		switch( e.getPropertyName() ) {
			case "ancestor":
				if( e.getNewValue() != null )
					addNotify();
				else
					removeNotify();
				break;

			case "resizable":
				updateVisibility();
				break;
		}
	}

	@Override
	public void windowStateChanged( WindowEvent e ) {
		updateVisibility();
	}

	@Override
	public void componentResized( ComponentEvent e ) {
		doLayout();
	}

	@Override public void componentMoved( ComponentEvent e ) {}
	@Override public void componentShown( ComponentEvent e ) {}
	@Override public void componentHidden( ComponentEvent e ) {}

	//---- class DragBorderComponent ------------------------------------------

	protected class DragBorderComponent
		extends JComponent
		implements MouseListener, MouseMotionListener
	{
		private final int leadingResizeDir;
		private final int centerResizeDir;
		private final int trailingResizeDir;

		private int resizeDir = -1;
		private int dragStartMouseX;
		private int dragStartMouseY;
		private Rectangle dragStartWindowBounds;

		protected DragBorderComponent( int leadingResizeDir, int centerResizeDir, int trailingResizeDir ) {
			this.leadingResizeDir = leadingResizeDir;
			this.centerResizeDir = centerResizeDir;
			this.trailingResizeDir = trailingResizeDir;

			setResizeDir( centerResizeDir );
			setVisible( false );

			addMouseListener( this );
			addMouseMotionListener( this );
		}

		protected void setResizeDir( int resizeDir ) {
			if( this.resizeDir == resizeDir )
				return;
			this.resizeDir = resizeDir;

			setCursor( getPredefinedCursor( resizeDir ) );
		}

		@Override
		public Dimension getPreferredSize() {
			int thickness = UIScale.scale( borderDragThickness );
			return new Dimension( thickness, thickness );
		}

		@Override
		protected void paintComponent( Graphics g ) {
			super.paintChildren( g );

			// this is necessary because Dialog.setResizable() does not fire events
			if( window instanceof Dialog )
				updateVisibility();

/*debug
			g.setColor( java.awt.Color.red );
			g.drawRect( 0, 0, getWidth() - 1, getHeight() - 1 );
debug*/
		}

		@Override
		public void mouseClicked( MouseEvent e ) {
		}

		@Override
		public void mousePressed( MouseEvent e ) {
			if( window == null )
				return;

			dragStartMouseX = e.getXOnScreen();
			dragStartMouseY = e.getYOnScreen();
			dragStartWindowBounds = window.getBounds();
		}

		@Override
		public void mouseReleased( MouseEvent e ) {
			dragStartWindowBounds = null;
		}

		@Override
		public void mouseEntered( MouseEvent e ) {
		}

		@Override
		public void mouseExited( MouseEvent e ) {
		}

		@Override
		public void mouseMoved( MouseEvent e ) {
			boolean topBottom = (centerResizeDir == N_RESIZE_CURSOR || centerResizeDir == S_RESIZE_CURSOR);
			int xy = topBottom ? e.getX() : e.getY();
			int wh = topBottom ? getWidth() : getHeight();
			int cornerWH = UIScale.scale( cornerDragWidth - (topBottom ? 0 : borderDragThickness) );

			setResizeDir( xy <= cornerWH
				? leadingResizeDir
				: (xy >= wh - cornerWH
					? trailingResizeDir
					: centerResizeDir) );
		}

		@Override
		public void mouseDragged( MouseEvent e ) {
			if( dragStartWindowBounds == null )
				return;

			if( !isWindowResizable() )
				return;

			int mouseDeltaX = e.getXOnScreen() - dragStartMouseX;
			int mouseDeltaY = e.getYOnScreen() - dragStartMouseY;

			int deltaX = 0;
            int deltaY = 0;
            int deltaWidth = 0;
            int deltaHeight = 0;

			// north
			if( resizeDir == N_RESIZE_CURSOR || resizeDir == NW_RESIZE_CURSOR || resizeDir == NE_RESIZE_CURSOR ) {
				deltaY = mouseDeltaY;
				deltaHeight = -mouseDeltaY;
			}

			// south
			if( resizeDir == S_RESIZE_CURSOR || resizeDir == SW_RESIZE_CURSOR || resizeDir == SE_RESIZE_CURSOR )
				deltaHeight = mouseDeltaY;

			// west
			if( resizeDir == W_RESIZE_CURSOR || resizeDir == NW_RESIZE_CURSOR || resizeDir == SW_RESIZE_CURSOR ) {
				deltaX = mouseDeltaX;
				deltaWidth = -mouseDeltaX;
			}

			// east
			if( resizeDir == E_RESIZE_CURSOR || resizeDir == NE_RESIZE_CURSOR || resizeDir == SE_RESIZE_CURSOR )
				deltaWidth = mouseDeltaX;

			// compute new window bounds
			Rectangle newBounds = new Rectangle( dragStartWindowBounds );
			newBounds.x += deltaX;
			newBounds.y += deltaY;
			newBounds.width += deltaWidth;
			newBounds.height += deltaHeight;

			// apply minimum window size
			boolean honorMinimumSizeOnResize =
				(honorFrameMinimumSizeOnResize && window instanceof Frame) ||
				(honorDialogMinimumSizeOnResize && window instanceof Dialog);
			Dimension minimumSize = honorMinimumSizeOnResize ? window.getMinimumSize() : null;
			if( minimumSize == null )
				minimumSize = UIScale.scale( new Dimension( 150, 50 ) );
			if( newBounds.width < minimumSize.width ) {
				if( deltaX != 0 )
					newBounds.x -= (minimumSize.width - newBounds.width);
				newBounds.width = minimumSize.width;
			}
			if( newBounds.height < minimumSize.height ) {
				if( deltaY != 0 )
					newBounds.y -= (minimumSize.height - newBounds.height);
				newBounds.height = minimumSize.height;
			}

			// set window bounds
			if( !newBounds.equals( dragStartWindowBounds ) ) {
				window.setBounds( newBounds );

				// immediately layout drag border components
				FlatWindowResizer.this.doLayout();

				if( Toolkit.getDefaultToolkit().isDynamicLayoutActive() ) {
					window.validate();
					rootPane.repaint();
				}
			}
		}
	}
}
