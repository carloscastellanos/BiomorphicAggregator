/**
 * 
 */
package cc.biomorphic;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;

import org.htmlparser.NodeFilter;
import org.htmlparser.Parser;
import org.htmlparser.filters.NodeClassFilter;
import org.htmlparser.tags.ImageTag;
import org.htmlparser.util.NodeList;
import org.htmlparser.util.ParserException;
import org.jickr.Flickr;
import org.jickr.FlickrException;
import org.jickr.Photo;
import org.jickr.PhotoSearch;

import com.sun.syndication.feed.module.mediarss.MediaModule;
import com.sun.syndication.feed.module.mediarss.types.Metadata;
import com.sun.syndication.feed.module.mediarss.types.Thumbnail;
import com.sun.syndication.feed.synd.SyndContentImpl;
import com.sun.syndication.feed.synd.SyndEntryImpl;
import com.sun.syndication.feed.synd.SyndFeed;
import com.sun.syndication.io.SyndFeedInput;
import com.sun.syndication.io.XmlReader;
import com.yahoo.search.ContentAnalysisRequest;
import com.yahoo.search.ContentAnalysisResults;
import com.yahoo.search.ImageSearchRequest;
import com.yahoo.search.ImageSearchResult;
import com.yahoo.search.ImageSearchResults;
import com.yahoo.search.NewsSearchRequest;
import com.yahoo.search.NewsSearchResult;
import com.yahoo.search.NewsSearchResults;
import com.yahoo.search.SearchClient;
import com.yahoo.search.SearchException;

/**
 * @author carlos
 *
 */
public class SearchAndParse
{
	private ArrayList<String> searchTerms = null;
	private static final String USER_AGENT = "User-Agent";
	private static final String ROME = "Rome/0.8";
	private static final String yahooAppId = "PqLiq6bV34HrGJHwKttj22edzw5E2pRo_uEOw5_jo_jrGAhSjRjBc7xVMfA-";
	private static final String flickrAppId = "2d829a3fd6cb37d31a256305eabf0975";
	private static final String flickrSharedSecret = "a33383d130f6b9e9";
	private DBStore dbStore = null;
	private static final String dbPath = "data/biomorphic.db";
	private static final String dbDriver = "org.sqlite.JDBC";
	private static final String imgSavePath = "data/downloads/";
	
	/* Contructors */
	public SearchAndParse() {
		//searchTerms = new ArrayList<String>(numSearchTerms);
		dbStore = new DBStore(dbPath, dbDriver);
	}

	public SearchAndParse(ArrayList<String> searchTerms) {
		this.searchTerms = searchTerms;
		dbStore = new DBStore(dbPath, dbDriver);
	}

	public void doSearchAndParse() throws Exception {
		System.out.println("Searching & parsing...");
		
		if(searchTerms == null) {
			throw new Exception("No search terms were specified!");
		}
		
		if(!parseYahooNews()) {
			System.out.println("Error parsing YahooNews");
		}
		
		if(!parseGoogleNews()) {
			System.out.println("Error parsing GoogleNews");
		}
		
		if(!parseFlickr()) {
			System.out.println("Error parsing Flickr");
		}
		
		if(!parseYahooNewsHTML()) {
			System.out.println("Error parsing YahooNewsHTML");
		}
		
		if(!parseYahooAPIImage()) {
			System.out.println("Error parsing YahooAPI (image)");
		}
		
		if(!parseYahooAPINews()) {
			System.out.println("Error parsing YahooAPI (news)");
		}
		
		if(dbStore.isConnected())
			dbStore.closeDB();
	}
	
	public void doSearchAndParse(ArrayList<String> terms) throws Exception {
		this.searchTerms = terms;
		try {
			this.doSearchAndParse();
		} catch(Exception e) {
			throw new Exception(e.getMessage());
		}
	}
	
