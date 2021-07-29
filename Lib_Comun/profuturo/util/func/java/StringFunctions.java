package profuturo.util.func.java;

import java.util.regex.Pattern;

public class StringFunctions {
	public static String fillWithZerosJava ( String valor, String longitud ) {
		String formato = "%0" + String.valueOf(longitud) + "d";
		String strFormato = String.format(formato, Integer.valueOf(valor)); 
		return strFormato;
	}
	
	public static Boolean validarExpresion( String expresion, String valor ) {
		Boolean resultado = new Boolean(Pattern.matches(expresion, valor));
		return resultado;
	}
}