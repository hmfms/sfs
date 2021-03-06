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

package org.sfs.io;

import io.vertx.core.logging.Logger;
import org.sfs.SfsVertx;
import org.sfs.rx.ObservableFuture;
import org.sfs.rx.RxHelper;
import rx.Observable;
import rx.functions.Func1;

import java.util.Set;

import static com.google.common.base.Joiner.on;
import static io.vertx.core.logging.LoggerFactory.getLogger;

public class WaitForActiveWriters implements Func1<Void, Observable<Void>> {

    private static final Logger LOGGER = getLogger(WaitForActiveWriters.class);
    private final SfsVertx vertx;
    private final Set<? extends BufferEndableWriteStream> writers;

    public WaitForActiveWriters(SfsVertx vertx, Set<? extends BufferEndableWriteStream> writers) {
        this.vertx = vertx;
        this.writers = writers;
    }

    @Override
    public Observable<Void> call(Void aVoid) {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Waiting for Active Writers " + on(", ").join(writers));
        }
        ObservableFuture<Void> handler = RxHelper.observableFuture();
        if (hasActive()) {
            vertx.setPeriodic(100, event -> {
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Waiting for Active Writers " + on(", ").join(writers));
                }
                if (!hasActive()) {
                    vertx.cancelTimer(event);
                    handler.complete(null);
                }
            });
        } else {
            handler.complete(null);
        }
        return handler
                .map(aVoid1 -> {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Done waiting for Active Writers " + on(", ").join(writers));
                    }
                    return null;
                });
    }

    protected boolean hasActive() {
        return !writers.isEmpty();
    }

}