	private boolean parseGoogleNews() {
		boolean ok = true;
		System.out.print("parsing GoogleNews...");
		
		ArrayList<HashMap> googleNewsResults = new ArrayList<HashMap>(20 * searchTerms.size());
		ListIterator<String> iter = searchTerms.listIterator();
		while(iter.hasNext()) {
			String currTerm = iter.next();
			try {
				// get and parse the feed
				String url = "http://news.google.com/news?hl=en&ned=us&ie=UTF-8&q="+currTerm+"&output=rss";
    			URL feedUrl = new URL(url);
    			URLConnection conn = feedUrl.openConnection();
    			conn.setRequestProperty(USER_AGENT, ROME);
    			SyndFeedInput input = new SyndFeedInput();
    			SyndFeed feed = input.build(new XmlReader(conn));
				// get each entry and store it (google gives you 20 items)
				List entries = feed.getEntries();
				for(int i=0; i<entries.size(); i++) {
					SyndEntryImpl entry = (SyndEntryImpl)entries.get(i);
					List entryContentsList = entry.getContents();
					SyndContentImpl contents = (SyndContentImpl)entryContentsList.get(0);
                	String title = entry.getTitle();
                	String html = contents.getValue();
                	Date pubDate = entry.getPublishedDate();
                	//DateFormat dateFormat = DateFormat.getDateTimeInstance();
                	//String pubDateStr = dateFormat.format(pubDate);
                	Parser parser;
                	NodeFilter filter = new NodeClassFilter(ImageTag.class);
                	NodeList list;
                	ImageTag imgTag = null;
                	try
                	{
                		parser = new Parser(html);
                		list = parser.extractAllNodesThatMatch(filter);
                		imgTag = (ImageTag)list.elementAt(0);
                    
                	}
                	catch (ParserException e)
                	{
                		System.out.println("Goggle HTML Parser Exception");
                		e.printStackTrace();
                		ok = false;
                	}
                	// get the img url (if there is one)
                	if(imgTag != null) {
                		int startidx = imgTag.getImageURL().lastIndexOf("imgurl=") + "imgurl=".length();
                		String imgurl = "http://" + imgTag.getImageURL().substring(startidx);
                		String filename = getFileNameFromUrl(imgurl).toLowerCase();
            			// rename file if it already exists
            			if(fileExists(imgSavePath+filename))
            				filename = String.valueOf(Math.random()).substring(2) + filename;
	                	// if the file was downloaded, add it to the hashmap/db
	                	if(FileDownload.download(imgurl, imgSavePath, filename)) {
	                		HashMap<String, Object> googleHash = new HashMap<String, Object>(5);
	                		googleHash.put("imageUrl", imgurl);
	                		googleHash.put("title", title);
	                		googleHash.put("searchTerm", currTerm);
	                		googleHash.put("fileName", filename);
	                		googleHash.put("pubDate", pubDate);
	                		googleNewsResults.add(new HashMap<String, Object>(googleHash));
	                		System.out.println(title + " " + imgurl);
	                	}
                	}
				}
			} catch(Exception ex) {
				ex.printStackTrace();
    			System.out.println("Google ERROR: "+ex.getMessage());
    			ok = false;
			}
			
		}
    	// store in the database
    	dbStore.store(googleNewsResults);
		return ok;
	}
	
