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

public class SingtelBillDownloader extends AbstractBillDownloader {
	private final String ssoId;
	private final String password;
	private final String accountNumber;

	public SingtelBillDownloader(String taskName, String dropboxFolder,
			String ssoId, String password) {
		this(taskName, dropboxFolder, ssoId, password, null);
	}

	public SingtelBillDownloader(String taskName, String dropboxFolder,
			String ssoId, String password, String accountNumber) {
		super(taskName, dropboxFolder);
		this.ssoId = ssoId;
		this.password = password;
		this.accountNumber = accountNumber;
	}

	@Override
	public boolean login() {
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.protocol.handle-redirects", false);
		
		HttpPost request = new HttpPost("https://onepass.singtel.com/login/mybill.action");
		request.setHeader("Keep-Alive", "true");
		request.setHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		request.setHeader("Host", "onepass.singtel.com");
		request.setHeader(
				"Referer",
				"https://mybill.singtel.com/login/login.asp?c=1");
		request.setParams(params);

		List<NameValuePair> loginPostDataPairs = new ArrayList<NameValuePair>();
		loginPostDataPairs.add(new BasicNameValuePair("loginOption", "sso"));
		loginPostDataPairs.add(new BasicNameValuePair("ssoid", this.ssoId));
		loginPostDataPairs.add(new BasicNameValuePair("password", this.password));
		try {
			request.setEntity(new UrlEncodedFormEntity(loginPostDataPairs));
			httpclient.execute(request, localContext);
			return true;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
	}

	@Override
	public HashMap<String, String[]> getAvailableBills() {
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.protocol.handle-redirects", false);

		HttpGet request = new HttpGet(
				"https://mybill.singtel.com/ebill/mybill.asp");
		request.setHeader("Keep-Alive", "true");
		request.setHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		request.setHeader("Host", "mybill.singtel.com");
		request.setHeader("Referer",
				"https://mybill.singtel.com/login/login.asp");
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
				while (contents.contains("<a href=\"pdf_sa.asp?acn=")) {
					if (contents.indexOf("<a href=\"pdf_sa.asp?acn=") != 0) {
						// drop everything before that
						contents = contents.substring(contents
								.indexOf("<a href=\"pdf_sa.asp?acn="));
						contents = contents.substring(contents.indexOf('"') + 1);
					}
					String lineToParse = contents.substring(0, contents.indexOf('"'));
					contents = contents.replace(lineToParse, "");
					SimpleEntry<String, String[]> billParameters = getBillParameters(lineToParse);
					
					if (accountNumber == null || billParameters.getValue()[0].equalsIgnoreCase(accountNumber))
						result.put(billParameters.getKey(), billParameters.getValue());
				}
			}
			return result;
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}

	private SimpleEntry<String, String[]> getBillParameters(String lineToParse) {
		String line = lineToParse.substring(lineToParse.indexOf('?') + 1);
		String[] params = line.split("&");
		
		StringBuilder id = new StringBuilder();
		for(int counter=0; counter<params.length; counter++) {
			params[counter] = params[counter].split("=")[1];
			id.append(params[counter]);
			
			if(counter != params.length - 1)
				id.append("-");
		}
		return new SimpleEntry<String, String[]>(id.toString(), params);
	}

	@Override
	public InputStream download(String[] billParams) {
		HttpParams params = new BasicHttpParams();
		params.setParameter("http.protocol.handle-redirects", false);
		
		HttpGet request = new HttpGet(
				"https://mybill.singtel.com/ebill/pdf_sa.asp?acn="
						+ billParams[0] + "&bid=" + billParams[1] + "&bdt="
						+ billParams[2]);
		request.setHeader("Keep-Alive", "true");
		request.setHeader("Accept",
				"text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
		request.setHeader("Host", "mybill.singtel.com");
		request.setHeader(
				"Referer",
				"https://mybill.singtel.com/ebill/mybill.asp");
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
