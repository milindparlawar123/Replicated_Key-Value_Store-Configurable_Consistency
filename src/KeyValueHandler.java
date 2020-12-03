
import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Map.Entry;
import java.sql.Timestamp;
import java.util.concurrent.ConcurrentHashMap;

public class KeyValueHandler implements ReplicatedKeyValueStore.Iface {
	private List<ReplicaID> replList;
	private String id;
	private int port;
	private String ip;

	private Map<String, List<Hint>> hints;
	private Map<Integer, ValueWithTimestamp> store;
	private boolean isHintedHandOff = false;

	public KeyValueHandler(boolean hinted, String id, String ipPort, List<ReplicaID> repLst)
			throws NumberFormatException, IOException {
		System.out.println("......................................................................");
		// System.out.println(hints);
		hints = new ConcurrentHashMap<String, List<Hint>>();
		replList = repLst;
		this.id = id;
		// System.out.println("con st "+id);
		ip = ipPort.split(":")[0];
		port = Integer.parseInt(ipPort.split(":")[1]);
		store = new ConcurrentHashMap<Integer, ValueWithTimestamp>();
		isHintedHandOff = hinted;

		File logFile = new File(id);
		if (logFile.exists()) {
			FileReader reader = new FileReader(logFile);
			BufferedReader br = new BufferedReader(reader);

			String line;
			while ((line = br.readLine()) != null) {
				String[] entry = line.split(",", 3);

				String s0 = entry[0];
				String s1 = entry[1];
				String s2 = entry[2];
				System.out.println("loaded from system logs file " + s0 + "," + s2 + "," + s1);
				Timestamp timestamp = Timestamp.valueOf(s1);
				store.put(Integer.parseInt(s0), new ValueWithTimestamp(s2, timestamp));
			}
			br.close();
		} else {
			// else create new file
			if (!(logFile.createNewFile())) {

				System.err.println("error occurred while creating the file.");

				System.exit(0);
			}
		}
	}

	@Override
	public String get(ReadOrWriteRequest request, int key, ReplicaID replicaID) throws SystemException, TException {
		String returnValue = "";
		if (isHintedHandOff) {// isHintedHandoff
			// milind
			int count = 0;
			for (ReplicaID replID : replList) {

				// String temp = replID.getIp() + ":" + replID.getPort();
				if (replID.getId().equals(id)) {
					count++;
					continue;
				}

				try {
					TTransport tTransport = new TSocket(replID.getIp(), replID.getPort());
					tTransport.open();
					TProtocol tProtocol = new TBinaryProtocol(tTransport);
					ReplicatedKeyValueStore.Client client = new ReplicatedKeyValueStore.Client(tProtocol);
					client.put(request, key, "", new ReplicaID().setIp(ip).setPort(port).setId(id), true, false);
					tTransport.close();
					count++;
				} catch (TTransportException e) {
					System.out.println("Could not connect to server " + replID.getPort());
					// storeHintsLocally(replID, key, value, request);
				} catch (SystemException e) {
					System.out.println("SystemException");
					System.out.println(e.getMessage());
				} catch (TException e) {
					System.out.println("TException");
					System.out.println(e.getMessage());
				}
			}
			int consistencyLevel = 0;
			if (request.getConsistencyLevel() == ConsistencyLevel.QUORUM) {
				consistencyLevel = 2;
			} else {
				consistencyLevel = 1;
			}
			if (count < consistencyLevel) {
				return "system exception - not enough replica servers available";
			}
			// milind
			if (store.get(key) != null) {
				returnValue = store.get(key).timestamp + "," + store.get(key).value;
			}
		} else {
			if (request.isIsCoordinator()) {
				returnValue = readFrmAllNodeReplicas(key, request);
			} else {
				if (store.get(key) != null) {
					returnValue = store.get(key).timestamp + "," + store.get(key).value;
				}
			}
		}

		return returnValue;
	}

