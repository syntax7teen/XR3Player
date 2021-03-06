package main.java.com.goxr3plus.xr3player.remote.dropbox.services;

import java.io.FileOutputStream;
import java.io.IOException;

import com.dropbox.core.DbxDownloader;
import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.DownloadErrorException;
import com.dropbox.core.v2.files.DownloadZipResult;
import com.dropbox.core.v2.files.FileMetadata;

import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.util.Duration;
import main.java.com.goxr3plus.xr3player.application.tools.ActionTool;
import main.java.com.goxr3plus.xr3player.application.tools.JavaFXTools;
import main.java.com.goxr3plus.xr3player.application.tools.NotificationType;
import main.java.com.goxr3plus.xr3player.remote.dropbox.io.ProgressOutputStream;
import main.java.com.goxr3plus.xr3player.remote.dropbox.presenter.DropboxFile;
import main.java.com.goxr3plus.xr3player.remote.dropbox.presenter.DropboxViewer;

public class DownloadService extends Service<Boolean> {
	
	/**
	 * DropBoxViewer
	 */
	public DropboxViewer dropBoxViewer;
	
	// Create Dropbox client
	private final DbxRequestConfig config = new DbxRequestConfig("XR3Player");
	private DbxClientV2 client;
	private DropboxFile dropboxFile;
	private String localFileAbsolutePath;
	
	/**
	 * Constructor
	 * 
	 * @param dropBoxViewer
	 */
	public DownloadService(DropboxViewer dropBoxViewer) {
		this.dropBoxViewer = dropBoxViewer;
		
	}
	
	/**
	 * Restart the Service
	 * 
	 * @param path
	 *            The path to follow and open the Tree
	 */
	public void startService(DropboxFile dropboxFile , String localFileAbsolutePath) {
		this.dropboxFile = dropboxFile;
		this.localFileAbsolutePath = localFileAbsolutePath;
		
		//Restart
		super.restart();
	}
	
	@Override
	protected Task<Boolean> createTask() {
		return new Task<Boolean>() {
			@Override
			protected Boolean call() throws Exception {
				
				try {
					
					//Create the Client
					client = new DbxClientV2(config, dropBoxViewer.getAccessToken());
					
					//Try to download the File
					downloadFile(client, dropboxFile, localFileAbsolutePath);
					
					//Show message to the User
					Platform.runLater(() -> ActionTool.showFontIconNotification("Download completed",
							"Completed downloading " + ( !dropboxFile.isDirectory() ? "File" : "Folder" ) + " :\n[ " + dropboxFile.getMetadata().getName() + " ]",
							Duration.millis(3000), NotificationType.SIMPLE, JavaFXTools.getFontIcon("fa-dropbox", dropBoxViewer.FONT_ICON_COLOR, 64)));
					
				} catch (Exception ex) {
					ex.printStackTrace();
					
					//Show message to the User
					Platform.runLater(() -> ActionTool.showNotification("Download Failed",
							"Failed to download " + ( !dropboxFile.isDirectory() ? "File" : "Folder" ) + ":\n[ " + dropboxFile.getMetadata().getName() + " ]",
							Duration.millis(3000), NotificationType.ERROR));
				}
				
				return true;
			}
			
			/**
			 * Download Dropbox File to Local Computer
			 * 
			 * @param client
			 *            Current connected client
			 * @param dropboxFile
			 *            The file path on the Dropbox cloud server -> [/foldername/something.txt] or a Folder [/fuck]
			 * @param localFileAbsolutePath
			 *            The absolute file path of the File on the Local File System
			 * @throws DbxException
			 * @throws DownloadErrorException
			 * @throws IOException
			 */
			public void downloadFile(DbxClientV2 client , DropboxFile dropboxFile , String localFileAbsolutePath) throws DownloadErrorException , DbxException , IOException {
				String dropBoxFilePath = dropboxFile.getMetadata().getPathLower();
				
				//Simple File
				if (!dropboxFile.isDirectory()) {
					//Create DbxDownloader
					try (DbxDownloader<FileMetadata> dl = client.files().download(dropBoxFilePath);
							//FileOutputStream
							FileOutputStream fOut = new FileOutputStream(localFileAbsolutePath);
							//ProgressOutPutStream
							ProgressOutputStream output = new ProgressOutputStream(fOut, dl.getResult().getSize(), (long completed , long totalSize) -> {
								//System.out.println( ( completed * 100 ) / totalSize + " %")
								
								//this.updateProgress(completed, totalSize)
							})) {
								
						//FileOutputStream
						System.out.println("Downloading .... " + dropBoxFilePath);
						
						//Add a progress Listener
						dl.download(output);
						
						//Fast way...
						//client.files().downloadBuilder(file).download(new FileOutputStream("downloads/" + md.getName()))
						//DbxRawClientV2 rawClient = new DbxRawClientV2(config,dropBoxViewer.getAccessToken())
						//DbxUserFilesRequests r = new DbxUserFilesRequests(client)
					} catch (Exception ex) {
						ex.printStackTrace();
					}
					//Directory
				} else {
					//Create DbxDownloader
					try (DbxDownloader<DownloadZipResult> dl = client.files().downloadZip(dropBoxFilePath);
							//FileOutputStream
							FileOutputStream fOut = new FileOutputStream(localFileAbsolutePath)) {
						
						//FileOutputStream
						System.out.println("Downloading .... " + dropBoxFilePath);
						
						//Add a progress Listener
						dl.download(fOut);
						
					} catch (Exception ex) {
						ex.printStackTrace();
					}
				}
			}
			
		};
		
	}
	
	/**
	 * @return the client
	 */
	public DbxClientV2 getClient() {
		return client;
	}
	
}
