/**
 * Part of the CCNx Java Library.
 *
 * Copyright (C) 2008, 2009 Palo Alto Research Center, Inc.
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 2.1
 * as published by the Free Software Foundation. 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. You should have received
 * a copy of the GNU Lesser General Public License along with this library;
 * if not, write to the Free Software Foundation, Inc., 51 Franklin Street,
 * Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.ccnx.ccn.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.HashMap;
import java.util.logging.Level;

import org.ccnx.ccn.impl.encoding.GenericXMLEncodable;
import org.ccnx.ccn.impl.encoding.XMLDecoder;
import org.ccnx.ccn.impl.encoding.XMLEncodable;
import org.ccnx.ccn.impl.encoding.XMLEncoder;
import org.ccnx.ccn.impl.security.crypto.util.CryptoUtil;
import org.ccnx.ccn.impl.support.DataUtils;
import org.ccnx.ccn.impl.support.Log;
import org.ccnx.ccn.io.content.ContentDecodingException;
import org.ccnx.ccn.io.content.ContentEncodingException;


/**
 * A KeyLocator specifies where a content consumer or intermediary can find the public key
 * necessary to verify a piece of content. It might include the key itself, a certificate
 * containing the key, or a CCN name pointing to a location where the key can be found.
 */
public class KeyLocator extends GenericXMLEncodable implements XMLEncodable {
	/**
	 * KeyLocator(name) must allow for a complete name -- i.e.
	 * a name and authentication information.
	 */
    public enum KeyLocatorType { NAME, KEY, CERTIFICATE }

    protected static final HashMap<KeyLocatorType, String> TypeNames = new HashMap<KeyLocatorType, String>();
    protected static final HashMap<String, KeyLocatorType> NameTypes = new HashMap<String, KeyLocatorType>();
    
    static {
        TypeNames.put(KeyLocatorType.NAME, "NAME");
        TypeNames.put(KeyLocatorType.KEY, "KEY");
        TypeNames.put(KeyLocatorType.CERTIFICATE, "CERTIFICATE");
        NameTypes.put("NAME", KeyLocatorType.NAME);
        NameTypes.put("KEY", KeyLocatorType.KEY);
        NameTypes.put("CERTIFICATE", KeyLocatorType.CERTIFICATE);
    }

    protected static final String KEY_LOCATOR_ELEMENT = "KeyLocator";
    protected static final String KEY_LOCATOR_TYPE_ELEMENT = "Type";
    protected static final String PUBLISHER_KEY_ELEMENT = "Key";
    protected static final String PUBLISHER_CERTIFICATE_ELEMENT = "Certificate";

    // Fake out a union.
    protected KeyName _keyName;       // null if wrong type
    protected PublicKey _key;
    protected X509Certificate _certificate;
    
    /**
     * Make a KeyLocator containing only a name
     * @param name the name
     */
    public KeyLocator(ContentName name) {
    	this (name, null);
    }
    
    /**
     * Make a KeyLocator containing a key name and the desired publisher
     * @param name the key name
     * @param publisher the desired publisher
     */
    public KeyLocator(ContentName name, PublisherID publisher) {
    	this(new KeyName(name, publisher));
    }
    
    /**
     * Make a KeyLocator specifying a KeyName -- a structure combining the name
     * at which to find the key and authentication information by which to verify it
     * @param keyName the KeyName to use to retrieve and authenticate the key
     */
   public KeyLocator(KeyName keyName) {
    	_keyName = keyName;
    }

   /**
    * Make a KeyLocator containing an explicit public key
    * @param key the key
    */
    public KeyLocator(PublicKey key) {
    	_key = key;
    }
    
    /**
     * Make a KeyLocator containing an explicit X509Certificate. 
     * @param certificate the certificate
     */
    public KeyLocator(X509Certificate certificate) {
    	_certificate = certificate;
    }
    
    /**
     * Internal constructor.
     * @param name
     * @param key
     * @param certificate
     */
    protected KeyLocator(KeyName name, PublicKey key, X509Certificate certificate) {
    	_keyName = name;
    	_key = key;
    	_certificate = certificate;
    }
    
    /**
     * Implement Cloneable
     */
    public KeyLocator clone() {
    	return new KeyLocator(name(),
    						  key(),
    						  certificate());
    }
    
    /**
     * For use by decoders.
     */
    public KeyLocator() {} 
    
    /**
 	 * Get the key if present
     * @return the key or null
     */
	public PublicKey key() { return _key; }
	
	/**
	 * Get the KeyName if present
	 * @return the KeyName or null
	 */
    public KeyName name() { return _keyName; }
    
    /**
     * Get the certificate if present
     * @return the certificate or null
     */
    public X509Certificate certificate() { return _certificate; }
    
    /**
     * Return the type of data stored in this KeyLocator - KEY, CERTIFICATE, or NAME.
     * @return the type
     */
    public KeyLocatorType type() { 
    	if (null != certificate())
    		return KeyLocatorType.CERTIFICATE;
    	if (null != key())
    		return KeyLocatorType.KEY;
    	return KeyLocatorType.NAME; 
    }

