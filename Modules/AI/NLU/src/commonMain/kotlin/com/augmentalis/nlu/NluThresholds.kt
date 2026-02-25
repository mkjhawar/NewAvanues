/**
 * NluThresholds.kt - Named confidence constants for the NLU pipeline
 *
 * Single source of truth for all confidence thresholds, weights, and tuning
 * parameters used across classifiers, matchers, and learning systems.
 *
 * Organized by semantic group. Each constant documents its purpose and
 * which components reference it.
 */
package com.augmentalis.nlu

object NluThresholds {

    // ── Classification Confidence ──────────────────────────────────────

    /** Minimum confidence when using semantic (ONNX/CoreML) embeddings.
     *  Used by: IntentClassifier (all platforms) */
    const val SEMANTIC_CONFIDENCE_THRESHOLD = 0.6f

    /** Minimum confidence when falling back to keyword/Jaccard matching.
     *  Used by: IntentClassifier (all platforms) */
    const val KEYWORD_CONFIDENCE_THRESHOLD = 0.5f

    /** Confidence at or above which a result is considered high-trust.
     *  Used by: HybridIntentClassifier, HybridClassifier, LearningDomain,
     *           VoiceOSLearningSyncWorker, VoiceOSLearningSource (SQL), NLUSelfLearner */
    const val HIGH_CONFIDENCE = 0.85f

    /** Minimum confidence for any match strategy to return a result.
     *  Used by: CommandMatchingService.MatchingConfig */
    const val MINIMUM_MATCH_CONFIDENCE = 0.5f

    /** Default minimum confidence for ClassifyIntentUseCase.
     *  Used by: ClassifyIntentUseCase */
    const val CLASSIFY_ACCEPT_THRESHOLD = 0.7f

    /** Default confidence for the high-confidence query in LearningDomain.
     *  Used by: IUnifiedLearningRepository.getHighConfidence() */
    const val HIGH_CONFIDENCE_QUERY_DEFAULT = 0.8f

    // ── Exact Match / Fast Path ────────────────────────────────────────

    /** Pattern match must meet this to be accepted as "exact" (skip ensemble).
     *  Used by: HybridIntentClassifier.ClassifierConfig */
    const val EXACT_MATCH_THRESHOLD = 0.95f

    /** Pattern confidence to skip ensemble voting entirely.
     *  Used by: HybridClassifier.EnhancedConfig */
    const val FAST_PATH_THRESHOLD = 0.95f

    /** Prefix match minimum similarity in PatternMatcher.
     *  Used by: PatternMatcher */
    const val PREFIX_MATCH_MIN_SIMILARITY = 0.8f

    // ── Fuzzy Matching ─────────────────────────────────────────────────

    /** Minimum Levenshtein similarity for basic classifiers.
     *  Used by: FuzzyMatcher, HybridIntentClassifier.ClassifierConfig */
    const val FUZZY_MIN_SIMILARITY = 0.7f

    /** Fuzzy score to auto-accept without semantic verification.
     *  Used by: HybridIntentClassifier.ClassifierConfig */
    const val FUZZY_ACCEPT_THRESHOLD = 0.85f

    /** Minimum Levenshtein similarity for enhanced classifier.
     *  Used by: HybridClassifier.EnhancedConfig (lower bar for ensemble) */
    const val ENHANCED_FUZZY_MIN_SIMILARITY = 0.65f

    /** Fuzzy threshold for CommandMatchingService.
     *  Used by: CommandMatchingService.MatchingConfig */
    const val FUZZY_MATCH_THRESHOLD = 0.7f

    // ── Semantic Matching ──────────────────────────────────────────────

    /** Minimum cosine similarity for basic classifiers.
     *  Used by: SemanticMatcher, HybridIntentClassifier.ClassifierConfig */
    const val SEMANTIC_MIN_SIMILARITY = 0.6f

    /** Minimum cosine similarity for enhanced classifier.
     *  Used by: HybridClassifier.EnhancedConfig (lower bar for ensemble) */
    const val ENHANCED_SEMANTIC_MIN_SIMILARITY = 0.55f

    /** Semantic threshold for CommandMatchingService.
     *  Used by: CommandMatchingService.MatchingConfig */
    const val SEMANTIC_MATCH_THRESHOLD = 0.6f

    // ── Hybrid / Ensemble ──────────────────────────────────────────────

    /** Minimum combined score from ensemble voting.
     *  Used by: HybridIntentClassifier.ClassifierConfig */
    const val HYBRID_MIN_SCORE = 0.5f

    // ── Ambiguity Detection ────────────────────────────────────────────

    /** Max difference between top-two scores to flag ambiguity.
     *  Used by: IntentClassifier (commonMain) */
    const val DEFAULT_AMBIGUITY_THRESHOLD = 0.15f

    /** Ambiguity threshold for command matching (tighter).
     *  Used by: CommandMatchingService.MatchingConfig */
    const val COMMAND_AMBIGUITY_THRESHOLD = 0.1f

    // ── BERT Verification ──────────────────────────────────────────────

    /** Confidence range where BERT verification is recommended (lower bound).
     *  Used by: HybridClassifier.EnhancedConfig.verificationRange */
    const val VERIFICATION_RANGE_LOW = 0.6f

    /** Confidence range where BERT verification is recommended (upper bound).
     *  Used by: HybridClassifier.EnhancedConfig.verificationRange */
    const val VERIFICATION_RANGE_HIGH = 0.85f

