package jabs.automaticbillsdownloader;

import jabs.automaticbillsdownloader.downloaders.AbstractBillDownloader;
import jabs.automaticbillsdownloader.downloaders.SingtelBillDownloader;
import jabs.automaticbillsdownloader.downloaders.SpServicesBillDownloader;
import jabs.automaticbillsdownloader.model.Bill;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import android.os.AsyncTask;
import android.os.Environment;
import android.util.Log;

import com.dropbox.client2.DropboxAPI;
import com.dropbox.client2.DropboxAPI.Entry;
import com.dropbox.client2.android.AndroidAuthSession;
import com.dropbox.client2.exception.DropboxException;

public abstract class DownloadManager extends AsyncTask<Void, Integer, Void> {
	private DropboxAPI<AndroidAuthSession> mDBApi;
	private List<AbstractBillDownloader> billList;
	
	public DownloadManager(DropboxAPI<AndroidAuthSession> mDBApi, List<Bill> bills) {
		this.mDBApi = mDBApi;
		this.billList = new ArrayList<AbstractBillDownloader>();
		
		for(Bill bill : bills) {
			if(bill.getBillType().equalsIgnoreCase("Singtel")) {
				this.billList.add(new SingtelBillDownloader(bill.toString(),
						bill.getDropboxFolder(), bill.getUsername(), bill
								.getPassword()));
			}
			else if(bill.getBillType().equalsIgnoreCase("SP Services")) {
				this.billList.add(new SpServicesBillDownloader(bill.toString(),
						bill.getDropboxFolder(), bill.getUsername(), bill
								.getPassword()));
			}
		}
	}
	
	@Override
	protected Void doInBackground(Void... params) {
		try {
			publishProgress(0);
			
			int billCount = 0;
			for (AbstractBillDownloader bill : billList) {
				if(bill.login() == false) {
					billCount++;
					publishProgress((int) ((double) billCount / billList.size() * 100));
					continue;
				}
				
				HashMap<String, String[]> availFiles = bill.getAvailableBills();

				int fileCount = 0;
				for (String fileId : availFiles.keySet()) {
					String dropboxFolder = bill.getDropboxFolder() + "/"
							+ bill.getName();
					String fileName = fileId + ".pdf";

					if (fileExistsOnDropbox(dropboxFolder, fileName) == false) {
						InputStream stream = bill.download(availFiles.get(fileId));
						File tempFile = writeToFile(stream, fileId + ".pdf");
						writeToDropbox(dropboxFolder, tempFile);
						tempFile.delete();
					}
					double percentComplete = (billCount + ((double) ++fileCount / availFiles.size())) / billList.size();
					this.publishProgress((int) (100 * percentComplete));
				}
				billCount++;
			}
		} catch (DropboxException | IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected abstract void onProgressUpdate(Integer... progress);
	
	protected abstract void onPostExecute(Void result);
	
	private boolean fileExistsOnDropbox(String dropboxFolder, String fileName) {
		try {
			Entry entry = mDBApi.metadata(dropboxFolder + "/" + fileName,
					1, null, false, null);
			return (entry.isDeleted == false);
		} catch (Exception exception) {
			return false;
		}
	}
	
	private void writeToDropbox(String dropboxFolder, File file) throws FileNotFoundException, DropboxException {
		FileInputStream inputStream = new FileInputStream(file);
		Entry response = mDBApi.putFile(dropboxFolder + "/" + file.getName(), inputStream,
		                                file.length(), null, null);
		Log.i("DbExampleLog", "The uploaded file's rev is: " + response.rev);
	}
	
	private File writeToFile(InputStream inputStream, String fileName) throws IOException {
		final int MEGABYTE = 1024 * 1024;
		
		String extStorageDirectory = Environment.getExternalStorageDirectory().toString();
        File pdfFile = new File(extStorageDirectory, fileName);
        if(pdfFile.exists() == false) {
        	pdfFile.createNewFile();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(pdfFile);

        byte[] buffer = new byte[MEGABYTE];
        int bufferLength = 0;
        while((bufferLength = inputStream.read(buffer))>0 ){
            fileOutputStream.write(buffer, 0, bufferLength);
        }
        fileOutputStream.close();
        return pdfFile;
	}
}
