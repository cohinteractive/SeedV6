/**
 * BRN-0: additive primitive nodes, unordered geometric relations and raw status bits.
 *
 * <p>Input uses Seed's P0-P3 at board[0..3] and the entire status long at Board.STATUS=4.
 * Board.KEY=5 is excluded. Board.getSquare supplies the four-bit code; zero is empty,
 * all codes 1..15 have slots, including currently unused symbols and colour-only code 8.
 * Squares are a1=0, h1=7, a8=56, h8=63. Status currently encodes side, castling,
 * en-passant square and half/full-move counters; it is neither decoded nor masked here.
 * No legality, material, attack, phase or other chess-derived features are used.
 *
 * <p>Flat parameter order (26,225 binary64 values): bias[1], nodes[15*64],
 * relations[15*15*112], status[64]. Node index is 1+(code-1)*64+square.
 * Relations orient endpoints by dy&gt;0 or dy==0,dx&gt;0; reverse endpoints swap codes.
 * Displacements enumerate (dx=1..7,dy=0), then dy=1..7 with dx=-7..7.
 * Relation index is 961+((codeA-1)*15+codeB-1)*112+displacement; status starts at 26161.
 * Every occupied pair contributes, including multiple occurrences of the same parameter.
 * Inference stores only active occurrences in reusable primitive scratch (at most 2,145),
 * sums bias/nodes/pairs/status in deterministic order, then applies StrictMath.tanh.
 *
 * <p>Values and targets use Seed's side-to-move convention, with no automatic sign flip,
 * board rotation or centipawn mapping. Like any additive model, status can only shift the
 * preactivation: it cannot condition individual node/relation weights on side to move.
 * This expressiveness limit is part of BRN-0, not an implied engine integration contract.
 * Immutable BrnModel snapshots can be shared; each evaluating worker owns BrnFeatures.
 * BrnTrainer owns mutable weights, optimizer state and scratch; it is not thread-safe.
 * Zero initialization, loss 0.5*(prediction-target)^2 and its tanh derivative feed online
 * masked sparse Adam. Repeated relations multiply the gradient before one parameter update.
 * Absent weights and moments freeze. Bias correction uses the global successful-example
 * count; there is no lazy decay, per-feature clock, hidden power cache or random state.
 * A caller supplies learning rate; default beta1=.9, beta2=.999, epsilon=1e-8.
 * All candidates are checked before any update is committed.
 *
 * <p>Persistence format 1 is big-endian, separate from every NNUE format. Header:
 * magic:i64 (ASCII S6BRNM01 for inference, S6BRNT01 for training), format:i32=1,
 * schema:i32=1, parameterCount:i32=26225. Model payload: weights in flat order.
 * Training payload: completed global step:i64; learningRate,beta1,beta2,epsilon
 * as four binary64s; weights, first moments, second moments in flat order.
 * Every scalar retains its binary64 bits. Trailer: CRC32:i32 over header and payload.
 * Exact sizes are 209,824 and 629,464 bytes. Wrong signatures/versions/dimensions,
 * nonfinite values, negative second moments/steps, nonzero moments at step zero,
 * truncation, checksum errors and trailing data are rejected. Reads consume one entire
 * bounded payload stream; streams remain caller-owned. Saving is only valid at an
 * optimizer boundary without concurrent training. Scratch is reconstructed on reload.
 * These codecs do not publish files, participate in checkpoints, or expose BRN in the GUI.
 */
package com.ohinteractive.seedv6.core.brn;
