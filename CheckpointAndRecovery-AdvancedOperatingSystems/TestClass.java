import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;


public class TestClass {
public static void main(String[] args) {
	System.out.println("Comparison results :"+CompareFile());
}
public static boolean CompareFile() {
	System.out.println("in compare file");
	try {
	    FileReader fr = new FileReader("Node3.txt"); 
	    FileReader fr1 = new FileReader("Node2.txt");
	
	    BufferedReader br = new BufferedReader(fr);
	    BufferedReader br2 = new BufferedReader(fr1);
	
	    String s1;
	    String s2 = null;
	    
	    while(((s1 = br.readLine()) != null) && ((s2 = br2.readLine()) != null)) { 
	    	System.out.println("Line s1: "+s1);
	    	System.out.println("Line s2: "+s2);
	    	if(s1.compareTo(s2) !=0)	
	    	{
	    		br.close();
	    	    br2.close();
	    		return false;
	    	}
	    }
	    if(br.ready()){
	    	 br.close();
	 	    br2.close();
	    	return false;
	    }
	    if(br2.ready()){
	    	 br.close();
	 	    br2.close();
	    	return false;
	    }
	    br.close();
	    br2.close();
	    
	}catch (IOException e) {
		e.printStackTrace();
	}
	return true;
}
}


