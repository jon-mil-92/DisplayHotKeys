package com.dhk.io;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * This class takes the data from the given input stream and sends it to the given output stream.
 * 
 * @author Jonathan Miller
 * @version 1.1.0
 * 
 * @license <a href="https://mit-license.org/">The MIT License</a>
 * @copyright Jonathan Miller 2024
 */
class StreamSync implements Runnable {
	InputStream inputStream;
	OutputStream outputStream;
	
	/**
	 * Constructor for the StreamSync class.
	 * 
	 * @param inputStream - The input stream to read from.
	 * @param outputStream - The output stream to sync to.
	 */
	public StreamSync(InputStream inputStream, OutputStream outputStream) {
		this.inputStream = inputStream;
		this.outputStream = outputStream;
	}
	
	/**
	 * This method takes the data from the given input stream and sends it to the given output stream.
	 */
	public void run() {
		try {
			// Define a buffer to hold the data from the input stream.
			final byte[] buffer = new byte[1024];
			
			// Read the buffer until it is empty and transfer the data to the output stream.
			for (int length = 0; (length = inputStream.read(buffer)) != -1;) {
				outputStream.write(buffer, 0, length);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
}
