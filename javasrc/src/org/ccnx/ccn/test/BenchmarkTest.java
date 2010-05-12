/**
 * A CCNx library test.
 *
 * Copyright (C) 2010 Palo Alto Research Center, Inc.
 *
 * This work is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation. 
 * This work is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details. You should have received a copy of the GNU General Public
 * License along with this program; if not, write to the
 * Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor,
 * Boston, MA 02110-1301, USA.
 */

package org.ccnx.ccn.test;

import java.security.DigestOutputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Random;

import org.ccnx.ccn.CCNHandle;
import org.ccnx.ccn.KeyManager;
import org.ccnx.ccn.config.SystemConfiguration;
import org.ccnx.ccn.config.UserConfiguration;
import org.ccnx.ccn.impl.CCNNetworkManager;
import org.ccnx.ccn.impl.CCNFlowControl.SaveType;
import org.ccnx.ccn.impl.security.crypto.CCNDigestHelper;
import org.ccnx.ccn.impl.security.crypto.util.SignatureHelper;
import org.ccnx.ccn.impl.support.Tuple;
import org.ccnx.ccn.io.NoMatchingContentFoundException;
import org.ccnx.ccn.io.NullOutputStream;
import org.ccnx.ccn.io.content.CCNStringObject;
import org.ccnx.ccn.profiles.SegmentationProfile;
import org.ccnx.ccn.profiles.VersioningProfile;
import org.ccnx.ccn.protocol.ContentName;
import org.ccnx.ccn.protocol.ContentObject;
import org.ccnx.ccn.protocol.Interest;
import org.junit.BeforeClass;
import org.junit.Test;


/**
 * This is not a unit test designed to verify functionality.
 * Instead, this test times some operations for basic benchmarking.
 * @author jthornto
 *
 */
public class BenchmarkTest {

	public static final int NUM_ITER = 1000;
	public static final int NUM_KEYGEN = 100; // Key generation is really slow
	
	public static final double NanoToMilli = 1000000.0d;
	
	public static CCNTestHelper testHelper = new CCNTestHelper(BenchmarkTest.class);
	public static CCNHandle handle;

	public static ContentName testName;
	public static byte[] shortPayload;
	public static byte[] longPayload;

	public abstract class Operation<T> {
		abstract Object execute(T input) throws Exception;
		
