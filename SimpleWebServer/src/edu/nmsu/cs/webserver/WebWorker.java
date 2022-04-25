package edu.nmsu.cs.webserver;

/**
 * Web worker: an object of this class executes in its own new thread to receive and respond to a
 * single HTTP request. After the constructor the object executes on its "run" method, and leaves
 * when it is done.
 *
 * One WebWorker object is only responsible for one client connection. This code uses Java threads
 * to parallelize the handling of clients: each WebWorker runs in its own thread. This means that
 * you can essentially just think about what is happening on one client at a time, ignoring the fact
 * that the entirety of the webserver execution might be handling other clients, too.
 *
 * This WebWorker class (i.e., an object of this class) is where all the client interaction is done.
 * The "run()" method is the beginning -- think of it as the "main()" for a client interaction. It
 * does three things in a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it writes out some HTML
 * content for the response content. HTTP requests and responses are just lines of text (in a very
 * particular format).
 * 
 * @author Jon Cook, Ph.D.
 *
 **/

 import java.io.File;
import java.io.FileInputStream;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.text.DateFormat;
import java.util.Date;
import java.util.TimeZone;

public class WebWorker implements Runnable
{
	private Socket socket;
	private String imgTypes[] = {".png",".jpg",".gif", ".jpeg", ".ico"};

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s)
	{
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP request and then returns, which
	 * destroys the thread. This method assumes that whoever created the worker created it with a
	 * valid open socket object.
	 **/
	public void run()
	{
		System.err.println("Handling connection...");
		try
		{
			InputStream is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			String filepath = readHTTPRequest(is);
			writeHTTPHeader(os, filepath, "text/html");
			writeContent(os, filepath);
			os.flush();
			socket.close();
		}
		catch (Exception e)
		{
			System.err.println("Output error: " + e);
		}
		System.err.println("Done handling connection.\n");
		return;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private String readHTTPRequest(InputStream is) {
		String filepath = "";
		String line;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		while (true)
		{
			try
			{
				while (!r.ready())
					Thread.sleep(1);

				line = r.readLine();
				System.err.println("Request line: (" + line + ")");

				if(line.length() > 13)
					if(line.substring(0,3).equals("GET")) {
						filepath = line.substring(5, line.indexOf("HTTP/1.1"));
						
					}//end if

				if (line.length() == 0)
					break;

			}
			catch (Exception e)
			{
				System.err.println("Request error: " + e);
				break;
			}
		}
		if(filepath.length() <= 1)
			return "www/index.html";
		
		File tempFile = new File(filepath);
		if(!tempFile.exists())
			return "www/404.html";

		return filepath;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 * @param contentType
	 *          is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, String filepath, String contentType) throws Exception
	{
		//checks if requested file is image type
		if(!filepath.endsWith(".html"))
			for(int i = 0; i < imgTypes.length; i++)
				if(filepath.endsWith(".jpg")) {
					contentType = "image/" + imgTypes[i].substring(1);
					break;
			}//end if

		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));
		
		//writes if file found or not
		if(filepath.equals("404.html"))
		    os.write("HTTP/1.1 404 NOT FOUND\n".getBytes());
		else
		    os.write("HTTP/1.1 200 OK\n".getBytes());

		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Joeys very own server\n".getBytes());
		// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		// os.write("Content-Length: 438\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done after the HTTP
	 * header has been written out.
	 * 
	 * @param os
	 *          is the OutputStream object to write to
	 **/
	private void writeContent(OutputStream os, String filepath) throws Exception {

		// check if .html file then reads in and writes out html files
		String line;
		filepath = filepath.trim(); //removes whitespace in front and back of string
		if(filepath.endsWith(".html")) {
		   BufferedReader br = new BufferedReader(new FileReader(filepath));	
		   while((line = br.readLine()) != null)
		      os.write(line.getBytes());
		   br.close();
		   System.err.println("Website file type: " + filepath);
		   return;
		}//end if

		//checks if image file then reads in and writes out the images file bytes
		else 
			for(String img : imgTypes) {
			    if(filepath.endsWith(img)) {
        			try {
						InputStream is = new FileInputStream(filepath);
            			long fileSize = new File(filepath).length();
            			byte[] allBytes = new byte[(int) fileSize];
 						int bytesRead = is.read(allBytes);
 
            			os.write(allBytes, 0, bytesRead);
						is.close();
						System.err.println("Image file type: " + filepath);
						return;
        				}//end try
						catch (IOException ex) {
            			   ex.printStackTrace();
        				}//end catch
				}//end if
			}//end for

			//if reaches here then the server understood the request but it is not an allowed file type or requsted a folder
			BufferedReader br = new BufferedReader(new FileReader("www/403.html"));	
			while((line = br.readLine()) != null)
			   os.write(line.getBytes());
			br.close();
			System.err.println("Forbidden file: " + filepath);
	}//end write content
} // end class
