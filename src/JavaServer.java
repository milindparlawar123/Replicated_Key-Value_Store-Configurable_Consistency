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

import org.apache.thrift.server.TServer;
import org.apache.thrift.server.TServer.Args;
import org.apache.thrift.server.TSimpleServer;
import org.apache.thrift.server.TThreadPoolServer;
import org.apache.thrift.transport.TSSLTransportFactory;
import org.apache.thrift.transport.TServerSocket;
import org.apache.thrift.transport.TServerTransport;
import org.apache.thrift.transport.TSSLTransportFactory.TSSLTransportParameters;

import java.net.InetAddress;

// Generated code

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.FileNotFoundException;
public class JavaServer {

	public static KeyValueHandler handler;

	public static ReplicatedKeyValueStore.Processor<ReplicatedKeyValueStore.Iface> processor ;

	public static int port;

	public static void main(String[] args) {
		try {
			handler = new KeyValueHandler(InetAddress.getLocalHost().getHostAddress() + ":" + args[0], getReplicasFromSystem("abc"));
			processor = new ReplicatedKeyValueStore.Processor(handler);
			port = Integer.valueOf(args[0]);
			Runnable simple = new Runnable() {
				public void run() {
					simple(processor);
				}
			};
			/*
			 * Runnable secure = new Runnable() { public void run() { secure(processor); }
			 * };
			 */

			new Thread(simple).start();
			// new Thread(secure).start();
		} catch (Exception x) {
			x.printStackTrace();
		}
	}

	public static void simple(ReplicatedKeyValueStore.Processor processor) {
		try {
			TServerTransport serverTransport = new TServerSocket(port);
			// TServer server = new TSimpleServer(new
			// Args(serverTransport).processor(processor));

			// Use this for a multithreaded server
			TServer server = new TThreadPoolServer(new TThreadPoolServer.Args(serverTransport).processor(processor));

			System.out.println("Started the simple server...");
			server.serve();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void secure(ReplicatedKeyValueStore.Processor processor) {
		try {
			/*
			 * Use TSSLTransportParameters to setup the required SSL parameters. In this
			 * example we are setting the keystore and the keystore password. Other things
			 * like algorithms, cipher suites, client auth etc can be set.
			 */
			TSSLTransportParameters params = new TSSLTransportParameters();
			// The Keystore contains the private key
			params.setKeyStore("/home/cs557-inst/thrift-0.13.0/lib/java/test/.keystore", "thrift", null, null);

			/*
			 * Use any of the TSSLTransportFactory to get a server transport with the
			 * appropriate SSL configuration. You can use the default settings if properties
			 * are set in the command line. Ex: -Djavax.net.ssl.keyStore=.keystore and
			 * -Djavax.net.ssl.keyStorePassword=thrift
			 * 
			 * Note: You need not explicitly call open(). The underlying server socket is
			 * bound on return from the factory class.
			 */
			TServerTransport serverTransport = TSSLTransportFactory.getServerSocket(port, 0, null, params);
			TServer server = new TSimpleServer(new Args(serverTransport).processor(processor));

			// Use this for a multi threaded server
			// TServer server = new TThreadPoolServer(new
			// TThreadPoolServer.Args(serverTransport).processor(processor));

			System.out.println("Starting the secure server...");
			server.serve();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static List<ReplicaID> getReplicasFromSystem(String fName) {
	  File file= new File(fName);
	  BufferedReader fileReader=null;
	  List<ReplicaID> rep = new ArrayList<ReplicaID>();
	  try {
		fileReader= new BufferedReader(new FileReader(file) );
		String l=null;
		while( null != (l=fileReader.readLine())) {
			String[] replica = l.split(" ");
			rep.add(createReplica(replica[0],replica[1], replica[2]));
		}
	} catch (FileNotFoundException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} catch (IOException e) {
		// TODO Auto-generated catch block
		e.printStackTrace();
	} finally{ 
		  //fileReader.close();
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
