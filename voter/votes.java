import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.*;
import java.net.*;
import java.math.BigInteger;
import java.util.*;



public class votes{
	private String username;
	private String password;
	private BigInteger id = null;

	private String ctfIP ;
      private String claIP ;
	private BigInteger nounce = null;
	private BigInteger validation = null;
      private voteUI ui = null;

	public votes(String username,String password, voteUI ui, String claIP, String ctfIP)
	{
		System.setProperty("javax.net.ssl.trustStore","truststore.ts");
            System.setProperty("javax.net.ssl.trustStorePassword", "12345678");
            this.username = username;
            this.password = password;
            this.ui = ui;
            this.claIP = claIP;
            this.ctfIP = ctfIP;
	}

	public void run()
	{
            if(login()){
                  if(this.ui != null) ui.setVisible(false);     
                  vote();
            }
	}

	public boolean login()
	{
		try{
			SSLSocketFactory sf = (SSLSocketFactory) SSLSocketFactory.getDefault();
			SSLSocket cla_socket = (SSLSocket)sf.createSocket();
			String[] enabledCipherSuites = { "SSL_RSA_WITH_RC4_128_MD5" };
			cla_socket.setEnabledCipherSuites(enabledCipherSuites);
			cla_socket.connect(new InetSocketAddress(claIP, 6868));
                  System.out.println("Try to connect to CLA");
			ObjectOutputStream output = new ObjectOutputStream(cla_socket.getOutputStream());
            ObjectInputStream input = new ObjectInputStream(cla_socket.getInputStream());

            //send log in request
		ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            ObjectOutputStream stream = new ObjectOutputStream(bytes);
            //generate a nounce here
            nounce = new BigInteger(64, new Random());
            stream.writeUTF("LOGIN");
            stream.writeObject(nounce);
            output.writeObject(bytes.toByteArray());
            bytes.close();
            stream.close();

            //send out password and username
            ByteArrayOutputStream loginBytes = new ByteArrayOutputStream();
            DataOutputStream loginStream = new DataOutputStream(loginBytes);
            loginStream.writeUTF(this.username);
            loginStream.writeUTF(this.password);
            output.writeObject(loginBytes.toByteArray());
            loginBytes.close();
            stream.close();

            byte[] response = (byte[])input.readObject();

            ObjectInputStream responseStream = new ObjectInputStream(new ByteArrayInputStream(response));

            BigInteger check = (BigInteger)responseStream.readObject();
            String res = responseStream.readUTF();

            if(nounce.equals(check)){
            	if(res.equalsIgnoreCase("Failed"))
            	{
            		System.out.println("Failed to loggin!! Check your username and password!!");
                        if(this.ui!=null)
                        {
                              ui.printMessageWithClose("Failed to loggin!! Check your username and password!!");
                        }
                        return false;
            	}else if(res.equalsIgnoreCase("logged")){
            		System.out.println("Successfully logged in!");
            		validation = (BigInteger) responseStream.readObject();
            	}else if(res.equalsIgnoreCase("noctf")){
                        System.out.println("The voting system is not set up yet(CTF is off).");
                        if(this.ui!=null)
                        {
                              ui.printMessageWithClose("The voting system is not set up yet(CTF is off).");
                        }
                        return false;
                  }
            }else{
            	System.out.println("Potential man in the middle attack!");
                  return false;
            }
            output.close();
            input.close();

            id = getID();
           	System.out.println("Your vote Id: "+id+"\n");
            return true;

		}catch(Exception e)
		{
                  System.out.println("CLA is not set up yet!");
                  return false;
		}
	}



      public BigInteger getID()
      {
            try{
                  ObjectInputStream stream = new ObjectInputStream (new FileInputStream(this.username+".id"));
                  BigInteger id = (BigInteger)stream.readObject();
                  stream.close();
                  return id;
            }catch(Exception e)
            {
                  try{
                  System.out.println("Generating new id:");
                  BigInteger id = new BigInteger(64,new Random());
                  ObjectOutputStream stream = new ObjectOutputStream (new FileOutputStream(this.username+".id", false));
                  stream.writeObject(id);
                  stream.close();
                  return id;
                  }catch(Exception ex){return null;}
            }
      }