	@Override
	public boolean put(ReadOrWriteRequest request, int key, String value, ReplicaID replicaID, boolean flag,
			boolean isFromHand) throws SystemException, TException {
		// System.out.println("put st "+id);
		if (flag && isHintedHandOff) {
			if (hints.containsKey(replicaID.getId())) {
				performHintedHandoff(replicaID);
			}
			return true;
		} else {
			// performHintedHandoffForPut(replicaID);
		}
		Timestamp timestamp;
		ValueWithTimestamp oldDataVal = store.get(key);
		try {
			if (request.isSetTimestamp()) {
				timestamp = Timestamp.valueOf(request.getTimestamp());
			} else {
				timestamp = new Timestamp(System.currentTimeMillis());
			}
			int count = 0;

			writeToSystemLogs(key, new ValueWithTimestamp(value, timestamp));

			store.put(key, new ValueWithTimestamp(value, timestamp));
			if (request.isIsCoordinator()) {
				// System.out.println("inside put " + request.isIsCoordinator());
				boolean isSuccessfull = sendReqToAllReplNodes(count + 1, key, value,
						request.setIsCoordinator(false).setTimestamp(timestamp.toString()));
				//
				if (!isSuccessfull) {

					writeToSystemLogs(key, oldDataVal);
					// System.out.println("inside put isSuccessfull "+ isSuccessfull);
					store.put(key, oldDataVal);
				}
				return isSuccessfull;
			} else {

				// System.out.println("inside put else");
				if (isHintedHandOff && isFromHand) {
					// System.out.println("in isFromHand ");
					performHintedHandoff(replicaID);
				}
				/*
				 * ReplicaID reId=null; for (ReplicaID replID : replList) {
				 * 
				 * String temp = replID.getIp() + ":" + replID.getPort(); if (temp.equals(id)) {
				 * reId=replID; break; } }
				 * 
				 * performHintedHandoff(reId);
				 */}
		} catch (IOException e) {
			throw new SystemException();
		}
		// milind
		/*
		 * if (hints.containsKey(replicaID.getId())) { performHintedHandoff(replicaID);
		 * }
		 */
		// milind

		if (oldDataVal == null || oldDataVal.timestamp.before(timestamp)) {

			ValueWithTimestamp valueWithTimestamp = new ValueWithTimestamp(value, timestamp);

			store.put(key, valueWithTimestamp);
		}
		return true;
	}

	private void performHintedHandoff(ReplicaID replicaID) {
		List<Hint> listOfHints = hints.get(replicaID.getId());
		// System.out.println(" listOfHints s");
		if (listOfHints != null) {
			// System.out.println(" listOfHints s"+listOfHints.size());

		}
		if (listOfHints != null) {

			for (Hint hint : listOfHints) {

				TTransport tTransport = new TSocket(replicaID.getIp(), replicaID.getPort());
				// System.out.println("performHintedHandoff "+replicaID.getIp()+" "+
				// replicaID.getPort() + " "+hint.key + " "+hint.value);
				// System.out.println(" "+ip +" ");
				try {
					tTransport.open();
					TProtocol tProtocol = new TBinaryProtocol(tTransport);
					ReplicatedKeyValueStore.Client client = new ReplicatedKeyValueStore.Client(tProtocol);
					hint.request.setIsCoordinator(false);
					ReplicaID temp = new ReplicaID();
					temp.setIp(ip);
					temp.setPort(port);
					temp.setId(id);
					client.put(hint.request, hint.key, hint.value, temp, false, false);
					tTransport.close();
				} catch (SystemException e) {
					e.printStackTrace();
				} catch (TException e) {
					e.printStackTrace();
				}
			}
			hints.remove(replicaID.getId());
		}
	}

	private boolean sendReqToAllReplNodes(int count, int key, String value, ReadOrWriteRequest request) {
		int consistencyLevel = 0;
		// int i = 0;
		boolean successWrite = false;
		if (request.getConsistencyLevel() == ConsistencyLevel.ONE) {
			consistencyLevel = 1;
		} else {
			consistencyLevel = 2;
		}

		if (count >= consistencyLevel) {
			successWrite = true;
		}
		for (ReplicaID replicaID : replList) {

			// String temp = replicaID.getIp() + ":" + replicaID.getPort();
			// System.out.println(replicaID.getId() + " "+ id);
			if (replicaID.getId().equals(id)) {
				// System.out.println("true");

				continue;
			}

			try {
				TTransport tTransport = new TSocket(replicaID.getIp(), replicaID.getPort());
				tTransport.open();
				TProtocol tProtocol = new TBinaryProtocol(tTransport);
				ReplicatedKeyValueStore.Client client = new ReplicatedKeyValueStore.Client(tProtocol);
				ReplicaID temp = new ReplicaID();
				temp.setIp(ip);
				temp.setPort(port);
				temp.setId(id);
				if (client.put(request, key, value, temp, false, true)) {
					count += 1;
				}
				if (count >= consistencyLevel) {
					successWrite = true;
				}
				tTransport.close();
			} catch (TTransportException e) {
				System.out.println("Could not connect to server " + replicaID.getPort());
				// below code is to store hints locally
				if (successWrite) {
					Hint hint = new Hint(replicaID, key, value, request);
					List<Hint> list;
					if (hints.containsKey(replicaID.getId())) {

						list = hints.get(replicaID.getId());
					} else {

						list = new ArrayList<Hint>();
					}
					list.add(hint);

					hints.put(replicaID.getId(), list);
				}

			} catch (SystemException e) {
				System.out.println("SystemException");
				System.out.println(e.getMessage());
			} catch (TException e) {
				System.out.println("TException ");
				System.out.println(e.getMessage());
			}

		}
		return successWrite;
	}

