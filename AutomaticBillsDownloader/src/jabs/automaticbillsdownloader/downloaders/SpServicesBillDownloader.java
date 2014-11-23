package jabs.automaticbillsdownloader.downloaders;


import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;

public class SpServicesBillDownloader extends AbstractBillDownloader {
	private String username;
	private String password;
	
	public SpServicesBillDownloader(String taskName, String dropboxFolder,
			String username, String password) {
		super(taskName, dropboxFolder);
		this.username = username;
		this.password = password;
	}
	
	public boolean login() {
		try {
			HttpParams params = new BasicHttpParams();
			params.setParameter("http.protocol.handle-redirects", false);

			HttpPost request = new HttpPost(
					"https://services.spservices.sg/ssllogin/verifpwd-cansupply.asp");
			request.setHeader("Keep-Alive", "true");
			request.setHeader("Accept",
					"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
			request.setHeader(
					"Referer",
					"https://services.spservices.sg/ssllogin/cs_mybills_login.asp?OriginalURL=cs_mybills_frameset.asp");
			request.setParams(params);

			List<NameValuePair> loginPostDataPairs = new ArrayList<NameValuePair>();
			loginPostDataPairs.add(new BasicNameValuePair("OriginalURL",
					"cs_mybills_frameset.asp"));
			loginPostDataPairs.add(new BasicNameValuePair("identity", ""));
			loginPostDataPairs.add(new BasicNameValuePair("UserID", this.username));
			loginPostDataPairs
					.add(new BasicNameValuePair("Password", this.password));
			request.setEntity(new UrlEncodedFormEntity(loginPostDataPairs));
			httpclient.execute(request, localContext);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	public HashMap<String, String[]> getAvailableBills() {
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.protocol.handle-redirects", false);

		HttpPost request = new HttpPost(
				"https://services.spservices.sg/cs_mybills.asp?whereFrom=progbar&key=");
		request.setHeader("Keep-Alive", "true");
		request.setHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		request.setParams(params);

		try {
			HttpResponse response = httpclient.execute(request, localContext);
			InputStream stream = response.getEntity().getContent();

			BufferedReader reader = new BufferedReader(new InputStreamReader(
					stream));
			HashMap<String, String[]> result = new HashMap<String, String[]>();

			String contents;
			while ((contents = reader.readLine()) != null) {
				contents = contents.trim();
				while (contents.contains("<a href=\"javascript:new_win2('")) {
					if (contents.indexOf("<a href=\"javascript:new_win2('") != 0) {
						// drop everything before that
						contents = contents.substring(contents
								.indexOf("<a href=\"javascript:new_win2('"));
					}
					String lineToParse = contents.substring(0,
							contents.indexOf("</a>") + 4);
					contents = contents.replace(lineToParse, "");
					SimpleEntry<String, String[]> billParameters = getBillParameters(lineToParse);
					result.put(billParameters.getKey(),
							billParameters.getValue());
				}
			}
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	private SimpleEntry<String, String[]> getBillParameters(String contents) {
		String[] parameters = null;
        parameters = contents.substring(contents.indexOf('(') + 1, contents.indexOf(')')).split(",");
        for (int i = 0; i < parameters.length; i++)
        {
            parameters[i] = parameters[i].trim();
            if (parameters[i].startsWith("'"))
                parameters[i] = parameters[i].substring(1);
            if (parameters[i].endsWith("'"))
                parameters[i] = parameters[i].substring(0, parameters[i].length() - 1);
        }
        String month = contents.substring(contents.indexOf(">") + 1, contents.indexOf("</a>"));
        return new SimpleEntry<String, String[]>(month, parameters);
	}
	
	public InputStream download(String[] parameters) {
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.protocol.handle-redirects",false);
		
		String downloadUrl = parameters.length == 4 ? "https://services.spservices.sg/viewpdf/cs_mybill_viewpdf.asp?pAid="
				+ parameters[0] + "&docId=" + parameters[1] + "&cnt=" + parameters[4] + "&prem=" + parameters[2] + "," + parameters[3]
				: "https://services.spservices.sg//viewpdf/ccm_showpdf2.asp?IPDF=" + parameters[0] + "&IURL=" + parameters[1];
		HttpGet request = new HttpGet(downloadUrl);
		request.setHeader("Keep-Alive", "true");
		request.setHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		request.setParams(params);
        
		try {
			HttpResponse response = httpclient.execute(request, localContext);
			return response.getEntity().getContent();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
}
