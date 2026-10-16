/**
 * BRN-1 uses the unchanged BRN primitive schema/extractor in core.brn. IDs 1..26224
 * map to embedding rows 0..26223; the old ID 0 scalar bias is skipped, with no
 * reinterpretation of any BRN-0 parameter. No handcrafted or Zobrist input is used.
 *
 * <p>Flat binary64 order: 26224*32 feature embeddings (row major), 32 hidden biases,
 * 32 output weights, one output bias: 839233 parameters. Hidden preactivations sum
 * bias and every active feature occurrence in extractor order, followed by ReLU.
 * Output is tanh(outputBias + dot(outputWeights, hiddenActivations)), directly from
 * side to move. Worker-owned Brn1Workspace retains features and 32 hidden values.
 * Immutable snapshots may be shared; mutable trainers/workspaces must not be shared.
 *
 * <p>Bootstrap uses java.util.Random seed 0x533642524e310001, embeddings uniform
 * [-.01,.01), zero biases, output weights uniform +/-sqrt(6/(32+1)). BRN Adam config
 * is reused without changing BRN-0: beta1=.9, beta2=.999, epsilon=1e-8. Loss is half
 * squared error. ReLU's derivative at zero is zero. Repeated feature gradients are
 * counted before one update per active row. Dense biases/output weights update every
 * step, including zero-gradient momentum decay; inactive embedding weights/moments
 * freeze. Bias correction uses the global successful step. No random state is needed
 * for optimizer continuation; the lifecycle owns generation-derived shuffle streams.
 *
 * <p>Architecture identity seedv6.brn.1. Big-endian codec format 1 header (28 bytes):
 * magic:i64 (ASCII S6BR1M01 model / S6BR1T01 training), format:i32=1,
 * sharedFeatureSchema:i32=1, featureCount:i32=26224, hiddenWidth:i32=32,
 * parameterCount:i32=839233. Model payload: parameters in the above flat order.
 * Training payload: globalStep:i64; learningRate,beta1,beta2,epsilon (four doubles);
 * parameters, first moments, second moments, each in the same flat order.
 * CRC32:i32 covers header and payload. Exact lengths: model 6,713,896 bytes;
 * training 20,141,664 bytes. Invalid dimensions, nonfinite values, negative steps
 * or second moments, nonzero moments at step zero, bad CRC, truncation and trailing
 * bytes fail closed. Streams remain caller-owned; use buffered file streams.
 * Saving requires an optimizer boundary. BRN-0 and NNUE payloads never migrate.
 */
package com.ohinteractive.seedv6.core.brn1;
