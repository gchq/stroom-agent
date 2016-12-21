package stroom.agent.tools;

import java.io.InputStreamReader;
import java.io.LineNumberReader;

public class FindGaps {
	public static void main(String[] args) throws Exception {
		LineNumberReader reader = new LineNumberReader(new InputStreamReader(System.in));
		
		String line;
		
		Long lastNumber = null;
		String lastLine = null;
		
		while ((line = reader.readLine()) != null) {
			String[] parts = line.split(",");
			String numberS = parts[0].replace("\"", "");
			long number = Long.parseLong(numberS);
			
			if (lastNumber != null) {
				if (lastNumber + 1 != number) {
					System.out.println(lastLine);
					System.out.println(line);
				}
			}
			
			lastNumber = number;
			lastLine = line;
			
		}
		
		
	}
}
