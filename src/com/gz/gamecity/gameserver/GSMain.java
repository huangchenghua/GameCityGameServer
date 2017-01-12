package com.gz.gamecity.gameserver;



public class GSMain {

	public static void main(String[] args) {
		Thread shutdownHook = new Thread() {  
            public void run() {  
            	GameServiceMain.getInstance().stopServer(); 
            }  
        };  
        Runtime.getRuntime().addShutdownHook(shutdownHook);  
        
        GameServiceMain.getInstance().startServer();

	}

}
