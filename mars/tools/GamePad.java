package mars.tools;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import mars.mips.hardware.*;
import mars.tools.*;

public class GamePad extends AbstractMarsToolAndApplication {

  public GamePad() { super("Game Station", "Game interface"); }

  // Required method - returns the name that appears in MARS Tools menu
  public String getName() { return "Game Station"; }

  // Required method - builds the main display area of your tool
  protected JComponent buildMainDisplayArea() {
    JPanel panel = new JPanel();
    // Add your tool's GUI components here

    return panel;
  }
}