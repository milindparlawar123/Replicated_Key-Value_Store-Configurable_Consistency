/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

// Generated code
import java.util.List;
import java.util.ArrayList;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.thrift.TException;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.apache.thrift.protocol.TProtocol;

public class JavaClient {
	public static void main(String[] args) {

		List<ReplicaID> replList = getReplicasFromSystem("replicaNodes.txt");
		TTransport transport = null;

		while (true) {
			try {
				BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
				System.out.println("select coordinator ");
				for (int i = 0; i < replList.size(); i++) {
					System.out.println("enter " + i + " for  " + replList.get(i).getId() + " " + replList.get(i).getIp()
							+ " " + replList.get(i).getPort());
				}
				int choice = Integer.parseInt(br.readLine());
				System.out.println(" selected coordinator is  " + replList.get(choice).getId() + " "
						+ replList.get(choice).getIp() + " " + replList.get(choice).getPort());
				ReplicaID coordinator = replList.get(choice);
				transport = new TSocket(coordinator.ip, coordinator.port);
				transport.open();
				TProtocol protocol = new TBinaryProtocol(transport);
				ReplicatedKeyValueStore.Client client = new ReplicatedKeyValueStore.Client(protocol);

				System.out.println("Enter request type : get or put");
				String reqType = br.readLine();
				System.out.println("Enter key");
				int reqKey = Integer.parseInt(br.readLine());
				String reqVal = "";
				if ("get".equalsIgnoreCase(reqType)) {

				} else {
					System.out.println("Enter value");
					reqVal = br.readLine();
				}
				System.out.println("Enter Consistency Level : ONE or QUORUM");
				String reqCL = br.readLine();

				ReadOrWriteRequest request = new ReadOrWriteRequest();
				request.setConsistencyLevel(ConsistencyLevel.valueOf(reqCL));
				request.setIsCoordinator(true);
				if (reqType.equalsIgnoreCase("get")) {
					String returnedValue = client.get(request, reqKey, new ReplicaID().setId("client"));
					if (returnedValue.equals("")) {
						System.out.println("The key either does not exist or has null value");
					} else {
						String[] value1 = returnedValue.split(",", 2);
						System.out.println("The value returned for key " + reqKey + " is " + value1[1]);
					}
				} else {
					if (!client.put(request, reqKey, reqVal, null)) {
						System.out.println("Write operation was not successful. Server might be down.");
					}
				}
				transport.close();

			} catch (TException e) {
				System.out.println("Error: Value could not be retrieved");
				transport.close();

			} catch (IOException e) {
				System.out.println("Error: Value could not be retrieved");
				transport.close();
			}
		}
	}

	public static List<ReplicaID> getReplicasFromSystem(String fName) {
		File file = new File(fName);
		BufferedReader fileReader = null;
		List<ReplicaID> rep = new ArrayList<ReplicaID>();
		try {
			fileReader = new BufferedReader(new FileReader(file));
			String l = null;
			while (null != (l = fileReader.readLine())) {
				String[] replica = l.split(" ");
				rep.add(createReplica(replica[0], replica[1], replica[2]));
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			// fileReader.close();
		}
		return rep;

	}

	public static ReplicaID createReplica(String id, String ip, String port) {
		ReplicaID replicaID = new ReplicaID();
		replicaID.id = id;
		replicaID.ip = ip;
		replicaID.port = Integer.parseInt(port);
		return replicaID;
	}
}
