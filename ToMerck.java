package com.ecsconnector;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.nt.NTEventLogAppender;

import com.liaison.ecs.cloud.ECSConnectorObject;

public class ToMerck {

	final static Logger log = org.apache.log4j.Logger.getLogger(ToMerck.class);
	static Scanner in = new Scanner(System.in);
	static ECSConnectorObject ECSConnect = null;
	static File ECSPath = null;
	static File MerckPath = null;
	static String fileNamePattern = null;
	static File sourceFile = null;
	static String sourcePath = null;
	static String userName = null;
	static String Password = null;
	static File Merckfile = null;
	static File targetFilePath = new File("");
	static List<File> sourceFileList = new ArrayList<File>();

	public static void main(String[] args) {
		log.info("####################ToMerck####################");
		NTEventLogAppender eventLogAppender = new NTEventLogAppender();
		eventLogAppender.setThreshold(Level.ERROR);
		log.addAppender(eventLogAppender);
		for(String s : args) {
			if (s.contains("/p")) {
				sourcePath = s.substring(2).toString();
				log.info("TargetPath :" + sourcePath);
			} else if (s.contains("/f")) {
				fileNamePattern = s.substring(2).toString();
				log.info("FileNamePattern : " + fileNamePattern);
			} else if (s.contains("/u")) {
				userName = s.substring(2).toString();
				log.info("userName : " + userName);
			} else if (s.contains("/c")) {
				Password =s.substring(2).toString();
				log.info("password : " + Password);
			} else {
				log.error("Invalid arguement: "
						+ s.toString());
			}
		}

		if (sourcePath == null || fileNamePattern == null || userName == null
				|| Password == null) {

			log.info("Invalid arguements.Please provide all Mandatory arguements: \n\n /p    Path the Source file will be placed \n /f    Filename or FileNamepattern of the Source File \n"
					+ " /u    User - the user used to authenticate against ECS \n /c    Credentials - the password used to authenticate against ECS \n ");
		} else {
			ToMerck obj = new ToMerck();

			try {
				log.info("Source Folder Path is : " + sourcePath);
				obj.validatePath(sourcePath, false);
				if (!sourceFileList.isEmpty()) {
					obj.movefile(sourceFileList, MerckPath, false);
					log.info("Exit");
				} else {
					log.info("Exit");
				}
			} catch (NoSuchElementException e) {
				log.error("Could Not Find such file path.\nPlease verify that the path is correct and try again.");
			}
		}
	}

	public void validatePath(String folderPath, boolean isValid) {
		while (!isValid)

		{
			if (folderPath.matches("^(.+)/[^/]+$")) {
				MerckPath = new File(folderPath);
				if (MerckPath.exists()) {
					log.info(MerckPath + " exists");
					isValid = true;
					String wildCard = "^(?i)";
					wildCard = wildCard + fileNamePattern.replace(".", "\\.");
					wildCard = wildCard.replace("*", ".*");

					File[] listOfFiles = MerckPath.listFiles();
					if (listOfFiles.length > 0) {
						for (int i = 0; i < listOfFiles.length; i++) {
							sourceFile = listOfFiles[i];
							if (sourceFile.isDirectory()) {
								continue;
							}

							if (sourceFile.getName().matches(wildCard)) {
								log.info(sourceFile.getName()
										+ " Matches the given pattern");
								sourceFileList.add(sourceFile);
							}
						}
						if (sourceFileList.isEmpty()) {
							log.info("The Source Folder doesn't consist of any files matching the fileName pattern");
						}
					} else {
						log.info("The Source Folder Doesn't consist of any files or folders");
					}
				} else {
					log.error("Could not find the specified path: "
							+ MerckPath.getAbsolutePath()
							+ "\nEnter the Correct Path again");
					folderPath = in.nextLine();
					isValid = false;
				}
			} else {
				isValid = false;
				log.error("Invalid Source file path.Please Try Again");
				System.out.println("Invalid Source path.Enter Again");
				folderPath = in.nextLine();
			}
		}

	}

	public void movefile(List<File> sourceFileList, File sourcfilePath,
			boolean isValid) {
		ECSConnect = new ECSConnectorObject();
		log.info("ECS URL is: " + ECSConnect.getUrl());
		ECSConnect.setUser(userName);
		ECSConnect.setPass(Password.toCharArray());
		ECSConnect.setContentType("application/text");

		for (int i = 0; i < sourceFileList.size(); i++) {
			Merckfile = sourceFileList.get(i);
			ECSConnect.setHeader("MFT-Filename", Merckfile.getName());
			try {
				if (Merckfile.exists()) {
					// Reading in the input file
					InputStream inputStream = new FileInputStream(Merckfile);
					// Checking the output file
					OutputStream outputStream = new FileOutputStream(new File(
							targetFilePath + "/" + Merckfile.getName()));

					// Making the connection to ECS with the input/output
					// streams we created
					ECSConnect.write(inputStream, outputStream);

					inputStream.close();
					outputStream.close();
					log.info(Merckfile.getName()
							+ " has been moved successfully to ECS");
				}
			} catch (FileNotFoundException e) {
				log.error("Could not find the specified path: "
						+ Merckfile.getPath()
						+ "\nPlease verify that the path is correct and try again.");
				break;
			} catch (IOException ex) {
				log.error("Could not read/write the following file: "
						+ Merckfile.getPath()
						+ "\nPlease verify that the permissions are correct and try again.");
				break;
			} catch (Exception e) {
				log.error("Something went wrong when sending: "
						+ Merckfile.getPath()
						+ "\nPlease verify that the settings are correct and try again.");
				break;
			}

		}
	}
}