    /** 10% boost when BERT agrees with hybrid classifier.
     *  Used by: NluService */
    const val BERT_AGREEMENT_BOOST_MULTIPLIER = 1.1f

    /** 5% penalty when BERT overrides hybrid classifier.
     *  Used by: NluService */
    const val BERT_OVERRIDE_CONFIDENCE_FACTOR = 0.95f

    // ── Strategy Weights (per-signal contribution) ─────────────────────

    /** Weight for fuzzy match scores in basic classifier.
     *  Used by: HybridIntentClassifier.ClassifierConfig */
    const val CLASSIFIER_FUZZY_WEIGHT = 0.9f

    /** Weight for semantic match scores in basic classifier.
     *  Used by: HybridIntentClassifier.ClassifierConfig */
    const val CLASSIFIER_SEMANTIC_WEIGHT = 0.85f

    /** Weight for fuzzy match scores in enhanced classifier.
     *  Used by: HybridClassifier.EnhancedConfig */
    const val ENHANCED_FUZZY_WEIGHT = 0.85f

    /** Weight for semantic match scores in enhanced classifier.
     *  Used by: HybridClassifier.EnhancedConfig */
    const val ENHANCED_SEMANTIC_WEIGHT = 0.9f

    /** Bonus per additional agreeing signal in ensemble (basic).
     *  Used by: HybridClassifier.EnhancedConfig */
    const val AGREEMENT_BONUS = 0.1f

    /** Bonus per additional agreeing signal in command matching.
     *  Used by: CommandMatchingService.MatchingConfig */
    const val COMMAND_AGREEMENT_BONUS = 0.05f

    /** CommandMatchingService strategy weights. */
    const val SYNONYM_STRATEGY_WEIGHT = 0.95f
    const val LEVENSHTEIN_STRATEGY_WEIGHT = 0.85f
    const val JACCARD_STRATEGY_WEIGHT = 0.8f
    const val SEMANTIC_STRATEGY_WEIGHT = 0.9f

    // ── Priority Boost Factors ─────────────────────────────────────────

    /** Priority boost factor for semantic match ranking.
     *  Used by: SemanticMatcher */
    const val SEMANTIC_PRIORITY_BOOST = 0.05f

    /** Priority boost factor for fuzzy match ranking (higher than semantic).
     *  Used by: FuzzyMatcher */
    const val FUZZY_PRIORITY_BOOST = 0.1f

    // ── Keyword Scoring ────────────────────────────────────────────────

    /** Bonus weight for keyword exact matches in Jaccard scoring.
     *  Used by: IntentClassifier (all platforms) */
    const val KEYWORD_EXACT_MATCH_BONUS_WEIGHT = 0.3f

    /** Bonus per partial word match (substring containment) in Jaccard.
     *  Used by: CommandMatchingService */
    const val PARTIAL_WORD_MATCH_BONUS = 0.1f

    // ── Calibration (Self-Learning) ────────────────────────────────────

    /** Default base confidence for CalibrationData.
     *  Used by: HybridClassifier.CalibrationData */
    const val DEFAULT_BASE_CONFIDENCE = 0.8f

    /** Maximum boost multiplier for pattern/fuzzy/semantic.
     *  Used by: HybridClassifier calibration */
    const val MAX_SIGNAL_BOOST = 1.2f

    /** Increment per positive feedback for signal boost.
     *  Used by: HybridClassifier calibration */
    const val SIGNAL_BOOST_INCREMENT = 0.01f

    /** Maximum context boost multiplier.
     *  Used by: HybridClassifier calibration */
    const val MAX_CONTEXT_BOOST = 1.3f

    /** Increment per positive context feedback.
     *  Used by: HybridClassifier calibration */
    const val CONTEXT_BOOST_INCREMENT = 0.02f

    /** Minimum base confidence floor after negative feedback.
     *  Used by: HybridClassifier calibration */
    const val MIN_BASE_CONFIDENCE = 0.5f

    /** Decrement per negative feedback.
     *  Used by: HybridClassifier calibration */
    const val CONFIDENCE_PENALTY_DECREMENT = 0.05f

    /** Maximum base confidence cap after positive feedback.
     *  Used by: HybridClassifier calibration */
    const val MAX_BASE_CONFIDENCE = 1.0f

    /** Increment per positive feedback.
     *  Used by: HybridClassifier calibration */
    const val CONFIDENCE_REWARD_INCREMENT = 0.03f

    // ── Learning / Variation ───────────────────────────────────────────

    /** Confidence discount for LLM-generated variations vs original.
     *  Used by: NLUSelfLearner */
    const val VARIATION_CONFIDENCE_DISCOUNT = 0.9f

    /** Minimum confidence to save any learned data.
     *  Used by: NLUSelfLearner, VoiceOSLearningSource, UnifiedLearningService */
    const val MIN_LEARNING_CONFIDENCE = 0.6f

    /** Default confidence when not provided in worker input.
     *  Used by: EmbeddingComputeWorker */
    const val DEFAULT_WORKER_CONFIDENCE = 0.8f

    // ── Embedding Quality ──────────────────────────────────────────────

    /** L2 norm tolerance for normalized embedding validation.
     *  Used by: AonEmbeddingComputer */
    const val EMBEDDING_NORM_TOLERANCE = 0.01f

    // ── Language Detection ─────────────────────────────────────────────

    /** Script must exceed this fraction of characters to detect language.
     *  Used by: MultilingualSupport.LanguageDetector */
    const val SCRIPT_MAJORITY_THRESHOLD = 0.5f
}
