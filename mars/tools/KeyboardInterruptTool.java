package mars.tools;

import com.jogamp.opengl.*;
import com.jogamp.opengl.awt.GLCanvas;
import com.jogamp.opengl.util.FPSAnimator;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.util.*;
import javax.swing.*;
import mars.*;
import mars.mips.hardware.*;
import mars.simulator.*;
import mars.tools.AbstractMarsToolAndApplication;


/**
 * A MARS tool that generates interrupts when keyboard keys are pressed.
 * This tool will cause the MIPS program to jump to a label named
 * __interrupt_KBD whenever a key is pressed.
 */
public class KeyboardInterruptTool extends AbstractMarsToolAndApplication {
  // GUI components
  private JTextArea displayArea;
  private JLabel statusLabel;
  private boolean isFocused;

  // Memory address where we'll store the latest key pressed
  private static final int KEY_PRESS_ADDRESS =
      0xFFFF0000; // Using memory-mapped I/O address
  private static final int KEY_RELEASE_ADDRESS =
      0xFFFF0004; // Using memory-mapped I/O address
  enum KeyType { PRESS, RELEASE }

  /**
   * Constructor sets up the tool's basic properties
   */
  public KeyboardInterruptTool() {
    super("Keyboard Interrupt Tool", "Generate Keyboard Interrupts");
    isFocused = false;
  }

  /**
   * Returns the name that will appear in MARS Tools menu
   */
  public String getName() { return "Keyboard Interrupt Tool"; }

  /**
   * Set up our tool to observe memory
   */
  protected void addAsObserver() {
    // We only need to observe the memory locations we're using
    addAsObserver(KEY_PRESS_ADDRESS, KEY_PRESS_ADDRESS);
    addAsObserver(KEY_RELEASE_ADDRESS, KEY_RELEASE_ADDRESS);
  }

  protected void processMIPSUpdate(Observable memory,
                                   AccessNotice accessNotice) {
    if (accessNotice.getAccessType() == AccessNotice.READ) {
      MemoryAccessNotice mem = (MemoryAccessNotice)accessNotice;

      try {
        if (mem.getAddress() == KEY_PRESS_ADDRESS && mem.getValue() != 0) {
          Globals.memory.setWord(KEY_PRESS_ADDRESS, 0);
        } else if (mem.getAddress() == KEY_RELEASE_ADDRESS &&
                   mem.getValue() != 0) {
          Globals.memory.setWord(KEY_RELEASE_ADDRESS, 0);
        }
      } catch (AddressErrorException ex) {
      }
    }
  }