	private boolean parseYahooNews() {
		boolean ok = true;
		System.out.print("parsing YahooNews...");
		
		// arraylist (with HashMap) for storeing results
		ArrayList<HashMap> yahooNewsResults = new ArrayList<HashMap>(10 * searchTerms.size());
		ListIterator<String> iter = searchTerms.listIterator();
        while(iter.hasNext()) {
        	String currTerm = iter.next();
        	try {
        		// get and parse the feed
				String url = "http://news.search.yahoo.com/news/rss?ei=UTF-8&p="+currTerm+"&c=news_photos&eo=UTF-8";
        		URL feedUrl = new URL(url);
        		URLConnection conn = feedUrl.openConnection();
        		conn.setRequestProperty(USER_AGENT, ROME);
				SyndFeedInput input = new SyndFeedInput();
				SyndFeed feed = input.build(new XmlReader(conn));
					
				// get each entry and store it (yahoo gives you 10 items)
				List entries = feed.getEntries();
				for(int j = 0; j < entries.size(); j++) {
					SyndEntryImpl entry = (SyndEntryImpl)entries.get(j);
					MediaModule mediamodule = (MediaModule)entry.getModule(MediaModule.URI);
					Metadata metadata = mediamodule.getMetadata();
					Thumbnail[] thumbs = metadata.getThumbnail();
					URL thumburl = thumbs[0].getUrl();
					String title = entry.getTitle();
					Date pubDate = entry.getPublishedDate();
					//DateFormat dateFormat = DateFormat.getDateTimeInstance();
					//String pubDateStr = dateFormat.format(pubDate);
					String filename = getFileNameFromUrl(thumburl.toString()).toLowerCase();
        			// rename file if it already exists
        			if(fileExists(imgSavePath+filename))
        				filename = String.valueOf(Math.random()).substring(2) + filename;
            		// if the file was downloaded, add it to the hashmap/db
            		if(FileDownload.download(thumburl.toString(), imgSavePath, filename)) {
            			HashMap<String, Object> yahooHash = new HashMap<String, Object>(5);
            			yahooHash.put("imageUrl", thumburl.toString());
            			yahooHash.put("title", title);
            			yahooHash.put("searchTerm", currTerm);
            			yahooHash.put("fileName", filename);
            			yahooHash.put("pubDate", pubDate);
            			yahooNewsResults.add(new HashMap<String, Object>(yahooHash));
            			System.out.println(title + " " + thumburl.toString());
            		}
				}
        	}
        	catch (Exception ex) {
        		ex.printStackTrace();
        		System.out.println("YahooNews ERROR: "+ex.getMessage());
        		ok = false;
        	}
        }

    	// store in the database
    	dbStore.store(yahooNewsResults);
		return ok;
	}
	
	private boolean parseYahooNewsHTML() {
		boolean ok = true;
		System.out.print("parsing YahooNewsHTML...");
		
		ArrayList<HashMap> yahooNewsResults = new ArrayList<HashMap>(90 * searchTerms.size());
		ListIterator<String> iter = searchTerms.listIterator();
		while(iter.hasNext()) {
			String currTerm = iter.next();
			try {
				// get and parse the page (100 images)
    			String url ="http://news.search.yahoo.com/news/search?p="+currTerm+"&c=news_photos&n=100";
                try
                {
                    Parser parser = new Parser();
                    Parser.getConnectionManager().setRedirectionProcessingEnabled (true);
                    Parser.getConnectionManager().setCookieProcessingEnabled (true);
                    parser.setURL(url);
                    NodeFilter filter = new NodeClassFilter(ImageTag.class);
                    NodeList list;
                    ImageTag imgTag = null;
                	list = parser.parse(filter);
                	// start at 11 (since the rss method gets the first 10)
                	for(int i=10; i<list.size(); i++) {
                		imgTag = (ImageTag)list.elementAt(i);
                		// get only the jpgs (just the photos)
                		if(imgTag.getAttribute("src").lastIndexOf(".jpg") != -1) {
                			String imgurl = imgTag.getImageURL();
                			//int startidx = img.lastIndexOf("imgurl=") + "imgurl=".length();
                			//String imgurl = img.substring(startidx);
                			String filename = getFileNameFromUrl(imgurl).toLowerCase();
                			// rename file if it already exists
                			if(fileExists(imgSavePath+filename))
                				filename = String.valueOf(Math.random()).substring(2) + filename;
                    		if(FileDownload.download(imgurl, imgSavePath, filename)) {
                    			HashMap<String, Object> yahooHash = new HashMap<String, Object>(5);
                    			yahooHash.put("imageUrl", imgurl);
                    			yahooHash.put("title", currTerm);
                    			yahooHash.put("searchTerm", currTerm);
                    			yahooHash.put("fileName", filename);
                    			yahooHash.put("pubDate", new Date());
                    			yahooNewsResults.add(new HashMap<String, Object>(yahooHash));
                    			System.out.println(currTerm + " " + imgurl);
                    		}
                		}
                	}
                } catch (ParserException e) {
            		System.out.println("Yahoo HTML Parser Exception");
            		e.printStackTrace();
            		ok = false;
                }
			} catch(Exception ex) {
				ex.printStackTrace();
				System.out.println("YahooNewsHTML ERROR: "+ex.getMessage());
				ok = false;
			}
		}
			
    	// store in the database
    	dbStore.store(yahooNewsResults);
		return ok;
	}
	
