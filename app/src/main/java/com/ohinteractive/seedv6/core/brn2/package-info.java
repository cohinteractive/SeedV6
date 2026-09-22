/**
 * BRN-2, identity {@code seedv6.brn.2}: one local relational composition stage and
 * one board composition stage, both width 32, followed by a scalar tanh value head.
 *
 * <p>BRN-2 feature schema 2 uses side-to-move canonical inputs. Piece type stays
 * in the low three bits; bit 3 means THEM, with zero meaning US. Black perspective
 * squares are rank-reflected (xor 56), preserving files. Existing table dimensions
 * remain 960 node IDs, 25,200 exact-displacement unordered relation IDs and 64 status
 * rows; unused code/status rows are inactive. Only canonical castling (us K/Q, them
 * K/Q), oriented nonzero EP and the halfmove clock enter status context. STM is
 * redundant; fullmove numbering and reserved bits are omitted. Fullmove numbering
 * advances after physical Black and is not chess rule state needed for evaluation.
 * No key, chess-derived feature, policy head or further message-passing stage is used.
 * Brn2Features emits nodes in ascending canonical square order, then each pair
 * (a,b), a&lt;b, then set canonical status bits. Lower canonical square is endpoint A
 * even when horizontal dx is negative. Color reversal produces identical ordered
 * inputs, including endpoint routing and pooling order, for arbitrary weights.
 * Each pair contributes its distinct learned A/B vectors to those two local nodes.
 * The sum of active status embeddings is added to every occupied node BEFORE ReLU.
 * Local ReLUs are summed with board bias, then board ReLU precedes the value head.
 * Values and terminal targets use sampled-position side to move, in [-1,+1].
 *
 * <p>Flat binary64 parameter order: 960x32 node embeddings, 25,200x32 endpoint A,
 * 25,200x32 endpoint B, 64x32 status, 32 local biases, 32 board biases, 32 output
 * weights, one output bias: 1,645,665 trainable parameters. java.util.Random seed
 * 0x533642524e320001 initializes nodes uniformly +/-0.01, endpoint/status embeddings
 * +/-0.005, and output weights +/-sqrt(6/33); both hidden biases and output bias
 * start at zero. No scale adjustment was needed by deterministic bootstrap tests.
 *
 * <p>Loss is half squared error. Online Adam defaults are learning rate .001,
 * beta1 .9, beta2 .999, epsilon 1e-8. Both ReLU derivatives at zero are zero.
 * Every derivative uses pre-update weights. Reusable primitive slots aggregate the
 * vector derivative for each touched sparse row over all physical occurrences;
 * occurrence counts alone cannot represent endpoint-specific local ReLU masks.
 * A row receives one Adam update per example. Absent rows and moments freeze;
 * local/board biases and the value head update densely. Adam uses the global step
 * for bias correction, matching the accepted sparse BRN convention. All candidate
 * values/moments are checked before any persistent update is published.
 *
 * <p>Caller-owned workspaces allocate once, retain at most 64x32 local channels,
 * and never construct graph objects or dense input vectors. The trainer retains
 * sparse gradient slots for at most 4,160 rows (including both endpoint families).
 * Runtime/shuffle seeds are separate from fixed model initialization.
 * Search retains both canonical perspectives' incident sums and updates only
 * changed endpoints/incident relations in each. STM selects one already-maintained
 * perspective for a single forward pass; no Board transform or output averaging
 * occurs. Both perspectives retain the prior 32-placement-transition drift bound.
 * Status-only transitions and search-state copies do not trigger rebuilds.
 *
 * <p>Independent big-endian codec format 1: 40-byte header of magic (long), format,
 * BRN-2 feature schema version (2), node row count, relation row count, status row count,
 * endpoint count, width and parameter count (eight ints). Model signature is
 * S6BR2M01; training signature S6BR2T01. Model then stores all weights. Training
 * stores global step (long), learning rate/beta1/beta2/epsilon (four doubles),
 * weights, first moments, second moments. Each ends with CRC32 of all preceding
 * bytes. Exact sizes: model 13,165,364 bytes; training 39,496,044 bytes. Dimensions,
 * integrity, finite values, step and moment validity are checked on load. Streams
 * remain caller-owned; filesystem callers should buffer them. CheckpointStore
 * adds its existing SHA-256 manifest, immutable publication and recovery protocol.
 * {@code network.brn2} and {@code training.state} continue exact generation-boundary
 * state. Stop discards an unfinished generation; it does not save an in-game or
 * in-dataset cursor. BRN-0, BRN-1 and NNUE bytes and identities are unchanged.
 * Manifests also record seedv6.brn.2/schema 2. Schema-1 absolute-color model,
 * optimizer and store loading fail with a fresh-lineage instruction; old weights
 * cannot be resumed or migrated. This is still BRN-2, not a new architecture.
 */
package com.ohinteractive.seedv6.core.brn2;
