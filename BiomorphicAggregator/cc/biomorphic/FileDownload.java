/**
 * 
 */
package cc.biomorphic;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;

/**
 * @author carlos
 *
 */

/*
 * Based on a command line program by Marco Schmidt
 * to download data from URLs and save it to local files
 */
public class FileDownload
{
	public static boolean download(String address, String path, String localFileName) {
		OutputStream out = null;
		URLConnection conn = null;
		InputStream  in = null;
		boolean ok = true;
		try {
			URL url = new URL(address);
			out = new BufferedOutputStream(
				new FileOutputStream(path+localFileName));
			conn = url.openConnection();
			in = conn.getInputStream();
			byte[] buffer = new byte[1024];
			int numRead;
			long numWritten = 0;
			while ((numRead = in.read(buffer)) != -1) {
				out.write(buffer, 0, numRead);
				numWritten += numRead;
			}
			System.out.println(localFileName + "\t" + numWritten + " bytes");
		} catch (Exception exception) {
			ok = false;
			exception.printStackTrace();
		} finally {
			try {
				if (in != null) {
					in.close();
				}
				if (out != null) {
					out.close();
				}
			} catch (IOException ioe) {
			}
		}
		return ok;
	}

	public static boolean download(String address, String folderPath) {
		boolean ok = true;
		int lastSlashIndex = address.lastIndexOf('/');
		if (lastSlashIndex >= 0 &&
		    lastSlashIndex < address.length() - 1) {
			ok = download(address, folderPath, address.substring(lastSlashIndex + 1));
		} else {
			System.err.println("Could not figure out local file name for " + address);
			ok = false;
		}
		return ok;
	}

}
