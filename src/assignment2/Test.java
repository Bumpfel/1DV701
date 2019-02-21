package assignment2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;


public class Test {
	public static void main(String[] args) throws IOException {

		FileInputStream fis = new FileInputStream("src/assignment2/content/bug.gif");

		byte[] bin = new byte[5000];
		bin = fis.readAllBytes();

		String s = new String(bin);

		System.out.print(s);

		// File f = new File(bin);

		// FileOutputStream fout = new FileOutputStream(f);

		// writeTextFile("somefile.txt", "Title\nrow1\n");
		// byte data[] = ...
		// FileOutputStream out = new FileOutputStream("the-file-name");
		// out.write(data);
		// out.close();
	}

	private static void writeTextFile(String fileName, String text) throws IOException {
		PrintWriter writer = new PrintWriter(fileName, "UTF-8");
		writer.println(text);
		writer.close();
	}

}
