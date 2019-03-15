/*
 * Copyright 2019 The Simple File Server Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.sfs.encryption.impl;

import com.google.common.hash.Hashing;
import com.google.common.math.LongMath;
import com.nimbusds.jose.crypto.BouncyCastleProviderSingleton;
import io.vertx.core.buffer.Buffer;
import org.sfs.encryption.Algorithm;
import org.sfs.io.BufferEndableWriteStream;
import org.sfs.io.CipherEndableWriteStream;
import org.sfs.io.CipherReadStream;
import org.sfs.io.EndableReadStream;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;

public class SAES256v02 extends Algorithm {

    public static final int KEY_SIZE_BITS = 256;
    public static final int KEY_SIZE_BYTES = KEY_SIZE_BITS / 8;
    public static final int TAG_LENGTH_BITS = 96;
    public static final int NONCE_SIZE_BYTES = 12;
    private byte[] salt;
    private final Cipher encryptor;
    private final Cipher decryptor;
    private static final long MAX_LONG_BUFFER_SIZE = Long.MAX_VALUE - 12;
    private static final int TAG_LENGTH_BYTES = TAG_LENGTH_BITS / 8;

    public SAES256v02(byte[] secretBytes, byte[] salt) {
        this.salt = salt.clone();
        secretBytes = secretBytes.clone();
        if (secretBytes.length != KEY_SIZE_BYTES) {
            secretBytes = Hashing.sha256().hashBytes(secretBytes).asBytes();
        }
        try {
            SecretKeySpec key = new SecretKeySpec(secretBytes, "AES");

            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH_BITS, this.salt);

            this.encryptor = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProviderSingleton.getInstance());
            this.encryptor.init(Cipher.ENCRYPT_MODE, key, spec);

            this.decryptor = Cipher.getInstance("AES/GCM/NoPadding", BouncyCastleProviderSingleton.getInstance());
            this.decryptor.init(Cipher.DECRYPT_MODE, key, spec);

        } catch (Exception e) {
            throw new RuntimeException("could not create cipher for AES256", e);
        } finally {
            Arrays.fill(secretBytes, (byte) 0);
        }
    }

    public long maxEncryptInputSize() {
        return MAX_LONG_BUFFER_SIZE;
    }

    @Override
    public long encryptOutputSize(long size) {
        try {
            return LongMath.checkedAdd(size, TAG_LENGTH_BYTES);
        } catch (ArithmeticException e) {
            // do nothing
        }
        return -1;
    }

    @Override
    public byte[] getSalt() {
        return salt.clone();
    }


    @Override
    public byte[] encrypt(byte[] buffer) {
        try {
            return encryptor.doFinal(buffer);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] decrypt(byte[] buffer) {
        try {
            return decryptor.doFinal(buffer);
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Buffer encrypt(Buffer buffer) {
        return Buffer.buffer(encrypt(buffer.getBytes()));
    }

    @Override
    public Buffer decrypt(Buffer buffer) {
        return Buffer.buffer(decrypt(buffer.getBytes()));
    }

    @Override
    public <T extends CipherEndableWriteStream> T decrypt(BufferEndableWriteStream writeStream) {
        return (T) new CipherEndableWriteStream(writeStream, decryptor);
    }

    @Override
    public <T extends CipherEndableWriteStream> T encrypt(BufferEndableWriteStream writeStream) {
        return (T) new CipherEndableWriteStream(writeStream, encryptor);
    }

    @Override
    public <T extends CipherReadStream> T encrypt(EndableReadStream<Buffer> readStream) {
        return (T) new CipherReadStream(readStream, encryptor);
    }

    @Override
    public <T extends CipherReadStream> T decrypt(EndableReadStream<Buffer> readStream) {
        return (T) new CipherReadStream(readStream, decryptor);
    }
}