	@Override
	public int hashCode() {
		final int PRIME = 31;
		int result = 1;
		result = PRIME * result + ((_key == null) ? 0 : _key.hashCode());
		result = PRIME * result + ((_keyName == null) ? 0 : _keyName.hashCode());
		result = PRIME * result + ((_certificate == null) ? 0 : _certificate.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		final KeyLocator other = (KeyLocator) obj;
		if (_key == null) {
			if (other._key != null)
				return false;
		} else if (!_key.equals(other._key))
			return false;
		if (_keyName == null) {
			if (other.name() != null)
				return false;
		} else if (!_keyName.equals(other.name()))
			return false;
		return true;
	}

	@Override
	public void decode(XMLDecoder decoder) throws ContentDecodingException {
		decoder.readStartElement(getElementLabel());

		if (decoder.peekStartElement(PUBLISHER_KEY_ELEMENT)) {
			try {
				byte [] encodedKey = decoder.readBinaryElement(PUBLISHER_KEY_ELEMENT);
				// This is a DER-encoded SubjectPublicKeyInfo.
				_key = CryptoUtil.getPublicKey(encodedKey);
			} catch (CertificateEncodingException e) {
				Log.warning("Cannot parse stored key: error: " + e.getMessage());
				throw new ContentDecodingException("Cannot parse key: ", e);
			} catch (InvalidKeySpecException e) {
				Log.warning("Cannot turn stored key " + " into key of appropriate type.");
				throw new ContentDecodingException("Cannot turn stored key " + " into key of appropriate type.");
			}
			if (null == _key) {
				throw new ContentDecodingException("Cannot parse key: ");
			}
		} else if (decoder.peekStartElement(PUBLISHER_CERTIFICATE_ELEMENT)) {
			try {
				byte [] encodedCert = decoder.readBinaryElement(PUBLISHER_CERTIFICATE_ELEMENT);
				CertificateFactory factory = CertificateFactory.getInstance("X.509");
				_certificate = (X509Certificate) factory.generateCertificate(new ByteArrayInputStream(encodedCert));
			} catch (CertificateException e) {
				throw new ContentDecodingException("Cannot decode certificate: " + e.getMessage(), e);
			}
			if (null == _certificate) {
				throw new ContentDecodingException("Cannot parse certificate! ");
			}
		} else {
			_keyName = new KeyName();
			_keyName.decode(decoder);
		}
		decoder.readEndElement();
	}
	
	public byte [] getEncoded() {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			encode(baos);
		} catch (ContentEncodingException e) {
			Log.log(Level.WARNING, "This should not happen: cannot encode KeyLocator to byte array.");
			Log.warningStackTrace(e);
			// DKS currently returning invalid byte array...
		}
		return baos.toByteArray();
	}

	@Override
	public void encode(XMLEncoder encoder) throws ContentEncodingException {
		if (!validate()) {
			throw new ContentEncodingException("Cannot encode " + this.getClass().getName() + ": field values missing.");
		}
		encoder.writeStartElement(getElementLabel());
		if (type() == KeyLocatorType.KEY) {
			encoder.writeElement(PUBLISHER_KEY_ELEMENT, key().getEncoded());
		} else if (type() == KeyLocatorType.CERTIFICATE) {
			try {
				encoder.writeElement(PUBLISHER_CERTIFICATE_ELEMENT, certificate().getEncoded());
			} catch (CertificateEncodingException e) {
				Log.warning("CertificateEncodingException attempting to write key locator: " + e.getMessage());
				throw new ContentEncodingException("CertificateEncodingException attempting to write key locator: " + e.getMessage(), e);
			}
		} else if (type() == KeyLocatorType.NAME) {
			name().encode(encoder);
		}
		encoder.writeEndElement();   		
	}
	
	@Override
	public String getElementLabel() { return KEY_LOCATOR_ELEMENT; }
	
	/**
	 * Conversion methods for dealing with enums. Not necessary, and will
	 * be removed.
	 * @param type
	 * @return
	 */
	public static String typeToName(KeyLocatorType type) {
		return TypeNames.get(type);
	}

	/**
	 * Conversion methods for dealing with enums. Not necessary, and will
	 * be removed.
	 * @param name
	 * @return
	 */
	public static KeyLocatorType nameToType(String name) {
		return NameTypes.get(name);
	}
	
	@Override
	public boolean validate() {
		return ((null != name() || (null != key()) || (null != certificate())));
	}
	
	@Override
	public String toString() {
		String output = typeToName(type()) + ": "; 
		if (type() == KeyLocatorType.KEY) {
			return output + new PublisherPublicKeyDigest(key()).toString();
		} else if (type() == KeyLocatorType.CERTIFICATE) {
			try {
				return output + DataUtils.printHexBytes(CryptoUtil.generateCertID(certificate()));
			} catch (CertificateEncodingException e) {
				return output + "Unable to encode certificate: " + e.getMessage();
			}
		} else if (type() == KeyLocatorType.NAME) {
			return output + name();
		}
		return output + " UNKNOWN";
	}

}