	private void writeToSystemLogs(int key, ValueWithTimestamp value) throws IOException {
		// System.out.println("id "+id);
		FileWriter fileWriter = new FileWriter(id, true);
		BufferedWriter bw = new BufferedWriter(fileWriter);

		String line = key + "," + value.timestamp + "," + value.value;

		bw.write(line);

		bw.newLine();

		bw.close();
	}

	// read from replicas
	private String readFrmAllNodeReplicas(int key, ReadOrWriteRequest request) {
		// System.out.println("inside");
		List<ValueWithTimestamp> valueList = new ArrayList<ValueWithTimestamp>();

		String result = null;
		int consistencyLevel = 0;
		if (request.getConsistencyLevel() == ConsistencyLevel.QUORUM) {
			consistencyLevel = 2;
		} else {
			consistencyLevel = 1;
		}
		if (store.get(key) != null) {
			valueList.add(store.get(key));
			result = store.get(key).timestamp + "," + store.get(key).value;
		}
		// System.out.println("valueList soze " + valueList.size());
		for (ReplicaID replicaID : replList) {
			try {
				// System.out.println("RESUL "+result);
				if (valueList.size() < consistencyLevel) {
					// System.out.println("count "+consistencyLevel +" v "+valueList.size());
					result = getUpdatedValFromValList(valueList);
				}
				// String temp = replicaID.getIp() + ":" + replicaID.getPort();
				if (replicaID.getId().equals(id)) {

					continue;
				}

				TTransport tTransport = new TSocket(replicaID.getIp(), replicaID.getPort());
				tTransport.open();
				TProtocol tProtocol = new TBinaryProtocol(tTransport);
				ReplicatedKeyValueStore.Client client = new ReplicatedKeyValueStore.Client(tProtocol);

				String val = client.get(request.setIsCoordinator(false), key, replicaID);
				String[] vals = val.split(",", 2);
				String val1 = vals[1];
				String val0 = vals[0];
				Timestamp timstamp = java.sql.Timestamp.valueOf(val0);
				ValueWithTimestamp value = new ValueWithTimestamp(val1, timstamp);
				// System.out.println("bepf " + replicaID.getId());
				valueList.add(value);

				tTransport.close();
			} catch (Exception e) {
				System.out.println("exception occured while reading replica  " + replicaID.getId());
			}

		}
		return result;
	}

	// below method is to find recent value
	private String getUpdatedValFromValList(List<ValueWithTimestamp> valueList) {
		//
		ValueWithTimestamp newestValue = valueList.get(0);
		String retStr = "";
		for (int i = 0; i < valueList.size(); i++) {
			// System.out.println("inside");
			if (newestValue.timestamp.compareTo(valueList.get(i).timestamp) < 0) {
				newestValue = valueList.get(i);
			}
		}
		retStr = newestValue.timestamp + "," + newestValue.value;

		return retStr;
	}

}

//below class is for storing hints
class Hint {

	ReplicaID replicaID;
	int key;
	String value;
	ReadOrWriteRequest request;

	public Hint(ReplicaID replicaID, int key, String value, ReadOrWriteRequest request) {
		this.replicaID = replicaID;
		this.key = key;
		this.value = value;
		this.request = request;
	}
}

//below class is for storing value with timestamp
class ValueWithTimestamp {

	String value;
	Timestamp timestamp;

	public ValueWithTimestamp(String value, Timestamp timestamp) {
		this.value = value;
		this.timestamp = timestamp;
	}

}
