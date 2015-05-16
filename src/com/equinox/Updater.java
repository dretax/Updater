package com.equinox;

import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.ini4j.Config;
import org.ini4j.Ini;
import org.ini4j.Wini;
import org.json.JSONException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.Scanner;

/**
 * Created by DreTaX on 2015.04.24
 */
public class Updater {
	public String dir;
	public String foldername = null;
	private Options options;
	private CommandLine cmd;
	private CommandLineParser parser;
	private File fil;
	private Config config;
	private String latest;
	private File Auth;
	private String user;
	private String password;
	private static Updater cls;
	private boolean is64bit;
	private final String Creator = "Created By Equinox Gaming - www.equinoxgamers.com";
	public final String UpdaterVersion = "1.4";
	public int MaxDownloadSpeed;
	private Scanner scanner;

	public Updater(String[] args) {
		cls = this;
		this.options = new Options();
		this.parser = new BasicParser();
		this.dir = System.getProperty("user.dir");
		this.config = new Config();
		this.config.setStrictOperator(true);
		this.options.addOption("h", "help", false, "Show help.");
		this.options.addOption("vl", "versionlist", false, "List Versions.");
		this.options.addOption("sc", "scanner", false, "Enable User Friendly interactive mode");
		this.options.addOption("c", "client", false, "Download Client.");
		this.options.addOption("s", "server", false, "Download Server.");
		this.options.addOption("d", "directory", true, "Specify Directory. Defaults to workdir");
		this.options.addOption("v", "version", true, "Specify Version");
		this.options.addOption("f", "folder", true, "Specify Extraction Folder name");
		this.options.addOption("l", "legacy", false, "Specify this if you want the legacy version");
		this.Auth = new File(this.dir + "/Config.ini");
		if (!this.Auth.exists()) {
			try {
				this.Auth.createNewFile();
			} catch (IOException e) {
				e.printStackTrace();
			}
			Wini ini = null;
			try {
				ini = new Wini(this.Auth);
			} catch (IOException e) {
				e.printStackTrace();
			}
			assert ini != null;
			ini.setConfig(config);
			ini.add("Auth", "Email", "YourEmail");
			ini.add("Auth", "Password", "YourPassword");
			ini.add("MaxDownload", "SpeedInKB", "1024");
			try {
				ini.store();
			} catch (IOException e) {
				//e.printStackTrace();
				print("Failed to create config.");
				return;
			}
			print("Config file created. Please edit It.");
			return;
		}
		try {
			Ini ini = new Ini(this.Auth);
			this.user = ini.get("Auth", "Email");
			this.password = ini.get("Auth", "Password");
			this.MaxDownloadSpeed = Integer.parseInt(ini.get("MaxDownload", "SpeedInKB"));
		} catch (IOException e) {
			print("Failed to grab the login details.");
			return;
		}

		try {
			this.cmd = parser.parse(options, args);
		} catch (ParseException e) {
			e.printStackTrace();
			return;
		}
		GetBits();
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				if (fil != null && fil.exists())
					fil.delete();
			}
		});
		Parse();

	}

	private void Parse() {
		print("Updater V" + UpdaterVersion);
		print(Creator);
		if ((cmd.hasOption("h") || (cmd.hasOption("s") && cmd.hasOption("c")) || (!cmd.hasOption("s") && !cmd.hasOption("c"))) && !cmd.hasOption("vl") && !cmd.hasOption("sc")) {
			help();
			return;
		}
		URL url;
		try {
			url = new URL("https://dl.dropboxusercontent.com/u/136953717/Versions.ini");
		} catch (MalformedURLException e) {
			print("Failed to Grab Versions inifile");
			e.printStackTrace();
			return;
		}
		this.fil = new File(this.dir + "/Versions.ini");
		try {
			FileUtils.copyURLToFile(url, this.fil);
		} catch (IOException e) {
			print("Failed to reach Versions ini file");
			e.printStackTrace();
			return;
		}
		if (cmd.hasOption("vl")) {
			VersionList();
			return;
		}
		try {
			this.latest = getValue("VersionCheck", "Latest");
		} catch (IOException e) {
			print("Failed to find the latest version.");
			e.printStackTrace();
		}
		if (cmd.hasOption("sc")) {
			this.scanner = new Scanner(System.in);
			try {
				print("--------------------------------------------------------------------------------");
				print("Interactive mode detected, enabling user friendly mode.");
				print("Please note that in this mode everything will be downloaded into the current directory.");
				print("\n");
				print("For example Server files will be downloaded in a folder called RustServer.");
				print("\n");
				print("The following inputs that you will enter are NOT case-sensitive");
				print("--------------------------------------------------------------------------------");
				print("** Specify Server type. Legacy or Experimental? **");
				print("- Type TRUE for legacy");
				print("- Type FALSE for experimental");
				String ll = scanner.nextLine();
				if (ll == null || (!ll.equalsIgnoreCase("false") && !ll.equalsIgnoreCase("true"))) {
					print("You have to type TRUE or FALSE");
					return;
				}
				boolean leg = Boolean.parseBoolean(ll);
				String v = null;
				if (!leg) {
					print("** Specify me a rust version **");
					print("Type LATEST for the latest files.");
					v = scanner.nextLine();
					if (v != null) {
						if (v.equalsIgnoreCase("latest")) {
							v = null;
						}
						else {
							v = v.toUpperCase();
						}
					}
				}
				print("** Download client or server? Please specify: **");
				String type = scanner.nextLine();
				if (type.equalsIgnoreCase("client")) {
					if (leg) {
						this.foldername = "RustLegacyClient";
					}
					else {
						this.foldername = "RustClient";
					}
					String link = GetLink(1, v, leg);
					if (link == null)  {
						print("Failed to get link. Maybe specified wrong version?");
						print("Versions start with DB and ends with a number. Example: DB60");
						return;
					}
					DownloadFile(link);
				} else if (type.equalsIgnoreCase("server")) {
					if (leg) {
						this.foldername = "RustLegacyServer";
					}
					else {
						this.foldername = "RustServer";
					}
					String link = GetLink(1, v, leg);
					if (link == null)  {
						print("Failed to get link. Maybe specified wrong version?");
						print("Versions start with DB and a number. Example: DB60");
						return;
					}
					DownloadFile(link);
				} else {
					print("You have to type SERVER or CLIENT");
					return;
				}
			}
			catch(Exception ex) {
				print("Scanner failed. Shutting off.");
			}
			return;
		}
		if (cmd.getOptionValue("d") != null) {
			this.dir = cmd.getOptionValue("d");
			print("Path: " + this.dir);
			File dir = new File(this.dir);
			if (!dir.exists()) {
				dir.mkdirs();
			}
		}
		if (cmd.getOptionValue("f") != null) {
			this.foldername = cmd.getOptionValue("f");
		}
		String link = null;
		String ver = cmd.getOptionValue("v");
		if (ver != null) {
			ver = ver.toUpperCase();
		}
		boolean legacy = false;
		if (cmd.hasOption("l")) {
			legacy = true;
		}
		if (cmd.hasOption("s")) {
			link = GetLink(1, ver, legacy);
			if (link == null) {
				print("Failed to get the link.");
				return;
			}
		} else if (cmd.hasOption("c")) {
			link = GetLink(2, ver, legacy);
			if (link == null) {
				print("Failed to get the link.");
				return;
			}
		}
		DownloadFile(link);
	}

	private void DownloadFile(String link) {
		print("Downloading File....");
		Download(link);
		print("Finished!");
		print(Creator);
	}
	
	private void VersionList() {
		try {
			Ini rini = new Ini(this.fil);
			print("Server Versions:");
			for (String name : rini.get("Server").keySet()) {
				print("- " + name);
			}
			print("Client x64 Versions:");
			for (String name : rini.get("Client").keySet()) {
				print("- " + name);
			}
			print("Client x32 Versions:");
			for (String name : rini.get("Client32").keySet()) {
				print("- " + name);
			}
		} catch (IOException e) {
			print("Failed to reach Versions ini file");
		}
	}

	private void Download(String url) {
		MegaHandler mh = new MegaHandler(user, password);
		if (!user.equalsIgnoreCase("YourEmail") || !password.equalsIgnoreCase("YourPassword")) {
			print("Connecting to mega with the given credentials");
			try {
				mh.login();
			} catch (IOException e) {
				//e.printStackTrace();
				print("Failed to login to mega.");
			}
		} else {
			print("Connecting to mega as Anonymous");
		}
		String s;
		if (foldername == null) {
			s = this.dir;
		}
		else {
			s = this.dir + File.separator + this.foldername;
			File f = new File(s);
			f.mkdirs();
		}
		try {
			mh.download_verbose(url, s);
		} catch (NoSuchAlgorithmException e) {
			print("Failed to Download file ex 1.");
		} catch (NoSuchPaddingException e) {
			print("Failed to Download file ex 2.");
		} catch (InvalidKeyException e) {
			print("Failed to Download file ex 3.");
		} catch (IOException e) {
			print("Failed to Download file ex 4.");
			e.printStackTrace();
		} catch (IllegalBlockSizeException e) {
			print("Failed to Download file ex 5.");
		} catch (BadPaddingException e) {
			print("Failed to Download file ex 6.");
		} catch (InvalidAlgorithmParameterException e) {
			print("Failed to Download file ex 7.");
		} catch (JSONException e) {
			print("Failed to Download file ex 8.");
		}
	}

	private String GetLink(int integer, String version, boolean legacy) {
		if (integer == 1) {
			if (legacy) {
				print("Downloading Legacy Server..") ;
				try {
					return getValue("Legacy", "Server");
				} catch (IOException e) {
					print("Couldn't find the specified server version!");
					//e.printStackTrace();
					return null;
				}
			}
			if (version != null) {
				try {
					print("Trying to download version: " + version);
					return getValue("Server", version);
				} catch (IOException e) {
					//e.printStackTrace();
					print("Couldn't find the specified server version!");
					return null;
				}
			}
			try {
				print("Trying to download version: " + latest) ;
				return getValue("Server", latest);
			} catch (IOException e) {
				print("Failed to get the latest server version.");
				return null;
				//e.printStackTrace();
			}
		} else if (integer == 2) {
			if (legacy) {
				print("Downloading Legacy Client..") ;
				try {
					return getValue("Legacy", "Client");
				} catch (IOException e) {
					print("Couldn't find the specified client version!");
					//e.printStackTrace();
					return null;
				}
			}
			if (version != null) {
				try {
					print("Trying to download version: " + version);
					if (is64bit) {
						print("Detecting 64bit OS");
						return getValue("Client", version);
					}
					print("Detecting 32bit OS");
					return getValue("Client32", version);
				} catch (IOException e) {
					//e.printStackTrace();
					print("Couldn't find the specified client version!");
					return null;
				}
			}
			try {
				print("Trying to download version: " + latest);
				if (is64bit) {
					print("Detecting 64bit OS");
					return getValue("Client", latest);
				}
				print("Detecting 32bit OS");
				return getValue("Client32", latest);
			} catch (IOException e) {
				print("Failed to get the latest client version.");
				return null;
				//e.printStackTrace();
			}
		}
		return null;
	}

	private void help() {
		// This prints out some help
		HelpFormatter formater = new HelpFormatter();
		print("Updater for Rust Clients/Servers");
		formater.printHelp("Updater", options);
		System.exit(0);
	}

	private String getValue(String section, String key) throws IOException {
		Ini rini = new Ini(this.fil);
		return rini.get(section, key);
	}

	private void GetBits() {
		this.is64bit = false;
		if (System.getProperty("os.name").contains("Windows")) {
			this.is64bit = (System.getenv("ProgramFiles(x86)") != null);
		} else {
			this.is64bit = (System.getProperty("os.arch").indexOf("64") != -1);
		}
	}

	public void print(Object o) {
		System.out.println(o);
	}

	public static Updater getUpdater() {
		return cls;
	}
}
