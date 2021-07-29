package profuturo.util.file;

import java.io.ByteArrayOutputStream;
import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.ibm.broker.config.common.Base64;
import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;

public class FileZipper_JavaCompute extends MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		MbOutputTerminal alt = getOutputTerminal("alternate");

		MbMessage inMessage = inAssembly.getMessage();
		MbMessageAssembly outAssembly = null;
		try {
			// create new message as a copy of the input
			MbMessage outMessage = new MbMessage(inMessage);
			outAssembly = new MbMessageAssembly(inAssembly, outMessage);
			// ----------------------------------------------------------
			// Add user code below

			// Mensaje SOAP Request
			MbElement soapMsg = inAssembly.getMessage().getRootElement().getLastChild();
			MbElement body = soapMsg.getLastChild();
			MbElement archivos = body.getLastChild();
			
			DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");

			Date date = new Date();
			String time = dateFormat.format(date);

			//nombre del archivo zip
			String zipFileName = "archivo" + "_" + time + ".zip";

			// Arreglo de objetos de archivos
			MbElement[] arrArchivos = archivos.getAllElementsByPath("archivo");

			// Stream de bytes para response en bus
			ByteArrayOutputStream baos = new ByteArrayOutputStream();

			// Stream de archivo zip fisico ----------------------
			//MODIFICAR ESTO SI SE QUIERE VERIFICAR EN EL AMBIENTE LOCAL QUE EL ZIP SE GENERO CORRECTAMENTE
			//DESCOMENTAR TODAS LAS LINEAS DEL OBJETO: zoutFisico
			 //File zip = new
			 //File("C:/dev/workspace/bus/brokerLogs/"+zipFileName);
			 //ZipOutputStream zoutFisico = new ZipOutputStream(new FileOutputStream(zip));
			 //-------------------------------------------------

			// Stream para comprimir archivos
			ZipOutputStream zout = new ZipOutputStream(baos);

			String fileName; //nombre del archivo (imagen, pdf, scan)
			byte[] fileData; //cadena base64 de ese archivo
			ZipEntry zipEntry; //objeto zip entry para comprimir en el zip
			
			String idTipoArchivo = null;

			// iterar arreglo de archivos
			for (int i = 0; i < arrArchivos.length; i++) {

				//se asigna nombre del archivo
				fileName = arrArchivos[i].getFirstChild().getValueAsString();
				
				//se asigna el id del tipo de archivo
				idTipoArchivo = arrArchivos[i].getFirstChild().getNextSibling().getValueAsString();
				
				//aqui recibimos el blob decodificado por el ESQL Compute y se vuelve a convertir a arreglo de bytes
				fileData = (byte[]) arrArchivos[i].getLastChild().getValue();

				//se anade el nombre del archivo al objeto zip entry
				zipEntry = new ZipEntry(fileName);
				
				//se anade el objeto zip entry al arreglo de archivos para crear el zip
				zout.putNextEntry(zipEntry);
				//zoutFisico.putNextEntry(zipEntry);

				//AQUI SE CREA EL ARCHIVO ZIP Y SE GUARDA EN UN STREAM DE BYTES
				zout.write(fileData, 0, fileData.length);
				//zoutFisico.write(fileData, 0, fileData.length);
				
				//se cierra arreglo zip
				zout.closeEntry();
				//zoutFisico.closeEntry();
			}

			//se cierra stream
			zout.close();
			//zoutFisico.close();

			// Archivo zip sin codificar
			byte[] responseBytes = baos.toByteArray();

			//AQUI SE CREA EL CHECKSUM
			String md5 = FileZipper_JavaCompute.generateMD5CheckSum(responseBytes);
			
			//AQUI SE CODIFICA EL ARCHIVO ZIP EN FORMATO BASE64
			String zip64 = Base64.encode(responseBytes);

			//AQUI SE CREA EL OBJETO (NODO EN EL RESPONSE DEL JAVA COMPUTE DEL BUS)
			MbElement archivoZip = 	outAssembly.getMessage().getRootElement().getLastChild().createElementAsLastChild(MbElement.TYPE_NAME,
					"ArchivoZip",null);
			
			//NODO PARA ALMACENAR EL NOMBRE DEL ARCHIVO ZIP RESULTADO
			MbElement nombreArchivo = archivoZip.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,
					"nombreArchivo", zipFileName);
			//NODO PARA ALMACENAR LA CADENA BASE64 DEL ARCHIVO ZIP RESULTADO
			MbElement cadenaBase64 = archivoZip.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,
					"cadenaBase64", zip64);
			//NODO PARA ALMACENAR LA CADENA CON EL CHECKSUM DEL ARCHIVO ZIP RESULTADO
			MbElement md5CheckSum = archivoZip.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,
					"checksum", md5);
			//NODO PARA ALMACENAR EL ID DEL TIPO DE ARCHIVO ZIP RESULTADO
			MbElement tipoArchivo = archivoZip.createElementAsLastChild(MbElement.TYPE_NAME_VALUE,
					"idTipoArchivo", idTipoArchivo);
			
			
			//FIN
			
			// End of user code
			// ----------------------------------------------------------
		} catch (MbException e) {
			// Re-throw to allow Broker handling of MbException
			throw e;
		} catch (RuntimeException e) {
			// Re-throw to allow Broker handling of RuntimeException
			throw e;
		} catch (Exception e) {
			// Consider replacing Exception with type(s) thrown by user code
			// Example handling ensures all exceptions are re-thrown to be
			// handled in the flow
			throw new MbUserException(this, "evaluate()", "", "", e.toString(),
					null);
		}
		// The following should only be changed
		// if not propagating message to the 'out' terminal
		out.propagate(outAssembly);

	}

	public static String generateMD5CheckSum(byte[] input) {

		String md5CheckSum = null;

		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
			byte[] thedigest = md.digest(input);
			
			md5CheckSum = new BigInteger(1,thedigest).toString(16);
			
			int longCheckSum = md5CheckSum.length();
			while (longCheckSum < 32) {
				md5CheckSum = '0' + md5CheckSum;
				longCheckSum = md5CheckSum.length();
			}
			
		} catch (NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return md5CheckSum;
	}
	
	public static String getClaveDocumento(String nombreArchivo){
		String tipoTramite = null;
		
		//detectar el tipo de formato
		
		//extraer clave de documento
		
		String claveDoc = nombreArchivo.substring(0,4);
		
		
		
		return claveDoc;
	}

}
