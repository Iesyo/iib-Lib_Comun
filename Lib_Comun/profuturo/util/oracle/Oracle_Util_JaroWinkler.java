package profuturo.util.oracle;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

import com.ibm.broker.javacompute.MbJavaComputeNode;
import com.ibm.broker.plugin.MbElement;
import com.ibm.broker.plugin.MbException;
import com.ibm.broker.plugin.MbMessage;
import com.ibm.broker.plugin.MbMessageAssembly;
import com.ibm.broker.plugin.MbOutputTerminal;
import com.ibm.broker.plugin.MbUserException;
import com.ibm.broker.plugin.MbXMLNSC;

public class Oracle_Util_JaroWinkler extends
		MbJavaComputeNode {

	public void evaluate(MbMessageAssembly inAssembly) throws MbException {
		MbOutputTerminal out = getOutputTerminal("out");
		MbOutputTerminal alt = getOutputTerminal("alternate");

		MbMessage inMessage = inAssembly.getMessage();

		// create new empty message
		MbMessage outMessage = new MbMessage();
		MbMessageAssembly outAssembly = new MbMessageAssembly(inAssembly,
				outMessage);
		
		Connection conn = null;
		PreparedStatement pstmt = null;
		ResultSet rs = null;

		try {
			// optionally copy message headers
			copyMessageHeaders(inMessage, outMessage);
			// ----------------------------------------------------------
			// Add user code below
			MbElement inRoot = inMessage.getRootElement();
			MbElement outRoot = outMessage.getRootElement();
			MbElement outBody = outRoot.createElementAsLastChild("XMLNSC");
			MbElement res = outBody.createElementAsFirstChild(MbXMLNSC.FOLDER, "res", null);
			
			String DS = (String)getUserDefinedAttribute("DATA_SOURCE");
			
			String qry = "SELECT UTL_MATCH.jaro_winkler(?, ?) PORC FROM DUAL";
			
			conn = getJDBCType4Connection(DS, JDBC_TransactionType.MB_TRANSACTION_AUTO);

			pstmt = conn.prepareStatement(qry);
			pstmt.setString(1, (String) inRoot.evaluateXPath("string(/params/param1)"));	
			pstmt.setString(2, (String) inRoot.evaluateXPath("string(/params/param2)"));
			
			rs = pstmt.executeQuery();
			while (rs.next()) {
				MbElement porcentaje = res.createElementAsFirstChild(MbXMLNSC.FOLDER, "porcentaje", null);

				porcentaje.createElementAsLastChild(MbXMLNSC.FIELD, "valor", rs.getString("PORC"));
			}
			
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
			// Example handling ensures all exceptions are re-thrown to be handled in the flow
			throw new MbUserException(this, "evaluate()", "", "", e.toString(),
					null);
		} finally {
			try {
				rs.close();
				pstmt.close();
				conn.close();
			}catch(Exception e) {rs = null; pstmt = null; conn = null;
				throw new MbUserException(this, "finally", "", "", e.toString(),
					null);};
		}
		// The following should only be changed
		// if not propagating message to the 'out' terminal
		out.propagate(outAssembly);
	}

	public void copyMessageHeaders(MbMessage inMessage, MbMessage outMessage)
			throws MbException {
		MbElement outRoot = outMessage.getRootElement();

		// iterate though the headers starting with the first child of the root
		// element
		MbElement header = inMessage.getRootElement().getFirstChild();
		while (header != null && header.getNextSibling() != null) // stop before
																	// the last
																	// child
																	// (body)
		{
			// copy the header and add it to the out message
			outRoot.addAsLastChild(header.copy());
			// move along to next header
			header = header.getNextSibling();
		}
	}

}
