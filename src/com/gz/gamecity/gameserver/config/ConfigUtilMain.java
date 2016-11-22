package com.gz.gamecity.gameserver.config;

import java.io.FileWriter;
import java.io.IOException;

import com.gz.util.Config;

public class ConfigUtilMain {

	public static void main(String[] args) {
		Config.instance().init();
		FileWriter writer=null;
		try {
			writer = new FileWriter("./src/com/gz/gamecity/gameserver/config/ConfigField.java");
			writer.write("package com.gz.gamecity.gameserver.config;"+"\r\n");
			writer.write("\r\n");
			writer.write("public class ConfigField{"+"\r\n");
			writer.write("\r\n");
			
			for (String field:Config.configs.keySet()){
				writer.write("	"+"public static final String "+field.toUpperCase()+" = \""+field+"\";\r\n");
			}
			
			writer.write("\r\n");
			writer.write("}");
			writer.flush();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				writer.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
