
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

	public KeyValueHandler(String ipPort, List<ReplicaID> repLst) throws NumberFormatException, IOException {
		System.out.println("......................................................................");
		hints = new ConcurrentHashMap<String, List<Hint>>();
		replList = repLst;
		id = ipPort;
		ip = ipPort.split(":")[0];
		port = Integer.parseInt(ipPort.split(":")[1]);
		store = new ConcurrentHashMap<Integer, ValueWithTimestamp>();

		
		File log = new File(id);
		if (log.exists()) {
			FileReader reader = new FileReader(log);
			BufferedReader br = new BufferedReader(reader);

			String line;
			while ((line = br.readLine()) != null) {
				String[] entry = line.split(",", 3);
				System.out.println("retrieved from log " + entry[0] + "," + entry[2] + "," + entry[1]);

				store.put(Integer.parseInt(entry[0]), new ValueWithTimestamp(entry[2], Timestamp.valueOf(entry[1])));
			}
			br.close();
		} else {
			if (!(log.createNewFile())) {
				System.err.println("ERROR: File cannot be created.");
				System.exit(0);
			}
		}
	}

	@Override
	public String get(ReadOrWriteRequest request, int key, ReplicaID replicaID) throws SystemException, TException {
		String returnValue = "";
		if (false) {// isHintedHandoff
			if (hints.containsKey(replicaID.getId())) {
				performHintedHandoff(replicaID);
			}
			if (store.get(key) != null) {
				returnValue = store.get(key).timestamp + "," + store.get(key);
			}
		} else {
			if (request.isIsCoordinator()) {
				returnValue = readFromAllReplicas(key, request);
			} else {
				if (store.get(key) != null) {
					returnValue = store.get(key).timestamp + "," + store.get(key).value;
				}
			}
		}

		return returnValue;
	}

	@Override
	public boolean put(ReadOrWriteRequest request, int key, String value, ReplicaID replicaID)
			throws SystemException, TException {
		Timestamp timestamp;
		ValueWithTimestamp oldValue = store.get(key);
		try {
			if (request.isSetTimestamp()) {
				timestamp = Timestamp.valueOf(request.getTimestamp());
			} else {
				timestamp = new Timestamp(System.currentTimeMillis());
			}
			int count = 0;
			writeToLog(key, new ValueWithTimestamp(value, timestamp));
			if (request.isIsCoordinator()) {
				boolean isSuccessfull = sendRequestToAllReplicas(count, key, value,
						request.setIsCoordinator(false).setTimestamp(timestamp.toString()));
				if (!isSuccessfull) {
					writeToLog(key, oldValue);
					store.put(key, oldValue);
				}
				return isSuccessfull;
			}
		} catch (IOException e) {
			throw new SystemException();
		}
		if (hints.containsKey(replicaID.getId())) {
			performHintedHandoff(replicaID);
		}

		if (oldValue == null || oldValue.timestamp.before(timestamp)) {
			ValueWithTimestamp valueWithTimestamp = new ValueWithTimestamp(value, timestamp);
			store.put(key, valueWithTimestamp);
		}
		return true;
	}

	private void performHintedHandoff(ReplicaID replicaID) {
		List<Hint> listOfHints = hints.get(replicaID.getId());
		for (Hint hint : listOfHints) {
			TTransport tTransport = new TSocket(replicaID.getIp(), replicaID.getPort());
			try {
				tTransport.open();
				TProtocol tProtocol = new TBinaryProtocol(tTransport);
				ReplicatedKeyValueStore.Client client = new ReplicatedKeyValueStore.Client(tProtocol);
				client.put(hint.request, hint.key, hint.value, new ReplicaID().setIp(ip).setPort(port).setId(id));
				tTransport.close();
			} catch (SystemException e) {
				e.printStackTrace();
			} catch (TException e) {
				e.printStackTrace();
			}
		}
		hints.remove(replicaID.getId());

	}

	private boolean sendRequestToAllReplicas(int count, int key, String value, ReadOrWriteRequest request) {
		int consistencyLevel = 0;
		// int i = 0;
		boolean writeSuccessful = false;
		if (request.getConsistencyLevel() == ConsistencyLevel.ONE) {
			consistencyLevel = 1;
		} else {
			consistencyLevel = 2;
		}
		for (ReplicaID replicaID : replList) {

			String temp = replicaID.getIp() + ":" + replicaID.getPort();
			if (temp.equals(id)) {
				continue;
			}

			try {
				TTransport tTransport = new TSocket(replicaID.getIp(), replicaID.getPort());
				tTransport.open();
				TProtocol tProtocol = new TBinaryProtocol(tTransport);
				ReplicatedKeyValueStore.Client client = new ReplicatedKeyValueStore.Client(tProtocol);
				if (client.put(request, key, value, new ReplicaID().setIp(ip).setPort(port).setId(id))) {
					count += 1;
				}
				if (count >= consistencyLevel) {
					writeSuccessful = true;
				}
				tTransport.close();
			} catch (TTransportException e) {
				System.out.println("Could not connect to server " + replicaID.getPort());
				storeHintsLocally(replicaID, key, value, request);
			} catch (SystemException e) {
				System.out.println("SystemException: " + e.getMessage());
			} catch (TException e) {
				System.out.println("TException: " + e.getMessage());
			}
		}
		return writeSuccessful;
	}

	private void storeHintsLocally(ReplicaID replicaID, int key, String value, ReadOrWriteRequest request) {
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

	private void writeToLog(int key, ValueWithTimestamp value) throws IOException {
		BufferedWriter bw = new BufferedWriter(new FileWriter(id, true));
		String logLine = key + "," + value.timestamp + "," + value.value;
		bw.write(logLine);
		bw.newLine();
		bw.close();
	}

	/**
	 * Read data from all Replicas.And if configured start the read repair
	 */
	private String readFromAllReplicas(int key, ReadOrWriteRequest request) {
		int consistencyLevel = 0;
		List<ValueWithTimestamp> valueList = new ArrayList<ValueWithTimestamp>();
		Map<ReplicaID, ValueWithTimestamp> readRepairList = new HashMap<ReplicaID, ValueWithTimestamp>();

		String result = null;

		if (request.getConsistencyLevel() == ConsistencyLevel.ONE) {
			consistencyLevel = 1;
		} else {
			consistencyLevel = 2;
		}
		if (store.get(key) != null) {
			valueList.add(store.get(key));
		}
		System.out.println("valueList soze " + valueList.size());
		for (ReplicaID replicaID : replList) {
			try {
				if (valueList.size() >= consistencyLevel) {
					result = getUpdatedValue(valueList);
				}
				String temp = replicaID.getIp() + ":" + replicaID.getPort();
				if (temp.equals(id)) {
					// Don't read from itself
					readRepairList.put(replicaID, store.get(key));
					continue;
				}

				TTransport tTransport = new TSocket(replicaID.getIp(), replicaID.getPort());
				tTransport.open();
				TProtocol tProtocol = new TBinaryProtocol(tTransport);
				ReplicatedKeyValueStore.Client client = new ReplicatedKeyValueStore.Client(tProtocol);

				String val = client.get(request.setIsCoordinator(false), key, replicaID);
				String[] tsVal = val.split(",", 2);
				ValueWithTimestamp value = new ValueWithTimestamp(tsVal[1], java.sql.Timestamp.valueOf(tsVal[0]));
				System.out.println("bepf " + replicaID.getId());
				valueList.add(value);
				readRepairList.put(replicaID, value);

				tTransport.close();
			} catch (Exception e) {
				System.out.println("Error while reading replica: " + replicaID.getId());
			}

		}
		// Start this method in background.
		Runnable runnable = new Runnable() {
			@Override
			public void run() {
				try {
					startReadRepair(key, request, readRepairList);
				} catch (TException e) {
					System.out.println("ERROR in ReadRepair: " + e.getMessage());
				}
			}
		};
		(new Thread(runnable)).start();
		return result;
	}

	/**
	 * Starts the readRepair mechanism
	 */
	private void startReadRepair(int key, ReadOrWriteRequest request, Map<ReplicaID, ValueWithTimestamp> readRepairList)
			throws SystemException, TException {
		ValueWithTimestamp newestValue = null;
		for (ValueWithTimestamp value : readRepairList.values()) {
			if (newestValue == null) {
				newestValue = value;
			} else {
				if (newestValue.timestamp.compareTo(value.timestamp) < 0) {
					newestValue = value;
				}
			}
		}

		for (Entry<ReplicaID, ValueWithTimestamp> entry : readRepairList.entrySet()) {
			ReplicaID replica = entry.getKey();
			ValueWithTimestamp value = entry.getValue();
			if ((newestValue != null) && (newestValue.timestamp.compareTo(value.timestamp) > 0)) {
				// Update value on that replica.
				TTransport tTransport = new TSocket(replica.getIp(), replica.getPort());
				tTransport.open();
				TProtocol tProtocol = new TBinaryProtocol(tTransport);
				ReplicatedKeyValueStore.Client client = new ReplicatedKeyValueStore.Client(tProtocol);
				client.put(request, key, newestValue.value, new ReplicaID().setIp(ip).setPort(port).setId(id));
				tTransport.close();
			}
		}
	}

	/**
	 * Get the recent value from value list.
	 * 
	 * @param valueList
	 * @return
	 */
	private String getUpdatedValue(List<ValueWithTimestamp> valueList) {
		ValueWithTimestamp newestValue = valueList.get(0);
		for (ValueWithTimestamp value : valueList) {
			if (newestValue.timestamp.compareTo(value.timestamp) < 0) {
				newestValue = value;
			}
		}
		return newestValue.timestamp + "," + newestValue.value;
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