	public void vote()
	{

        try{
			SSLSocketFactory ssf = (SSLSocketFactory) SSLSocketFactory.getDefault();
			SSLSocket socket = (SSLSocket)ssf.createSocket();
			String[] enabledCipherSuites = { "SSL_RSA_WITH_RC4_128_MD5" };
			socket.setEnabledCipherSuites(enabledCipherSuites);
			socket.connect(new InetSocketAddress(ctfIP, 7000));

			ObjectOutputStream output = new ObjectOutputStream(socket.getOutputStream());
                  ObjectInputStream input = new ObjectInputStream(socket.getInputStream());

                  ByteArrayOutputStream reqBytes = new ByteArrayOutputStream();
                  ObjectOutputStream reqStream = new ObjectOutputStream(reqBytes);
                  reqStream.writeUTF("VOTE");
                  reqStream.writeObject(this.validation);
                  output.writeObject(reqBytes.toByteArray());

                  reqBytes.close();
                  reqStream.close();

                  byte[] res = (byte[]) input.readObject();
                  ObjectInputStream responseStream = new ObjectInputStream(new ByteArrayInputStream(res));
                  String response = responseStream.readUTF();
            
                  if(response.equalsIgnoreCase("SUCCESS")){
            	     String choices = responseStream.readUTF();
            	     int lo = responseStream.readInt();
            	     int hi = responseStream.readInt();
                        int i = 0;
                        if(this.ui == null){
            	           System.out.println(choices);
            	           i = getvote(lo,hi);
                              sendVote(output,input,i);
                        }else{
                              ui.printVoteContent(output,input,this,choices, lo, hi);
                        }
                  }else if(response.equalsIgnoreCase("voted"))
                  {
            	     System.out.println("You have voted. Please log in to see the result when the election is finished");
                        if(this.ui!=null){
                              ui.printMessageWithClose("You have voted. Please log in to see the result when the election is finished");
                        }
                  }else if(response.equalsIgnoreCase("result"))
                  {
                        System.out.println("Vote is done!");
                        String result =responseStream.readUTF();
                        result =  result +"\n\nYour ID is "+this.id;
                        System.out.println(result);
                        if(this.ui!=null)
                        {
                              ui.printMessageWithClose(result);
                        }
                  }

		}catch(Exception e)
		{
		}
	}

      public void sendVote(ObjectOutputStream output,ObjectInputStream input , int i )
      {
            try{
                  ByteArrayOutputStream choiceByte = new ByteArrayOutputStream();
                  ObjectOutputStream choiceStream = new ObjectOutputStream(choiceByte);
                  choiceStream.writeUTF("Choice");
                  choiceStream.writeInt(i);
                  choiceStream.writeObject(validation);
                  choiceStream.writeObject(id);
                  output.writeObject(choiceByte.toByteArray());

                  byte[] response = (byte[]) input.readObject();
                  ObjectInputStream responseStream = new ObjectInputStream(new ByteArrayInputStream(response));
                  String res = responseStream.readUTF();
                  if(res.equalsIgnoreCase("votesuccess"))
                  {
                        System.out.println("Vote Successfully");
                        if(this.ui!=null)
                        {
                              ui.printMessageWithClose("Vote Successfully");
                        }
                  }

            }catch(Exception e){

            }
      }


	public int getvote(int lo, int hi)
	{
		BufferedReader kb = new BufferedReader(new InputStreamReader(System.in));
		while (true) {
			try {
				String s = kb.readLine();
				int i =  Integer.parseInt(s);
				if(i>=lo && i<=hi){
					return i;
				}else{
					System.out.println("Please input an integer in range "+lo+"~"+hi);
				}
			} catch (NumberFormatException e) {
				System.out.print("That is not an integer.  Enter again: ");
			} catch (IOException e) {
				// should never happen
			}
		}
	}

}