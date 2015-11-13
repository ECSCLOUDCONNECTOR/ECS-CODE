package com.ecsconnector;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.nt.NTEventLogAppender;

import com.liaison.ecs.cloud.ECSConnectorObject;

public class FromMerck {
	final static Logger log = org.apache.log4j.Logger
			.getLogger(FromMerck.class);
	static Scanner in = new Scanner(System.in);
	static ECSConnectorObject ECSConnect = null;
	static File ECSPath = null;
	static File MerckPath = null;
	static String targetPath = null;
	static String fileNamePattern = null;
	static String userName = null;
	static String Password =null;
	static boolean appendToFile = false;
	static boolean override = false;
	static boolean testMode = false;
	static boolean isAnyFileMatchesPattern = false;
	static OutputStream outputStream;

	public static void main(String[] args) {

		log.info("####################FromMerck####################");
		NTEventLogAppender eventLogAppender = new NTEventLogAppender();
		eventLogAppender.setThreshold(Level.ERROR);
		log.addAppender(eventLogAppender);
		for(String s:args) {
			if (s.contains("-t")) {
				testMode = true;
				break;
			} else if (s.contains("/p")) {
				targetPath = s.substring(2).toString();
				log.info("TargetPath :" + targetPath);
			} else if (s.contains("/f")) {
				fileNamePattern = s.substring(2).toString();
				log.info("FileName : " + fileNamePattern);
			} else if (s.contains("/u")) {
				userName = s.substring(2).toString();
				log.info("userName : " + userName);
			} else if (s.contains("/c")) {
				Password = s.substring(2).toString();
				log.info("password : " + Password);
			} else if (s.contains("-ba")) {
				appendToFile = true;
				log.info("Checked Append to Existing File");
			} else if (s.contains("-bo")) {
				override = true;
				log.info("Checked Override the Existing File");
			} else {
				log.info("Invalid argument : "
						+s.toString());
			}			
		}	
		if (targetPath == null || userName == null
				|| Password == null) {

			System.out
			.println("Invalid arguements.Please provide mandatory arguements: \n\n /p    Path the Target file will be placed \n /f    Filename or FileNamepattern of the Source File \n "
					+ "/u    User - the user used to authenticate against ECS \n /c    Credentials - the password used to authenticate against ECS \n-ba    Behavior in case of duplications, Append to existing file \n-bo    Behavior in case of duplications, Overwrite the existing file\n ");
		} else {

			FromMerck obj = new FromMerck();

			try {
				log.info("Target Folder Path is : " + targetPath);
				obj.validatePath(targetPath, false);
				obj.movefile(targetPath);
				log.info("Exit");
			} catch (NoSuchElementException e) {
				log.error("Could Not Find such file path.\nPlease verify that the path is correct and try again.");
			}
		}
	}

	public void validatePath(String folderPath, boolean isValid) {
		while (!isValid) {
			if (folderPath.matches("^(.+)/[^/]+$")) {
				isValid = true;
				if (targetPath == null) {
					log.info("The Current Target Folder is : " + folderPath);
					targetPath = folderPath;
				}
			} else {
				isValid = false;
				targetPath = null;
				log.error("Invalid Target path.Enter Correct Target Path");
				folderPath = in.nextLine();
			}
		}
	}

	public void movefile(String targetPath) {
		ECSConnect = new ECSConnectorObject();
		ECSConnect.setUser(userName);
		ECSConnect.setPass(Password.toCharArray());
		ECSConnect.setContentType("application/text");
		log.info("ECS URL is: " + ECSConnect.getUrl());
		if(fileNamePattern == null)
		{
			log.info("File Name or Pattern is not sepecifed, so Pending DocIDs will be taken as the file Name pattern");
		}
		try {
			ArrayList<String> docIds = ECSConnect.getPendingDocIds();				

			// Looping through and retrieving any pending documents that are
			// available for the user
			if (!testMode) {
				if (docIds.size() > 0) {

					for (String s : docIds) {
						String wildCard = "^(?i)";
						if(fileNamePattern != null)
						{					    
							wildCard = wildCard + fileNamePattern.replace(".", "\\.");
						}
						else
						{							
							wildCard = wildCard + s.replace(".", "\\.");							
						}
						wildCard = wildCard.replace("*", ".*");
						if (s.matches(wildCard)) {
							isAnyFileMatchesPattern = true;							
							log.info(s + " Matches the file Pattern");							
							File path = new File(targetPath + "/" + s + ".edi");
							if (path.exists()) {
								log.info(path + " already exists");
								if (appendToFile) {
									outputStream = new FileOutputStream(path,
											true);
									log.info(s
											+ " Contents will be appended to the existing File in : "
											+ path);
								} else {
									outputStream = new FileOutputStream(path);
									log.info(s
											+ " Contents will be Overridden the existing file in : "
											+ path);
								}
							} else {
								outputStream = new FileOutputStream(path);
							}

							// Making the connection to ECS to GET the file id
							// of 's'
							ECSConnect.read(outputStream, s);
							String a = ECSConnect.getHeader("MFT-Filename");

							log.info("The " + s + ".edi"
									+ " has been successfully placed in "
									+ targetPath);

							outputStream.close();
						}
					}
					if (!isAnyFileMatchesPattern) {
						log.info("None of the fileName in the server Matches the given pattern");
					}
				} else {
					log.info("No files found in the ECS server to download");
				}
			} else {
				log.info("ECS Server connection has been established");
			}
		} catch (FileNotFoundException e) {
			log.error("Could not find the specified path: " + targetPath
					+ "\nPlease verify that the path is correct and try again.");
		} catch (IOException ex) {
			log.error("ECS could not write to the following path: "
					+ targetPath
					+ "\nPlease verify that the permissions are correct and try again.");
		} catch (Exception e) {
			log.error("Something went wrong when sending: "
					+ targetPath
					+ "\nPlease verify that the settings are correct and try again.");
		}
	}
}