  /**
   * Builds the main interface for the tool
   */
  protected JComponent buildMainDisplayArea() {
    JPanel panel = new JPanel(new BorderLayout(10, 10));
    panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

    // Create display area for showing key events
    displayArea = new JTextArea(10, 40);
    displayArea.setEditable(false);
    displayArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 14));
    JScrollPane scrollPane = new JScrollPane(displayArea);

    // Create status label to show focus state
    statusLabel =
        new JLabel("Click here and connect to enable keyboard interrupts");
    statusLabel.setHorizontalAlignment(JLabel.CENTER);

    // Add key listener to the panel
    panel.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(KeyEvent e) {
        if (isObserving()) {
          handleKeyEvent(e, KeyType.PRESS);
        }
      }

      @Override
      public void keyReleased(KeyEvent e) {
        handleKeyEvent(e, KeyType.RELEASE);
      }
    });

    // Add mouse listener to handle focus
    panel.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        panel.requestFocusInWindow();
        isFocused = true;
        updateStatus();
      }
    });

    // Add focus listeners
    panel.addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent e) {
        isFocused = true;
        updateStatus();
      }

      @Override
      public void focusLost(FocusEvent e) {
        isFocused = false;
        updateStatus();
      }
    });

    panel.setFocusable(true);

    panel.add(scrollPane, BorderLayout.CENTER);
    panel.add(statusLabel, BorderLayout.SOUTH);

    return panel;
  }

  /**
   * Handles a key press by triggering a MIPS interrupt
   */
  private void handleKeyEvent(KeyEvent e, KeyType t) {
    try {
      if (t == KeyType.PRESS) {
        Globals.memory.setWord(KEY_PRESS_ADDRESS, e.getKeyCode());
      } else {
        Globals.memory.setWord(KEY_RELEASE_ADDRESS, e.getKeyCode());
      }

      // Log the key press
      displayArea.append(String.format("Key %s: %s (code: %d)\n", t.toString(),
                                       KeyEvent.getKeyText(e.getKeyCode()),
                                       e.getKeyCode()));
      displayArea.setCaretPosition(displayArea.getDocument().getLength());

      // Check if interrupts are enabled
    } catch (AddressErrorException ex) {
      displayArea.append("Error accessing memory: " + ex.getMessage() + "\n");
    }
  }

  /**
   * Updates the status label based on focus and connection state
   */
  private void updateStatus() {
    if (!isFocused) {
      statusLabel.setText("Click here to enable keyboard input");
    } else if (!isObserving()) {
      statusLabel.setText("Click 'Connect' to enable interrupts");
    } else {
      statusLabel.setText("Ready for keyboard input");
    }
  }

  /**
   * Reset clears the display and memory
   */
  protected void reset() {
    displayArea.setText("");
    isFocused = false;
    updateStatus();

    try {
      // Clear our memory-mapped I/O addresses
      Globals.memory.setWord(KEY_PRESS_ADDRESS, 0);
      Globals.memory.setWord(KEY_RELEASE_ADDRESS, 0);
    } catch (AddressErrorException ex) {
      displayArea.append("Error resetting memory: " + ex.getMessage() + "\n");
    }
  }

  public void setPixel(int x, int y, int r, int g, int b) {
    int index = (y * WIDTH + x) * 3;
    pixelBuffer.put(index, (byte)r);
    pixelBuffer.put(index + 1, (byte)g);
    pixelBuffer.put(index + 2, (byte)b);
    glCanvas.repaint();
  }

  @Override
  public void init(GLAutoDrawable drawable) {
    GL2 gl = drawable.getGL().getGL2();
    int[] textures = new int[1];
    gl.glGenTextures(1, textures, 0);
    textureId = textures[0];
    gl.glBindTexture(GL2.GL_TEXTURE_2D, textureId);
    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MIN_FILTER,
                       GL2.GL_NEAREST);
    gl.glTexParameteri(GL2.GL_TEXTURE_2D, GL2.GL_TEXTURE_MAG_FILTER,
                       GL2.GL_NEAREST);
    gl.glTexImage2D(GL2.GL_TEXTURE_2D, 0, GL2.GL_RGB, WIDTH, HEIGHT, 0,
                    GL2.GL_RGB, GL2.GL_UNSIGNED_BYTE, pixelBuffer);
  }

  @Override
  public void display(GLAutoDrawable drawable) {
    GL2 gl = drawable.getGL().getGL2();
    gl.glClear(GL2.GL_COLOR_BUFFER_BIT);
    gl.glBindTexture(GL2.GL_TEXTURE_2D, textureId);
    gl.glTexSubImage2D(GL2.GL_TEXTURE_2D, 0, 0, 0, WIDTH, HEIGHT, GL2.GL_RGB,
                       GL2.GL_UNSIGNED_BYTE, pixelBuffer);
    gl.glBegin(GL2.GL_QUADS);
    gl.glTexCoord2f(0, 0);
    gl.glVertex2f(-1, -1);
    gl.glTexCoord2f(1, 0);
    gl.glVertex2f(1, -1);
    gl.glTexCoord2f(1, 1);
    gl.glVertex2f(1, 1);
    gl.glTexCoord2f(0, 1);
    gl.glVertex2f(-1, 1);
    gl.glEnd();
  }

  @Override
  public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {}
  @Override
  public void dispose(GLAutoDrawable drawable) {}
}