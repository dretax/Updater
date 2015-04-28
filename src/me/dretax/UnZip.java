package me.dretax;

/**
 * Created by DreTaX on 2015.04.25
 */
import java.io.*;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class UnZip {
	List<String> fileList;
	private static String INPUT_ZIP_FILE = "";
	private static String OUTPUT_FOLDER = "";

	public UnZip(String input, String output) {
		INPUT_ZIP_FILE = input;
		OUTPUT_FOLDER = output;
		try {
			unZipIt(INPUT_ZIP_FILE, OUTPUT_FOLDER);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public void unZipIt(String zipFile, String location) throws IOException {
		int size;
		byte[] buffer = new byte[8192];

		try {
			if (!location.endsWith("/")) {
				location += "/";
			}
			File f = new File(location);
			if (!f.isDirectory()) {
				f.mkdirs();
			}
			ZipInputStream zin = new ZipInputStream(new BufferedInputStream(new FileInputStream(zipFile), 8192));
			try {
				ZipEntry ze = null;
				while ((ze = zin.getNextEntry()) != null) {
					String path = location + ze.getName();
					File unzipFile = new File(path);
					String name = unzipFile.getName();
					if (name.toLowerCase().contains("lumaemu") || name.toLowerCase().contains("start.bat")) {
						File filerino = new File(location + File.separator + name);
						if (filerino.exists()) {
							continue;
						}
					}

					if (ze.isDirectory()) {
						if (!unzipFile.isDirectory()) {
							unzipFile.mkdirs();
						}
					} else {
						// check for and create parent directories if they don't exist
						File parentDir = unzipFile.getParentFile();
						if (null != parentDir) {
							if (!parentDir.isDirectory()) {
								parentDir.mkdirs();
							}
						}

						// unzip the file
						FileOutputStream out = new FileOutputStream(unzipFile, false);
						BufferedOutputStream fout = new BufferedOutputStream(out, 8192);
						
						try {
							while ((size = zin.read(buffer, 0, 8192)) != -1) {
								fout.write(buffer, 0, size);
							}

							zin.closeEntry();
						} finally {
							fout.flush();
							fout.close();
						}
					}
				}
			} finally {
				zin.close();
			}
		} catch (Exception e) {
			System.out.println("Failed to unzip");
		}
	}
}