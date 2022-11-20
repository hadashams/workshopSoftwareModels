package game;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import tau.smlab.syntech.controller.executor.ControllerExecutor;
import tau.smlab.syntech.games.controller.jits.BasicJitController;

/*
 * Manages the simulation - GUI, controller input/output, board (visualization)
 */

public class ControlPanel {
	// board dimensions
	int x;
	int y;
	// board constants
	final int dim = 100;
	static final int y_offset = 30;

	int num_frogs;
	int num_cars;
	Point[] frog;
	Point[] cars;
	Point[] free_points;
	Point[] goals;

	// holds the previous position (for use when animating transitions)
	Point[] frog_prev = new Point[num_frogs];

	// Board and GUI elements
	JFrame frame;
	Board board;
	JButton advance_button;
	JButton autorun_button;

	// holds states for the animation
	boolean ready_for_next;
	boolean autorun;

	// The controller and its inputs
	ControllerExecutor executor;
	Map<String, String> inputs = new HashMap<String, String>();

	// The path to the controller files
	String path;

	public ControlPanel(int x, int y, int num_frogs, Point[] cars, Point[] free_points, Point[] goals, String path) {
		this.x = x;
		this.y = y;
		this.num_frogs = num_frogs;
		this.num_cars = cars.length;
		this.frog = new Point[num_frogs];
		this.frog_prev = new Point[num_frogs];
		this.cars = cars;
		this.free_points = free_points;
		this.goals = goals;
		this.path = path;
	}

	public boolean is_free_point(Point p) {
		for (int i = 0; i < this.free_points.length; i++) {
			if (p.getX() == this.free_points[i].getX() && p.getY() == this.free_points[i].getY()) {
				return true;
			}
		}
		return false;
	}

	public void init() throws Exception {
		autorun = false;

		for (int i = 0; i < num_frogs; i++) {
			frog[i] = new Point();
			frog_prev[i] = new Point();
		}

		// init controller
		executor = new ControllerExecutor(new BasicJitController(), this.path, "FrogAndPond");
		
		// set initial cars location
		inputs.put("freePoint1Y", Integer.toString(4));
		inputs.put("freePoint2Y", Integer.toString(6));
		
		executor.initState(inputs);

		Map<String, String> sysValues = executor.getCurrOutputs();

		// set initial robot locations
		for (int i = 0; i < num_frogs; i++) {
			frog_prev[i].setX(Integer.parseInt(sysValues.get("frogX")));
			frog_prev[i].setY(Integer.parseInt(sysValues.get("frogY")));
			frog[i].setX(Integer.parseInt(sysValues.get("frogX")));
			frog[i].setY(Integer.parseInt(sysValues.get("frogY")));
		}

		setUpUI();
	}
	
	public static int fixYBounds(int y) {
		if (y < 0) {
			return 7;
		}
		if (y > 7) {
			return 0;
		}
		return y;
	}
	
	void next() throws Exception {

		Map<String, String> sysValues = executor.getCurrOutputs();
		int frogCounter = Integer.parseInt(sysValues.get("frogCounter"));
		int frogX = Integer.parseInt(sysValues.get("frogX"));
		
		int[] movesOptions = new int[] {-1, 0 ,1};
		Random rand = new Random();

		//if cars can move 
		if (frogCounter == 3 && frogX != 2 && frogX != 5) {
			int nextY1 = movesOptions[rand.nextInt(3)];
			int nextY2 = movesOptions[rand.nextInt(3)];
			int y1 = fixYBounds(Integer.parseInt(inputs.get("freePoint1Y")) + nextY1);
			int y2 = fixYBounds(Integer.parseInt(inputs.get("freePoint2Y")) + nextY2);
			this.free_points[0].y = y1;
			this.free_points[1].y = y2;
			inputs.put("freePoint1Y", Integer.toString(y1));
			inputs.put("freePoint2Y", Integer.toString(y2));
		}
		
		
		ready_for_next = false;
		advance_button.setText("...");
		for (int i = 0; i < num_frogs; i++) {
			frog_prev[i].setX(frog[i].getX());
			frog_prev[i].setY(frog[i].getY());
		}
		executor.updateState(inputs);

		

		// Update robot locations
		for (int i = 0; i < num_frogs; i++) {
			frog[i].setX(Integer.parseInt(sysValues.get("frogX")));
			frog[i].setY(Integer.parseInt(sysValues.get("frogY")));
		}

		// Animate transition
		board.animate();
	}

	void setUpUI() throws Exception {
		advance_button = new JButton();
		autorun_button = new JButton();
		frame = new JFrame();
		frame.add(advance_button);
		frame.add(autorun_button);
		board = new Board(this);
		board.init();
		frame.setTitle("The Frog and the Pond");
		frame.setSize(x * dim + 8 + 150, y * dim + y_offset + 8);
		board.setSize(x * dim, y * dim);
		frame.setLayout(null);
		frame.add(board, BorderLayout.CENTER);

		// Handle presses of the "next step" button
		advance_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					if (ready_for_next && !autorun)
						next();
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		});
		advance_button.setBounds(x * dim + 8, 0, 130, 50);
		advance_button.setText("Start");

		// Handle presses of the "autorun/stop autorun" button
		autorun_button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				try {
					if (autorun) {
						autorun = false;
						autorun_button.setText("Auto run");
					} else if (ready_for_next) {
						autorun = true;
						autorun_button.setText("Stop Auto run");
						next();
					}
				} catch (Exception e) {
					System.out.println(e);
				}
			}
		});
		autorun_button.setBounds(x * dim + 8, 50, 130, 50);
		autorun_button.setText("Auto run");
		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		frame.setVisible(true);
		board.setVisible(true);
		advance_button.setVisible(true);
		autorun_button.setVisible(true);
		ready_for_next = true;
	}

}
