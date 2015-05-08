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
	public final String UpdaterVersion = "1.3";
	public int MaxDownloadSpeed;

	public Updater(String[] args) {
		cls = this;
		this.options = new Options();
		this.parser = new BasicParser();
		this.dir = System.getProperty("user.dir");
		this.config = new Config();
		this.config.setStrictOperator(true);
		this.options.addOption("h", "help", false, "Show help.");
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
				fil.delete();
			}
		});
		Parse();

	}

	private void Parse() {
		print("Updater V" + UpdaterVersion);
		print(Creator);
		if (cmd.hasOption("h") || (cmd.hasOption("s") && cmd.hasOption("c")) || (!cmd.hasOption("s") && !cmd.hasOption("c"))) {
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
			print("Failed to reach Versions inifile");
			e.printStackTrace();
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
		try {
			this.latest = getValue("VersionCheck", "Latest");
		} catch (IOException e) {
			print("Failed to find the latest version.");
			e.printStackTrace();
		}
		String link = null;
		if (cmd.hasOption("s")) {
			link = GetLink(1);
			if (link == null) {
				print("Failed to get the link.");
				return;
			}
		} else if (cmd.hasOption("c")) {
			link = GetLink(2);
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

	private String GetLink(int integer) {
		if (integer == 1) {
			if (cmd.hasOption("l")) {
				print("Downloading Legacy Server..") ;
				try {
					return getValue("Legacy", "Server");
				} catch (IOException e) {
					print("Couldn't find the specified server version!");
					//e.printStackTrace();
					return null;
				}
			}
			if (cmd.getOptionValue("v") != null) {
				try {
					print("Trying to download version: " + cmd.getOptionValue("v").toUpperCase());
					return getValue("Server", cmd.getOptionValue("v").toUpperCase());
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
			if (cmd.hasOption("l")) {
				print("Downloading Legacy Client..") ;
				try {
					return getValue("Legacy", "Client");
				} catch (IOException e) {
					print("Couldn't find the specified client version!");
					//e.printStackTrace();
					return null;
				}
			}
			if (cmd.getOptionValue("v") != null) {
				try {
					print("Trying to download version: " + cmd.getOptionValue("v").toUpperCase());
					if (is64bit) {
						print("Detecting 64bit OS");
						return getValue("Client", cmd.getOptionValue("v").toUpperCase());
					}
					print("Detecting 32bit OS");
					return getValue("Client32", cmd.getOptionValue("v").toUpperCase());
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
