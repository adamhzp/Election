import javax.swing.JFrame;
import java.awt.Dimension;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.JButton;
import javax.swing.JTable;
import javax.swing.JLabel;
import java.awt.Color;
import javax.swing.JList;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import javax.swing.JComboBox;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Component;
import java.io.*;


public class vote{

	public static void main(String[] args)
	{
		if(args.length!=2)
		{
			System.out.println("USAGE: vote <CLA_IP> <CTF_IP>");
			return;
		}
		voteUI ui = new voteUI(args[0], args[1]); 
		ui.setVisible(true);
	}

}

class voteUI extends JFrame{

	private JTextField usernameText;
	private JTextField passText;
	private JButton login;
	private voteUI self = null;
	private String choices = null;
	private String claIP = null;
	private String ctfIP = null;

		public voteUI(String cla, String ctf)
		{
			self = this;
			this.claIP = cla;
			this.ctfIP = ctf;


			setResizable(false);
			getContentPane().setSize(new Dimension(300, 230));
			setSize(new Dimension(300, 230));
			setTitle("VOTE SYSTEM");
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			//setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			getContentPane().setLayout(null);


			usernameText = new JTextField();
			usernameText.setAlignmentX(Component.CENTER_ALIGNMENT);
			usernameText.setBounds(160, 60, 80, 33);
			getContentPane().add(usernameText);


			passText = new JTextField();
			passText.setAlignmentX(Component.CENTER_ALIGNMENT);

			passText.setBounds(160, 100, 80, 33);
			getContentPane().add(passText);


			JLabel userLabel = new JLabel("Username:");
			userLabel.setHorizontalAlignment(SwingConstants.CENTER);
			userLabel.setBounds(80, 60, 80, 33);

			getContentPane().add(userLabel);

			JLabel passLabel = new JLabel("Password:");
			passLabel.setHorizontalAlignment(SwingConstants.CENTER);
			passLabel.setBounds(80, 100, 80, 33);
			getContentPane().add(passLabel);

			login = new JButton("LOGIN");
			login.setHorizontalAlignment(SwingConstants.CENTER);
			login.setBounds(100, 140, 80, 33);
			login.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(login.isSelected()){
					if(usernameText.getText().isEmpty() || passText.getText().isEmpty())
					{
						return;
					}else{
						String user = usernameText.getText();
						String pass = passText.getText();
						votes v = new votes(user, pass, self,claIP,ctfIP);
						v.run();
					}
				}else
				{
					if(usernameText.getText().isEmpty() || passText.getText().isEmpty())
					{
						return;
					}else{
						String user = usernameText.getText();
						String pass = passText.getText();
						votes v = new votes(user, pass, self,claIP,ctfIP);
						v.run();
					}
				}
			}
			});
			getContentPane().add(login);
		}


		public void printVoteContent(ObjectOutputStream output, ObjectInputStream input, votes v,String candidates, int lo, int hi)
		{
			voteContentUI content = new voteContentUI(output,input,v,candidates, lo, hi);
			content.setVisible(true);
		}

		public void printMessage(String msg)
		{
			JOptionPane.showMessageDialog(null, msg);
		}
		public void printMessageWithClose(String msg)
		{
			msgFrame f = new msgFrame(msg);
			f.setVisible(true);
		}

	class msgFrame extends JFrame{
		private msgFrame self;
		public msgFrame(String msg)
		{
			self = this;
			setResizable(false);
			getContentPane().setSize(new Dimension(300, 200));
			setSize(new Dimension(300, 200));
			setTitle("Alert");
			this.getContentPane().setLayout(null);
			setDefaultCloseOperation(EXIT_ON_CLOSE);

			JTextArea textArea = new JTextArea (msg);
			textArea.setEditable(false);
			textArea.setBackground(Color.LIGHT_GRAY);
			JScrollPane scroll = new JScrollPane (textArea, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			scroll.getHorizontalScrollBar();
			scroll.getVerticalScrollBar();
			scroll.setBounds(0,0,300,300);
			this.add(scroll);
		}

	}


	class voteContentUI extends JFrame{
		private JTextField choiceText;
		private String candidates;
		private int lo;
		private int hi;
		private ObjectOutputStream output;
		private ObjectInputStream input;
		private votes v;

		public voteContentUI(ObjectOutputStream out,ObjectInputStream in, votes vo,String candidates, int lo, int hi)
		{
			this.candidates = candidates;
			this.hi = hi;
			this.lo = lo;
			this.v = vo;
			this.output = out;
			this.input = in;

			setResizable(false);
			getContentPane().setSize(new Dimension(500, 400));
			setSize(new Dimension(500, 400));
			setTitle("VOTE SYSTEM");
			setDefaultCloseOperation(EXIT_ON_CLOSE);
			//setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
			this.getContentPane().setLayout(null);

			JTextArea log = new JTextArea(candidates);
			log.setEditable(false);
			log.setBackground(Color.LIGHT_GRAY);
			log.setVisible(true);
        	log.setLineWrap(true);
        	log.setBounds(20,20,460,200);
        	log.setWrapStyleWord(true);
			this.getContentPane().add(log);

			JLabel msg = new JLabel("Please input your vote(input an integer from "+lo+" to "+hi+"):");
			msg.setBounds(20,225,460,30);
			this.getContentPane().add(msg);

			choiceText = new JTextField();
			choiceText.setBounds(20,270,100, 30);
			this.getContentPane().add(choiceText);

			JButton voteBotton = new JButton("VOTE!");
			voteBotton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent arg0) {
				if(login.isSelected()){
					if(choiceText.getText().isEmpty())
					{
						return;
					}else{
						int i = getvote();
						if(i != -1)
						{
							v.sendVote(output,input,i);
						}
					}
				}else
				{
					if(choiceText.getText().isEmpty())
					{
						return;
					}else{
						int i = getvote();
						if(i != -1)
						{
							v.sendVote(output,input,i);
						}
					}
				}
			}
			});
			voteBotton.setBounds(140,270,100,30);
			this.getContentPane().add(voteBotton);

		}
		public int getvote()
		{
				try {
					int i =  Integer.parseInt(choiceText.getText());
					if(i>=lo && i<=hi){
						return i;
					}else{
						printMessage("Please input an integer in range "+lo+"~"+hi);
						return -1;
					}
				} catch (NumberFormatException e) {
					printMessage("That is not an integer. ");
				} catch (Exception e) {
				// should never happen
				}
				return -1;
			
		}
	}

}