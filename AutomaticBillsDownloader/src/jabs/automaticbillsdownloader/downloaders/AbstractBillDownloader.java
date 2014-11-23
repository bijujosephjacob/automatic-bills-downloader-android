package jabs.automaticbillsdownloader.downloaders;

import java.io.InputStream;
import java.util.HashMap;

import org.apache.http.client.HttpClient;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

public abstract class AbstractBillDownloader {
	protected HttpClient httpclient;
	protected HttpContext localContext;
	private String taskName, dropboxFolder;
	
	protected AbstractBillDownloader(String taskName, String dropboxFolder) {
		this.taskName = taskName;
		this.dropboxFolder = dropboxFolder;
		
		this.httpclient = new DefaultHttpClient();
		
		this.localContext = new BasicHttpContext();
		this.localContext.setAttribute(ClientContext.COOKIE_STORE, new BasicCookieStore());
	}
	
	public String getName() {
		return taskName;
	}
	
	public String getDropboxFolder() {
		return dropboxFolder;
	}
	
	public abstract boolean login();

	public abstract HashMap<String, String[]> getAvailableBills();
	
	public abstract InputStream download(String[] billParams);
}
