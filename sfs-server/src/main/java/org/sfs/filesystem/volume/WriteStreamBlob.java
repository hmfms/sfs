/*
 * Copyright 2016 The Simple File Server Authors
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

package org.sfs.filesystem.volume;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.streams.ReadStream;
import rx.Observable;

public abstract class WriteStreamBlob extends HeaderBlob {

    public WriteStreamBlob(String volume, long position, long length) {
        super(volume, position, length);
    }

    public WriteStreamBlob(HeaderBlob headerBlob) {
        super(headerBlob);
    }

    public abstract Observable<Void> consume(ReadStream<Buffer> src);
}