	private boolean parseYahooAPINews() {
		boolean ok = true;
		System.out.print("parsing YahooAPI (news)...");
		
		ArrayList<HashMap> yahooNewsResults = new ArrayList<HashMap>();
		ListIterator<String> iter = searchTerms.listIterator();
		// Create the search client. Pass it the application ID.
		SearchClient client = new SearchClient(yahooAppId);
		while(iter.hasNext()) {
			String currTerm = iter.next();
	        // Create a news search request.
	        NewsSearchRequest newsRequest = new NewsSearchRequest(currTerm);
	        // the max allowed number of results is 50
	        newsRequest.setResults(50);
	        //newsRequest.setSort("date");
	        try {
	        	// Execute the search.
	            NewsSearchResults newsResults = client.newsSearch(newsRequest);
	            // Iterate over the results.
	            for (int i = 0; i < newsResults.listResults().length; i++) {
	                NewsSearchResult newsResult = newsResults.listResults()[i];
	                // get the news summary
	                String contextStr = newsResult.getSummary();
	                //String title = newsResult.getTitle();
	                // create a content analysis search based on the news summary
	                ContentAnalysisRequest contentRequest = new ContentAnalysisRequest(contextStr);
	                contentRequest.setQuery(currTerm);
	                try {
	                	// Execute the context search.
	                	ContentAnalysisResults contentResults = client.termExtractionSearch(contentRequest);
	                	// Iterate over the results
	                	for (int j = 0; j < contentResults.getExtractedTerms().length; j++) {
	                		String extractedTerm = contentResults.getExtractedTerms()[j];                		
	                		// replace any spaces with "+"
	                		if(extractedTerm.indexOf(" ") != -1) {
	                			String[] split = extractedTerm.split("\\s");
	                			StringBuffer buf = new StringBuffer();
	                			 for (int x=0; x<split.length; x++) {
	                				 if(x < split.length-1) {
	                					 buf.append(split[x] + "+");
	                				 } else {
	                					 buf.append(split[x]);
	                				 }
	                			 }
	                			 extractedTerm = buf.toString();
	                		}
	                	     
	                		// now do a news photo search for these terms
	                		HashMap<String, Object> yahooHash = null;
	                    	try {
	                    		// get and parse the feed
	                    		System.out.println("Extracted term:"+extractedTerm);
	            				String url = "http://news.search.yahoo.com/news/rss?ei=UTF-8&p="+extractedTerm+"&c=news_photos&eo=UTF-8";
	                    		URL feedUrl = new URL(url);
	                    		URLConnection conn = feedUrl.openConnection();
	                    		conn.setRequestProperty(USER_AGENT, ROME);
	            				SyndFeedInput input = new SyndFeedInput();
	            				SyndFeed feed = input.build(new XmlReader(conn));
	            					
	            				// get each entry and store it (yahoo gives you 10 items)
	            				List entries = feed.getEntries();
	            				for(int k = 0; k < entries.size(); k++) {
	            					SyndEntryImpl entry = (SyndEntryImpl)entries.get(k);
	            					MediaModule mediamodule = (MediaModule)entry.getModule(MediaModule.URI);
	            					Metadata metadata = mediamodule.getMetadata();
	            					Thumbnail[] thumbs = metadata.getThumbnail();
	            					URL thumburl = thumbs[0].getUrl();
	            					String title = entry.getTitle();
	            					Date pubDate = entry.getPublishedDate();
	            					//DateFormat dateFormat = DateFormat.getDateTimeInstance();
	            					//String pubDateStr = dateFormat.format(pubDate);
	            					String filename = getFileNameFromUrl(thumburl.toString()).toLowerCase();
	                    			// rename file if it already exists
	                    			if(fileExists(imgSavePath+filename))
	                    				filename = String.valueOf(Math.random()).substring(2) + filename;
	            					if(FileDownload.download(thumburl.toString(), imgSavePath, filename)) {
	            						yahooHash = new HashMap<String, Object>(5);
	            						yahooHash.put("imageUrl", thumburl.toString());
	            						yahooHash.put("title", title);
	            						yahooHash.put("searchTerm", currTerm);
	            						yahooHash.put("fileName", filename);
	            						yahooHash.put("pubDate", pubDate);
	            						yahooNewsResults.add(new HashMap<String, Object>(yahooHash));
	            						System.out.println(title + " " + thumburl.toString());
	            					}
	            				}
	                    	}
	                    	catch (Exception ex) {
	                    		System.out.println("YahooAPI ERROR: "+ex.getMessage());
	                    		ex.printStackTrace();
	                    		ok = false;
	                    		// Yahoo imposes rate limiting so let's store
	                    		// what we have and return from this method
	                    		// An error here probably means we're hitting them too hard
	                    		if(yahooHash != null)
	                    			dbStore.store(yahooNewsResults);
	                    		return ok;
	                    	}
	                		
	                	}
	                	
	                } catch(IOException ioex) {
	                	System.out.println("YahooAPI IO Error! (ContentAnalysisRequest)" + ioex.getMessage());
	                	ok = false;
	                	return ok;
	                } catch(SearchException sex) {
	                	System.out.println("YahooAPI Search Error! (ContentAnalysisRequest)" + sex.getMessage());
	                	ok = false;
	                	return ok;
	                }
	                
	            }
	            
	        } catch(IOException ioe) {
	        	System.out.println("YahooAPI IO Error! (NewsSearchRequest)" + ioe.getMessage());
	        	ok = false;
	        } catch(SearchException se) {
	        	System.out.println("YahooAPI Search Error! (NewsSearchRequest) " + se.getMessage());
	        	ok = false;
	        }
		}
    	// store in the database
    	dbStore.store(yahooNewsResults);
		return ok;
	}
	
