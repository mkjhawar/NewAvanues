package com.augmentalis.voiceos.speech.engines.vivoka.model

import com.google.gson.annotations.SerializedName

data class Root(
    val version: String,
    val csdk: Csdk
)

data class Csdk(
    val log: Log,
    val asr: Asr,
    val paths: Paths
)

data class Log(
    val cache: Cache
)

data class Cache(
    val enabled: Boolean
)

data class Asr(
    val recognizers: Recognizers,
    val models: MutableMap<String, Model> // Dynamic map for models
)

data class Recognizers(
    val rec: Rec
)

data class Rec(
    val acmods: MutableList<String> // Mutable for merging
)

data class Model(
    val type: String,
    val file: String,
    val acmod: String? = null,
    @SerializedName("extra_models")
    val extraModels: MutableMap<String, String>? = null, // Nullable for optional models
    val settings: MutableMap<String, Int>? = null, // Mutable for flexibility in merging
    val slots: Map<String, Slot>? = null, // Nullable for optional slots
    val lexicon: Lexicon? = null // Nullable for optional lexicon
)

data class Slot(
    val slot: String,
    val category: String,
    @SerializedName("allow_custom_phonetic")
    val allowCustomPhonetic: Boolean
)

data class Lexicon(
    val clc: String
)

data class Paths(
    val acmod: String,
    val asr: String,
    @SerializedName("audio_based_classifier_model")
    val audioBasedClassifierModel: String,
    val clc: String,
    @SerializedName("clc_ruleset")
    val clcRuleset: String,
    @SerializedName("confusion_dictionary")
    val confusionDictionary: String,
    @SerializedName("data_root")
    val dataRoot: String,
    val dictionary: String,
    @SerializedName("language_model")
    val languageModel: String,
    val search: String,
    val sem3: String,
    val users: String
)