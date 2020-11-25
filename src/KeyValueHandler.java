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

import org.apache.thrift.TException;
import org.apache.thrift.transport.TTransportException;
import org.apache.thrift.transport.TTransport;
import org.apache.thrift.transport.TSocket;
import org.apache.thrift.protocol.TProtocol;
import org.apache.thrift.protocol.TBinaryProtocol;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
// Generated code
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
public class KeyValueHandler implements ReplicatedKeyValueStore.Iface {
	private List<ReplicaID> replList;
	private String id; 
	private int port;
	private String ip;
	private Map<Integer,Map<String, String>> store;
	private Map<String,List<Hint>> hints;
	public KeyValueHandler(String ipPort,  List<ReplicaID> repLst) {
		// TODO Auto-generated constructor stub
		replList=repLst;
		id=ipPort;
		ip=ipPort.split(":")[0];
		port=Integer.parseInt( ipPort.split(":")[1]);
		store = new ConcurrentHashMap<Integer, Map<String, String>>();
		hints = new ConcurrentHashMap<String, List<Hint>>();
		System.out.println("......................................................................");
	}
	@Override
	public String get(ReadOrWriteRequest request,int key,  ReplicaID replicaID) throws SystemException, TException {
		return "";
	}
	
	@Override
	public boolean put(ReadOrWriteRequest request,int key, String value,  ReplicaID replicaID) throws SystemException, TException {
		return true;
	}


}
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