	private boolean parseYahooAPIImage() {
		boolean ok = true;
		System.out.print("parsing YahooAPI (image)...");
		
		ArrayList<HashMap> yahooImageResults = new ArrayList<HashMap>(150);
		ListIterator<String> iter = searchTerms.listIterator();
		// Create the search client. Pass it the application ID.
		SearchClient client = new SearchClient(yahooAppId);
		while(iter.hasNext()) {
			String currTerm = iter.next();
	        // Create a news search request.
	        ImageSearchRequest imageRequest = new ImageSearchRequest(currTerm);
	        // the max allowed number of results is 50
	        imageRequest.setResults(50);
	        imageRequest.setAdultOk(true);
	        try {
	        	// Execute the search.
	            ImageSearchResults imageResults = client.imageSearch(imageRequest);
	            // Iterate over the results.
	            for (int i = 0; i < imageResults.listResults().length; i++) {
	                ImageSearchResult imageResult = imageResults.listResults()[i];
	                // get the image data
	                String title = imageResult.getTitle();
	                String imgurl = imageResult.getUrl();
	                String filename = getFileNameFromUrl(imgurl).toLowerCase();
        			// rename file if it already exists
        			if(fileExists(imgSavePath+filename))
        				filename = String.valueOf(Math.random()).substring(2) + filename;
	                if(FileDownload.download(imgurl, imgSavePath, filename)) {
	                	HashMap<String, Object> yahooHash = new HashMap<String, Object>(5);
	                	yahooHash.put("imageUrl", imgurl);
	                	yahooHash.put("title", title);
	                	yahooHash.put("searchTerm", currTerm);
	                	yahooHash.put("fileName", filename);
	                	yahooHash.put("pubDate", new Date());
	                	yahooImageResults.add(new HashMap<String, Object>(yahooHash));
	                	System.out.println(currTerm + " " + imgurl);
	                }
	            }

	        } catch(IOException ioe) {
	        	System.out.println("YahooAPI IO Error! (ImageSearchRequest)" + ioe.getMessage());
	        	ok = false;
	        } catch(SearchException se) {
	        	System.out.println("YahooAPI Search Error! (ImageSearchRequest) " + se.getMessage());
	        	ok = false;
	        }
		}
    	// store in the database
    	dbStore.store(yahooImageResults);
		return ok;		
	}
	