		int size(T input) {
			if (null == input) {
				return -1;
			} else if (input instanceof byte[]) {
				return ((byte[])input).length;
			} else if (input instanceof ContentObject) {
				return ((ContentObject)input).content().length;
			} else {
				throw new RuntimeException("Unsupported input type " + input.getClass());
			}
		}
	}
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		ContentName namespace = testHelper.getTestNamespace("benchmarkTest");
		testName = ContentName.fromNative(namespace, "BenchmarkObject");
		testName = VersioningProfile.addVersion(testName);
		shortPayload = ("this is sample segment content").getBytes();
		longPayload = new byte[1000];
		Random rnd = new Random();
		rnd.nextBytes(longPayload);
		handle = CCNHandle.open();
		System.out.println("Benchmark Test starting on " + System.getProperty("os.name"));
	}

	@SuppressWarnings("unchecked")
	public void runBenchmark(String desc, Operation op, Object input) throws Exception {
		runBenchmark(NUM_ITER, desc, op, input);
	}
	
	@SuppressWarnings("unchecked")
	public void runBenchmark(int count, String desc, Operation op, Object input) throws Exception {
		long start = System.nanoTime();
		op.execute(input);
		long dur = System.nanoTime() - start;
		//System.out.println("Start " + start + " dur " + dur);
		int size = op.size(input);
		System.out.println("Initial time to " + desc + (size >= 0 ? " (payload " + op.size(input) + " bytes)" : "") + " = " + dur/NanoToMilli + " ms.");
		
		start = System.nanoTime();
		for (int i=0; i<count; i++) {
			op.execute(input);
		}
		dur = System.nanoTime() - start;
		System.out.println("Avg. to " + desc + " (" + count + " iterations) = " + dur/count/NanoToMilli + " ms.");		
	}
	@Test
	public void testDigest() throws Exception {
		System.out.println("==== Digests");
		Operation<byte[]> digest = new Operation<byte[]>() {
			Object execute(byte[] input) throws Exception {
				MessageDigest md = MessageDigest.getInstance(CCNDigestHelper.DEFAULT_DIGEST_ALGORITHM);
				md.update(input);
				return md.digest();
			}
		};
		System.out.println("--- Raw = digest only of byte[]");
		runBenchmark("raw digest short", digest, shortPayload);
		runBenchmark("raw digest long", digest, longPayload);
		ContentName segment = SegmentationProfile.segmentName(testName, 0);
		
		Operation<ContentObject> digestObj = new Operation<ContentObject>() {
			Object execute(ContentObject input) throws Exception {
				MessageDigest md = MessageDigest.getInstance(CCNDigestHelper.DEFAULT_DIGEST_ALGORITHM);
				DigestOutputStream dos = new DigestOutputStream(new NullOutputStream(), md);
				input.encode(dos);
				return md.digest();
			}
		};
		System.out.println("--- Object = digest of ContentObject");
		ContentObject shortObj = ContentObject.buildContentObject(segment, shortPayload, null, null, SegmentationProfile.getSegmentNumberNameComponent(0));
		ContentObject longObj = ContentObject.buildContentObject(segment, longPayload, null, null, SegmentationProfile.getSegmentNumberNameComponent(0));
		runBenchmark("obj digest short", digestObj, shortObj);
		runBenchmark("obj digest long", digestObj, longObj);
	}
		
	@Test
	public void testRawSigning() throws Exception {
		
		Operation<byte[]> sign = new Operation<byte[]>() {
			KeyManager keyManager = KeyManager.getDefaultKeyManager();
			PrivateKey signingKey = keyManager.getDefaultSigningKey();

			Object execute(byte[] input) throws Exception {
				return SignatureHelper.sign(CCNDigestHelper.DEFAULT_DIGEST_ALGORITHM, input, signingKey);
			}
		};
		System.out.println("==== PK Signing");
		runBenchmark("sign short", sign, shortPayload);
		runBenchmark("sign long", sign, longPayload);	
		
		byte[] sigShort = (byte[])sign.execute(shortPayload);
		byte[] sigLong = (byte[])sign.execute(longPayload);
		
		Operation<Tuple<byte[],byte[]>> verify = new Operation<Tuple<byte[],byte[]>>() {
			KeyManager keyManager = KeyManager.getDefaultKeyManager();
			PublicKey pubKey = keyManager.getDefaultPublicKey();
			
			Object execute(Tuple<byte[],byte[]> input) throws Exception {
				return SignatureHelper.verify(input.first(), input.second(), CCNDigestHelper.DEFAULT_DIGEST_ALGORITHM, pubKey);
			}
			
			int size(Tuple<byte[], byte[]> input) {
				return input.first().length;
			}
		};
		
		System.out.println("==== PK Verifying");
		runBenchmark("verify short", verify, new Tuple<byte[],byte[]>(shortPayload, sigShort));
		runBenchmark("verify long", verify, new Tuple<byte[],byte[]>(longPayload, sigLong));

	}
	
	@Test
	public void testKeyGen() throws Exception {
		final KeyPairGenerator kpg = KeyPairGenerator.getInstance(UserConfiguration.defaultKeyAlgorithm());
		kpg.initialize(UserConfiguration.defaultKeyLength());

		Operation<Object> genpair = new Operation<Object>() {
			Object execute(Object input) throws Exception {
				KeyPair userKeyPair = kpg.generateKeyPair();
				return userKeyPair;
			}		
		};	
		
		System.out.println("==== Key Generation: " + UserConfiguration.defaultKeyLength() + "-bit " + UserConfiguration.defaultKeyAlgorithm() + " key.");
		runBenchmark(NUM_KEYGEN, "generate keypair", genpair, null);
	}
	
	@Test
	public void testCcndRetrieve() throws Exception {
		// Floss some content into ccnd
		ContentName dataPrefix = testHelper.getTestNamespace("TestCcndRetrieve");

		Flosser floss = new Flosser(dataPrefix);
		CCNStringObject so = new CCNStringObject(dataPrefix, "This is the value", SaveType.RAW, handle);
		so.save();
		ContentName name = so.getVersionedName();
		so.close();
		floss.stop();
		
		// Now that content is in local ccnd, we can benchmark retrieval of one content item
		Operation<Interest> getcontent = new Operation<Interest>() {
			Object execute(Interest interest) throws Exception {
				// Note as of this writing, interest refresh was PERIOD*2 with no constant in net mgr
				// We will use PERIOD for now, as we want to be sure to avoid refreshes and this should be fast.
				ContentObject result = handle.get(interest, CCNNetworkManager.PERIOD);
				// Make sure to throw exception if we get nothing back so this doesn't just 
				// look like a long successful run.
				if (null == result) {
					throw new NoMatchingContentFoundException("timeout on get for " + interest.name());
				}
				return null;
			}
			
			int size(Interest interest) {
				return -1;
			}
		};
		Interest interest = new Interest(name);
		System.out.println("==== Single data retrieval from ccnd: " + name);
		runBenchmark("retrieve data", getcontent, interest);
	}
}