import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.awt.image.BufferedImage;

public class MultiprocessingDemo1 extends JPanel {

	public static void main(String[] args) {
		JFrame window = new JFrame("Multiprocessing Demo 1");
		MultiprocessingDemo1 content = new MultiprocessingDemo1();
		window.setContentPane(content);
		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		window.pack();
		window.setResizable(false);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		window.setLocation((screenSize.width - window.getWidth()) / 2,
				(screenSize.height - window.getHeight()) / 2);
		window.setVisible(true);
	}

	private Runner[] workers; 
	private volatile boolean running; 
	private volatile int threadsCompleted; 
	private JButton startButton;
	private JComboBox threadCountSelect; 
	private BufferedImage image; 

	private JPanel display = new JPanel() {
		protected void paintComponent(Graphics g) {

			if (image == null)
				super.paintComponent(g); 
			else {
				synchronized (image) {
					g.drawImage(image, 0, 0, null);
				}
			}
		}
	};


	public MultiprocessingDemo1() {
		display.setPreferredSize(new Dimension(800, 600));
		display.setBackground(Color.LIGHT_GRAY);
		setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
		setLayout(new BorderLayout());
		add(display, BorderLayout.CENTER);
		JPanel bottom = new JPanel();
		startButton = new JButton("Start");
		bottom.add(startButton);
		threadCountSelect = new JComboBox();
		threadCountSelect.addItem("Use 1 thread.");
		threadCountSelect.addItem("Use 2 threads.");
		threadCountSelect.addItem("Use 3 threads.");
		threadCountSelect.addItem("Use 4 threads.");
		threadCountSelect.addItem("Use 5 threads.");
		threadCountSelect.setSelectedIndex(1);
		bottom.add(threadCountSelect);
		bottom.setBackground(Color.WHITE);
		add(bottom, BorderLayout.SOUTH);
		startButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (running)
					stop();
				else
					start();
			}
		});
	}

	private void start() {
		startButton.setText("Abort"); 
		threadCountSelect.setEnabled(false); 
		int width = display.getWidth() + 2;
		int height = display.getHeight() + 2;
		if (image == null)

			image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

		Graphics g = image.getGraphics();
		g.setColor(Color.LIGHT_GRAY);
		g.fillRect(0, 0, width, height);
		g.dispose();
		display.repaint();
		int threadCount = threadCountSelect.getSelectedIndex() + 1;
		workers = new Runner[threadCount];
		int rowsPerThread; 
		rowsPerThread = height / threadCount;
		running = true; 
		threadsCompleted = 0; 
		for (int i = 0; i < threadCount; i++) {
			int startRow; 
			int endRow; 
			startRow = rowsPerThread * i;
			if (i == threadCount - 1)
				endRow = height - 1;
			else
				endRow = rowsPerThread * (i + 1) - 1;
			workers[i] = new Runner(startRow, endRow);
			try {
				workers[i]
						.setPriority(Thread.currentThread().getPriority() - 1);
			} catch (Exception e) {
			}
			workers[i].start();
		}
	}

	private void stop() {
		startButton.setEnabled(false); 
		running = false;
	}

	synchronized private void threadFinished() {
		threadsCompleted++;
		if (threadsCompleted == workers.length) { 
			startButton.setText("Start Again");
			startButton.setEnabled(true);
			running = false; 
			workers = null;
			threadCountSelect.setEnabled(true); 
		}
	}

	private class Runner extends Thread {
		double xmin, xmax, ymin, ymax;
		int maxIterations;
		int[] rgb;
		int[] palette;
		int width, height;
		int startRow, endRow;

		Runner(int startRow, int endRow) {
			this.startRow = startRow;
			this.endRow = endRow;
			width = image.getWidth();
			height = image.getHeight();
			rgb = new int[width];
			palette = new int[256];
			for (int i = 0; i < 256; i++)
				palette[i] = Color.getHSBColor(i / 255F, 1, 1).getRGB();
			xmin = -1.6744096740931858;
			xmax = -1.674409674093473;
			ymin = 4.716540768697223E-5;
			ymax = 4.716540790246652E-5;
			maxIterations = 10000;
		}

		public void run() {
			try {
				double x, y;
				double dx, dy;
				dx = (xmax - xmin) / (width - 1);
				dy = (ymax - ymin) / (height - 1);

				for (int row = startRow; row <= endRow; row++) { 
					y = ymax - dy * row;
					for (int col = 0; col < width; col++) {
						x = xmin + dx * col;
						int count = 0;
						double xx = x;
						double yy = y;
						while (count < maxIterations && (xx * xx + yy * yy) < 4) {
							count++;
							double newxx = xx * xx - yy * yy + x;
							yy = 2 * xx * yy + y;
							xx = newxx;
						}
						if (count == maxIterations)
							rgb[col] = 0;
						else
							rgb[col] = palette[count % palette.length];
					}
					if (!running) { 
						return;
					}
					synchronized (image) {
						image.setRGB(0, row, width, 1, rgb, 0, width);
					}
					display.repaint(0, row, width, 1); 
				}
			} finally {
				threadFinished();
			}
		}
	}

}