	private boolean parseFlickr() {
		boolean ok = true;
		System.out.print("parsing Flickr...");
		
		ArrayList<HashMap> flickrResults = new ArrayList<HashMap>(100 * searchTerms.size());
		ListIterator<String> iter = searchTerms.listIterator();
		Flickr.setApiKey(flickrAppId);
		Flickr.setSharedSecret(flickrSharedSecret);
        while(iter.hasNext()) {
        	String currTerm = iter.next();
        	PhotoSearch search = new PhotoSearch();
        	search.setSearchText(currTerm);
        	//search.setTags(currTerm);
        	HashMap<String, Object> flickrHash = null;
        	try {
        		List<Photo> photos = Photo.search(search);
        		ListIterator<Photo> piter = photos.listIterator();
        		int count = 0;
        		// get the 100 most recent photos
        		while (piter.hasNext() && count < 100) {
        			Photo photo = piter.next();
        			URL url = photo.getStaticURL();
        			String title = photo.getTitle();
        			Date dateUploaded = photo.getDateUploaded();
                	//DateFormat dateFormat = DateFormat.getDateTimeInstance();
                	//String pubDateStr = dateFormat.format(dateUploaded);
        			/*
            		List<Tag> tags = photo.getTags();
            		String toptag = "";
            		if(tags.size() > 0) {
            			toptag = tags.get(0).getRawText();
            		}
        			 */
        			count++;
        			
        			String filename = getFileNameFromUrl(url.toString()).toLowerCase(); // convert to lowercase
        			// rename file if it already exists
        			if(fileExists(imgSavePath+filename))
        				filename = String.valueOf(Math.random()).substring(2) + filename;
        			if(FileDownload.download(url.toString(), imgSavePath, filename)) {
        				flickrHash = new HashMap<String, Object>(5);
        				flickrHash.put("imageUrl", url.toString());
        				flickrHash.put("title", title);
        				flickrHash.put("searchTerm", currTerm);
        				flickrHash.put("fileName", filename);
        				flickrHash.put("pubDate", dateUploaded);
        				flickrResults.add(new HashMap<String, Object>(flickrHash));
        				System.out.println("Title: " + title + " Photo URL: " + url.toString());
        			}
        		}
        	} catch(FlickrException fe) {
        		System.out.println("Flickr Error! " + fe.getMessage() + " " + fe.getCode());
        		fe.printStackTrace();
        		// Flickr may impose rate limiting so let's store
        		// what we have and return from this method
        		if(flickrHash != null)
        			dbStore.store(flickrResults);
        		ok = false;
        		return ok;
        	}
            	
        }
    	// store in the database
    	dbStore.store(flickrResults);
		return ok;	
	}
	
	private static String getFileNameFromUrl(String url) {
		int lastSlashIndex = url.lastIndexOf('/');
		String localFilePath = null;
		if (lastSlashIndex >= 0 && lastSlashIndex < url.length() - 1) {
			localFilePath = url.substring(lastSlashIndex + 1);
		} else {
			System.err.println("Could not figure out local file name for " + url);
		}
		return localFilePath;
	}
	
	/*
	 *  Convenience method to determine if the file already exists
	 */
	private static boolean fileExists(String filePath) {
		boolean exists = false;
		File file = new File(filePath);
		if(file.exists())
			exists = true;
		return exists;
	}

}
