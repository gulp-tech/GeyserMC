/*
 * Copyright (c) 2019-2026 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.geyser.network.bedrock;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import lombok.RequiredArgsConstructor;
import org.geysermc.geyser.GeyserImpl;
import org.geysermc.geyser.GeyserLogger;
import org.geysermc.geyser.session.GeyserSession;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.concurrent.RejectedExecutionException;

/**
 * Handles exceptions that bubble up from the Bedrock pipeline.
 *
 * <p>Not every exception means the player did something wrong: network hiccups, disconnects that race
 * with in-flight packets, and single packets that a client sends with a slightly different (but
 * harmless) layout used to kick the player with "An internal error occurred!". This handler
 * distinguishes those cases so a transient problem no longer ends the session.</p>
 */
@RequiredArgsConstructor
public class InvalidPacketHandler extends ChannelInboundHandlerAdapter {
    public static final String NAME = "rak-error-handler";

    /**
     * How many packets we are willing to skip before we assume the connection is genuinely broken.
     * Bedrock packets are length prefixed, so skipping one undecodable packet keeps the stream usable;
     * a client that keeps sending garbage will still be disconnected.
     *
     * <p>Set {@code -DGeyser.MaxUndecodablePackets=0} to restore the old "kick on the first bad packet"
     * behaviour, or a higher value for very chatty clients.</p>
     */
    private static final int MAX_UNDECODABLE_PACKETS = Math.max(0, Integer.getInteger("Geyser.MaxUndecodablePackets", 25));

    private final GeyserSession session;

    private int undecodablePackets;

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        Throwable rootCause = rootCause(cause);

        GeyserLogger logger = GeyserImpl.getInstance().getLogger();
        String sessionName = describeSession();

        // The session is already gone (a disconnect raced with packets that were still in flight).
        // There is nothing left to kick - just make sure the channel gets cleaned up quietly.
        if (session.isClosed() || !ctx.channel().isActive()) {
            logger.debug("Ignoring exception on closed session of " + sessionName + ": " + rootCause);
            ctx.close();
            return;
        }

        if (rootCause instanceof IllegalPacketException) {
            // The client explicitly sent a packet it is not allowed to send (e.g. server-auth violations)
            logger.error("Illegal packet from " + sessionName, cause);
            closeWithReason("Invalid packet received!");
            return;
        }

        if (isNetworkError(rootCause)) {
            // Timeouts, resets and half-closed sockets are not our fault and not worth a scary kick message
            logger.debug("Network error on session of " + sessionName + ": " + rootCause);
            closeQuietly();
            return;
        }

        if (isDecodingError(cause, rootCause)) {
            // A single packet could not be decoded. For example, a client that is slightly ahead of the
            // codec we ship, or a packet with an unexpected trailing field. The batch format is length
            // prefixed, so we can drop just this packet instead of kicking the player.
            if (++undecodablePackets <= MAX_UNDECODABLE_PACKETS) {
                if (logger.isDebug() || undecodablePackets == 1) {
                    logger.warning("Skipping undecodable packet from " + sessionName
                            + " (" + undecodablePackets + "/" + MAX_UNDECODABLE_PACKETS + "): " + rootCause);
                }
                if (logger.isDebug()) {
                    logger.debug("Undecodable packet", cause);
                }
                return;
            }

            logger.error("Too many undecodable packets from " + sessionName + ", disconnecting", cause);
            closeWithReason("Invalid packet received!");
            return;
        }

        // Genuinely unexpected: the session state may be inconsistent, so we still have to end it,
        // but we make sure the disconnect itself cannot throw and leave the channel dangling.
        logger.error("Exception caught in session of " + sessionName, cause);
        closeWithReason("An internal error occurred!");
    }

    /**
     * Describes the session in log messages, including the Bedrock protocol version so that a kick can be
     * traced back to the client build that caused it.
     */
    private String describeSession() {
        try {
            return session.bedrockUsername() + " (protocol " + session.protocolVersion() + ")";
        } catch (Throwable ignored) {
            return session.bedrockUsername();
        }
    }

    private static Throwable rootCause(Throwable cause) {
        Throwable current = cause;
        // Bounded walk so a (theoretical) cyclic cause chain cannot hang us
        for (int i = 0; current.getCause() != null && current.getCause() != current && i < 32; i++) {
            current = current.getCause();
        }
        return current;
    }

    private static boolean isNetworkError(Throwable cause) {
        return cause instanceof IOException
                || cause instanceof ClosedChannelException
                || cause instanceof RejectedExecutionException
                || cause instanceof InterruptedException;
    }

    private static boolean isDecodingError(Throwable cause, Throwable rootCause) {
        if (!(cause instanceof DecoderException)) {
            return false;
        }
        // EncoderException means *we* produced something the client cannot read - that is a Geyser bug,
        // and retrying the same packet will not help.
        for (Throwable current = cause; current != null; current = current.getCause()) {
            if (current instanceof EncoderException) {
                return false;
            }
            if (current == current.getCause()) {
                break;
            }
        }
        // Exceptions thrown while serializing our own packets are not decoding errors either
        return !(rootCause instanceof IllegalPacketException && rootCause.getMessage() != null
                && rootCause.getMessage().startsWith("Server tried to send"));
    }

    private void closeWithReason(String reason) {
        try {
            session.disconnect(reason);
        } catch (Throwable ex) {
            GeyserImpl.getInstance().getLogger().debug("Could not send disconnect reason to " + describeSession(), ex);
        } finally {
            closeQuietly();
        }
    }

    private void closeQuietly() {
        try {
            if (!session.isClosed()) {
                session.forciblyCloseUpstream();
            }
        } catch (Throwable ex) {
            GeyserImpl.getInstance().getLogger().debug("Could not close session of " + describeSession(), ex);
        }
    }
}
