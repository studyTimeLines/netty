/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.channel.udt.nio;

import com.barchart.udt.TypeUDT;
import com.barchart.udt.nio.SocketChannelUDT;
import io.netty.buffer.BufType;
import io.netty.buffer.MessageBuf;
import io.netty.channel.ChannelMetadata;

import java.util.concurrent.TimeUnit;

/**
 * Byte Channel Acceptor for UDT Streams.
 */
public class NioUdtByteAcceptorChannel extends NioUdtAcceptorChannel {

    private static final ChannelMetadata METADATA = new ChannelMetadata(
            BufType.BYTE, false);

    private Runnable acceptExceptionHandler;

    public NioUdtByteAcceptorChannel() {
        super(TypeUDT.STREAM);
    }

    @Override
    protected int doReadMessages(final MessageBuf<Object> buf) throws Exception {
        SocketChannelUDT channelUDT = null;
        try {
            channelUDT = javaChannel().accept();
            if (channelUDT == null) {
                return 0;
            } else {
                buf.add(new NioUdtByteConnectorChannel(this, channelUDT.socketUDT()
                        .id(), channelUDT));
                return 1;
            }
        } catch (Throwable t) {
            logger.warn("Failed to accept socket and create a new channel.", t);

            if (channelUDT != null) {
                try {
                    channelUDT.close();
                } catch (Throwable t2) {
                    logger.warn("Failed to close a socket.", t2);
                }
            }

            if (config().isAutoRead()) {
                if (acceptExceptionHandler == null) {
                    acceptExceptionHandler = new Runnable() {
                        @Override
                        public void run() {
                            config().setAutoRead(true);
                        }
                    };
                }
                config().setAutoRead(false);
                eventLoop().schedule(acceptExceptionHandler, 1, TimeUnit.SECONDS);
            }
        }
        return 0;
    }

    @Override
    public ChannelMetadata metadata() {
        return METADATA;
    }

}
