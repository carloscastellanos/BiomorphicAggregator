/**
 * 
 */
package cc.biomorphic;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.ListIterator;
import java.sql.SQLException;

/**
 * @author carlos
 *
 */
class DBStore
{
	private String dbPath = null;
	private String driver = null;
	private Connection connection = null;
	private boolean connected = false;
	private PreparedStatement statement = null;
	private static final String psql = "INSERT INTO searchfeeds VALUES (NULL, ?, ?, ?, ?, ?)";
	
	DBStore(String dbPath, String driver) {
		this.dbPath = dbPath;
		this.driver = driver;
		getConnection();
	}
	
	void setDriver(String dr) {
		this.driver = dr;
	}
	
	void setDBFilePath(String path) {
		this.dbPath = path;
	}
	
	// Create a PreparedStatement object for the database connection.
	private void createPreparedStatement() {
		if(connection != null) {
			try {
				statement = connection.prepareStatement(psql);
			} catch(SQLException sqle) {
				System.out.println(sqle.getMessage());
				sqle.printStackTrace();
			}
		}
	}
	
	private void getConnection() {
		try {
			// JDBC Driver to Use
			Class.forName(driver);
    
			// Create Connection Object to SQLite Database
			// If you want to only create a database in memory, exclude the +fileName
			connection = DriverManager.getConnection("jdbc:sqlite:"+dbPath);
			connected = true;
		} catch(ClassNotFoundException cnfe) {
			// Print some generic debug info
			System.out.println(cnfe.getMessage());
			connected = false;
		} catch(SQLException s) {
			// Print some generic debug info
			System.out.println(s.getMessage());
			s.printStackTrace();
			connected = false;
		}
	}
	
	public boolean isConnected() {
		return connected;
	}
	
	public void openDB() {
		if(!connected)
			getConnection();
	}
	
	public void closeDB() {
		// Close the connection
		if(connected) {
			try {
				connection.close();
				connected = false;
			} catch(SQLException sex) {
				System.out.println(sex.getMessage() + "\n" + "Error closing DB connection!");
				sex.printStackTrace();
			}
		}
	}
	
	boolean store(ArrayList<HashMap> elems) {
		boolean ok = true;
		ListIterator<HashMap> iter = elems.listIterator();
		while(iter.hasNext()) {
			HashMap hm = iter.next();
			try {
				if(connection != null) {
					if(statement == null)
						createPreparedStatement();
						
					statement.setString(1, (String)hm.get("imageUrl"));
					statement.setString(2, (String)hm.get("title"));
					statement.setString(3, (String)hm.get("searchTerm"));
					statement.setString(4, (String)hm.get("fileName"));
					//String dateStr = (String)hm.get("pubDate");
					//Timestamp timestamp = Timestamp.valueOf(dateStr);
					//statement.setTimestamp(4, timestamp);
					// execute the insert/update
					Date d = (Date) (hm.get("pubDate"));
					long t = d.getTime();
					Timestamp ts = new Timestamp(t);
					statement.setTimestamp(5, ts);
					statement.executeUpdate();
				}
			} catch (SQLException e) {
				ok = false;
				// Print some generic debug info
				System.out.println(e.getMessage());
				e.printStackTrace();
			}
		}
		return ok;
	}
		
}
