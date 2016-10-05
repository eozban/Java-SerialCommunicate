package com.esintibilisim.serialcommunicate;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import com.fazecast.jSerialComm.SerialPort;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JComboBox;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.awt.event.ActionEvent;
import java.awt.FlowLayout;


public class frmMain extends JFrame {
	private static final long serialVersionUID = 1L;

	private SerialPort serialPorts[] = null;
	private String[] bauntRates = new String[] { "300","600","1200","2400","4800","9600","14400","19200","28800","38400","56000","57600","115200" };
	private SerialPort currentPort = null;
	private Thread readThread = null;
	private SimpleDateFormat stringFormatter = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss.SSS");
	
	private JPanel contentPane;
	private JComboBox<String> cmbSerialPorts;
	private JComboBox<String> cmbBauntRates;
	private JTextArea textAreaConsole;
	private JScrollPane scrollPanel1;
	private JButton btnConnect;
	private JButton btnRead;
	private JButton btnSendCommand;

	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					frmMain frame = new frmMain();
					frame.setLocationRelativeTo(null);
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	public frmMain() {
		setTitle("Java-SerialPort Okuma/Yazma");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 700, 400);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);
		
		JPanel pnlTop = new JPanel();
		contentPane.add(pnlTop, BorderLayout.NORTH);
		
		JButton btnYenile = new JButton("Refresh");
		btnYenile.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				loadSerialPorts();
			}
		});
		pnlTop.setLayout(new FlowLayout(FlowLayout.LEFT, 5, 5));
		pnlTop.add(btnYenile);
		
		cmbSerialPorts = new JComboBox<String>();
		pnlTop.add(cmbSerialPorts);
		
		cmbBauntRates = new JComboBox<String>(bauntRates);
		cmbBauntRates.setSelectedIndex(5);
		pnlTop.add(cmbBauntRates);
		
		btnConnect = new JButton("Connect");
		btnConnect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
					
				if (currentPort != null) {
					if (currentPort.isOpen()) {
						disconnectSerialPort();
					} else {
						connectSerialPort();
					}
				} else {
					connectSerialPort();
				}
			}
		});
		pnlTop.add(btnConnect);
		
		btnRead = new JButton("Start Read");
		btnRead.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (cmbSerialPorts.getSelectedItem() != null) {
					readSerialPort();
				} else {
					JOptionPane.showMessageDialog(null, "SeriPort Seçilmedi!");
				}				
			}
		});
		pnlTop.add(btnRead);
		
		btnSendCommand = new JButton("Send Command");
		btnSendCommand.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				sendCommand();
			}
		});
		pnlTop.add(btnSendCommand);

		JPanel pnlBottom = new JPanel();
		contentPane.add(pnlBottom, BorderLayout.SOUTH);
		
		scrollPanel1 = new JScrollPane();
		contentPane.add(scrollPanel1, BorderLayout.CENTER);
		
		textAreaConsole = new JTextArea();
		scrollPanel1.setViewportView(textAreaConsole);
		
		loadSerialPorts();
		changeButtons(false);
	}
	
	private void loadSerialPorts() {
		cmbSerialPorts.removeAllItems();
		serialPorts = SerialPort.getCommPorts();
		for (SerialPort port : serialPorts) {
			cmbSerialPorts.addItem(port.getSystemPortName());
		}				
	}
	
	private void add2Console(String message) {
		Date now = new Date();
		textAreaConsole.setText( String.format("%s%s:\t%s\n", textAreaConsole.getText(), stringFormatter.format(now),  message) );
		scrollPanel1.getVerticalScrollBar().setValue(scrollPanel1.getVerticalScrollBar().getMaximum());
	}
	
	private void changeButtons(boolean b) {
		btnConnect.setText(b ? "Disconnect" : "Connect");
		btnRead.setEnabled(b);
		btnSendCommand.setEnabled(b);
	}
	
	private void connectSerialPort() {
		try {
			currentPort = serialPorts[cmbSerialPorts.getSelectedIndex()];
			currentPort.setBaudRate(Integer.parseInt(cmbBauntRates.getSelectedItem().toString()));
			currentPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_SEMI_BLOCKING, 1000, 0);

			if (currentPort.openPort()) {
				add2Console("Bağlantı BAŞARILI!");
				changeButtons(true);
				readSerialPort();
			} else {
				add2Console("Bağlantı BAŞARISIZ!");
				changeButtons(true);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void disconnectSerialPort() {
		if (readThread!=null) {
			readThread.stop();
			readThread=null;
			btnRead.setText("Start Read");
		}
		
		currentPort.closePort();
		add2Console("BAĞLANTI KESİLDİ!");
		changeButtons(false);
	}
	
	private void sendCommand() {
		if (currentPort!=null) {
			if (currentPort.openPort()) {
				String komut = JOptionPane.showInputDialog("Gönderilecek Komutu Giriniz!","led");
				if (komut!=null && komut.trim()!="") {
					byte[] buffer = komut.getBytes();
					currentPort.writeBytes(buffer, buffer.length);	
					add2Console("==>\t"+komut);
				}
			}
		}
	}
	
	private void readSerialPort() {
		if (currentPort!=null) {
			if (currentPort.openPort()) {
				if (readThread != null) {
					readThread.stop();
					readThread = null;
					btnRead.setText("Start Read");
				} else {
					btnRead.setText("Stop Read");
					readThread = new Thread(new readSerialPort());
					readThread.start();
				}
			}
		}
	}

	class readSerialPort extends Thread {
		public void run() {
			Scanner data;
			while (true) {
				data = new Scanner(currentPort.getInputStream());
				while (data.hasNextLine())
					add2Console("<==\t" + data.nextLine());
			}
		}
	}

}
