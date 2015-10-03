package com.sanjay900.jgbe.emu;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


public final class FHandler {

	public static InputStream getInputStream(Path fname) throws IOException {
		String error = "";
		try {
			return new FileInputStream(fname.toFile());
		} catch (Exception e) {
			error = error + e.toString() + '\n';
		};
		throw new IOException(error);
	}


	public static DataInputStream getDataInputStream(String fname) throws IOException {

		InputStream instr = getInputStream(Paths.get(fname));

		int dotPos=0;
		int dp = fname.indexOf(".");
		while(dp>=0) {
			dotPos=dp;
			dp=fname.indexOf(".",dp+1);
		}
		String fext = fname.substring(dotPos);
		if ( !fext.equals(".zip") ) {
			DataInputStream distream = new DataInputStream(instr);
			return distream;
		}
		else {

			ZipInputStream zistream = new ZipInputStream(instr);


			ZipEntry entry = zistream.getNextEntry();

			BufferedInputStream bistream = new BufferedInputStream(zistream);
			DataInputStream distream = new DataInputStream(bistream);

			return distream;
		}



	}

	public static DataOutputStream getDataOutputStream(String fname) throws IOException {

		int dotPos=0;
		int dp = fname.indexOf(".");
		while(dp>0) {
			dotPos=dp;
			dp=fname.indexOf(".",dp+1);
		}
		String fext = fname.substring(dotPos);
		if ( !fext.equals(".zip") ) {

			FileOutputStream fostream = new FileOutputStream(fname);
			BufferedOutputStream bostream = new BufferedOutputStream(fostream);
			DataOutputStream dostream = new DataOutputStream(bostream);

			return dostream;
		}
		else {
			System.out.println("FHandler opening zipfile not supported!");
			return null;
		}



	}

}
