/*
 * PluginProtoStubs.kt - Stub proto message classes for Plugin Service
 *
 * Copyright (C) Manoj Jhawar, Intelligent Devices LLC
 * Created: 2026-01-22
 *
 * These are simplified stub classes that match plugin.proto messages.
 * They will be replaced with Wire-generated classes when proto generation is enabled.
 *
 * TODO: Re-enable Wire plugin and regenerate from plugin.proto
 */

package com.augmentalis.universalrpc.plugin

import com.squareup.wire.FieldEncoding
import com.squareup.wire.Message
import com.squareup.wire.ProtoAdapter
import com.squareup.wire.ProtoReader
import com.squareup.wire.ProtoWriter
import com.squareup.wire.ReverseProtoWriter
import com.squareup.wire.Syntax.PROTO_3
import com.squareup.wire.WireEnum
import com.squareup.wire.WireField
import com.squareup.wire.`internal`.JvmField
import okio.ByteString
import kotlin.Any
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.Nothing
import kotlin.String

// ============= Enums =============

/**
 * Plugin lifecycle state enum
 */
enum class PluginStateProto(override val value: Int) : WireEnum {
    PLUGIN_STATE_UNKNOWN(0),
    PLUGIN_STATE_REGISTERED(1),
    PLUGIN_STATE_INITIALIZING(2),
    PLUGIN_STATE_ACTIVE(3),
    PLUGIN_STATE_PAUSED(4),
    PLUGIN_STATE_ERROR(5),
    PLUGIN_STATE_STOPPING(6),
    PLUGIN_STATE_STOPPED(7);

    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<PluginStateProto> = object : ProtoAdapter<PluginStateProto>(
            FieldEncoding.VARINT, PluginStateProto::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.PluginState", PROTO_3, null, "plugin.proto"
        ) {
            override fun encodedSize(value: PluginStateProto): Int = ProtoAdapter.INT32.encodedSize(value.value)
            override fun encode(writer: ProtoWriter, value: PluginStateProto) { ProtoAdapter.INT32.encode(writer, value.value) }
            override fun encode(writer: ReverseProtoWriter, value: PluginStateProto) { ProtoAdapter.INT32.encode(writer, value.value) }
            override fun decode(reader: ProtoReader): PluginStateProto = fromValue(ProtoAdapter.INT32.decode(reader))
            override fun redact(value: PluginStateProto): PluginStateProto = value
        }

        fun fromValue(value: Int): PluginStateProto = entries.find { it.value == value } ?: PLUGIN_STATE_UNKNOWN
    }
}

/**
 * Lifecycle action enum
 */
enum class LifecycleAction(override val value: Int) : WireEnum {
    LIFECYCLE_ACTIVATE(0),
    LIFECYCLE_PAUSE(1),
    LIFECYCLE_RESUME(2),
    LIFECYCLE_STOP(3),
    LIFECYCLE_CONFIG_CHANGED(4);

    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<LifecycleAction> = object : ProtoAdapter<LifecycleAction>(
            FieldEncoding.VARINT, LifecycleAction::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.LifecycleAction", PROTO_3, null, "plugin.proto"
        ) {
            override fun encodedSize(value: LifecycleAction): Int = ProtoAdapter.INT32.encodedSize(value.value)
            override fun encode(writer: ProtoWriter, value: LifecycleAction) { ProtoAdapter.INT32.encode(writer, value.value) }
            override fun encode(writer: ReverseProtoWriter, value: LifecycleAction) { ProtoAdapter.INT32.encode(writer, value.value) }
            override fun decode(reader: ProtoReader): LifecycleAction = fromValue(ProtoAdapter.INT32.decode(reader))
            override fun redact(value: LifecycleAction): LifecycleAction = value
        }

        fun fromValue(value: Int): LifecycleAction = entries.find { it.value == value } ?: LIFECYCLE_ACTIVATE
    }
}

// ============= Messages =============

/**
 * Plugin capability advertisement
 */
data class PluginCapabilityProto(
    val id: String = "",
    val name: String = "",
    val version: String = "",
    val interfaces: List<String> = emptyList(),
    val metadata: Map<String, String> = emptyMap()
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<PluginCapabilityProto> = PluginCapabilityProtoAdapter
    }
}

private object PluginCapabilityProtoAdapter : ProtoAdapter<PluginCapabilityProto>(
    FieldEncoding.LENGTH_DELIMITED, PluginCapabilityProto::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.PluginCapability", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: PluginCapabilityProto): Int = 0
    override fun encode(writer: ProtoWriter, value: PluginCapabilityProto) {}
    override fun encode(writer: ReverseProtoWriter, value: PluginCapabilityProto) {}
    override fun decode(reader: ProtoReader): PluginCapabilityProto = PluginCapabilityProto()
    override fun redact(value: PluginCapabilityProto): PluginCapabilityProto = value
}

/**
 * Register plugin request
 */
data class RegisterPluginRequest(
    val request_id: String = "",
    val plugin_id: String = "",
    val plugin_name: String = "",
    val version: String = "",
    val capabilities: List<PluginCapabilityProto> = emptyList(),
    val endpoint_address: String = "",
    val endpoint_protocol: String = ""
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<RegisterPluginRequest> = RegisterPluginRequestAdapter
    }
}

private object RegisterPluginRequestAdapter : ProtoAdapter<RegisterPluginRequest>(
    FieldEncoding.LENGTH_DELIMITED, RegisterPluginRequest::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.RegisterPluginRequest", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: RegisterPluginRequest): Int = 0
    override fun encode(writer: ProtoWriter, value: RegisterPluginRequest) {}
    override fun encode(writer: ReverseProtoWriter, value: RegisterPluginRequest) {}
    override fun decode(reader: ProtoReader): RegisterPluginRequest = RegisterPluginRequest()
    override fun redact(value: RegisterPluginRequest): RegisterPluginRequest = value
}

/**
 * Register plugin response
 */
data class RegisterPluginResponse(
    val request_id: String = "",
    val success: Boolean = false,
    val message: String = "",
    val assigned_id: String = ""
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<RegisterPluginResponse> = RegisterPluginResponseAdapter
    }
}

private object RegisterPluginResponseAdapter : ProtoAdapter<RegisterPluginResponse>(
    FieldEncoding.LENGTH_DELIMITED, RegisterPluginResponse::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.RegisterPluginResponse", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: RegisterPluginResponse): Int = 0
    override fun encode(writer: ProtoWriter, value: RegisterPluginResponse) {}
    override fun encode(writer: ReverseProtoWriter, value: RegisterPluginResponse) {}
    override fun decode(reader: ProtoReader): RegisterPluginResponse = RegisterPluginResponse()
    override fun redact(value: RegisterPluginResponse): RegisterPluginResponse = value
}

/**
 * Unregister plugin request
 */
data class UnregisterPluginRequest(
    val request_id: String = "",
    val plugin_id: String = ""
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<UnregisterPluginRequest> = UnregisterPluginRequestAdapter
    }
}

private object UnregisterPluginRequestAdapter : ProtoAdapter<UnregisterPluginRequest>(
    FieldEncoding.LENGTH_DELIMITED, UnregisterPluginRequest::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.UnregisterPluginRequest", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: UnregisterPluginRequest): Int = 0
    override fun encode(writer: ProtoWriter, value: UnregisterPluginRequest) {}
    override fun encode(writer: ReverseProtoWriter, value: UnregisterPluginRequest) {}
    override fun decode(reader: ProtoReader): UnregisterPluginRequest = UnregisterPluginRequest()
    override fun redact(value: UnregisterPluginRequest): UnregisterPluginRequest = value
}

/**
 * Unregister plugin response
 */
data class UnregisterPluginResponse(
    val request_id: String = "",
    val success: Boolean = false,
    val message: String = ""
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<UnregisterPluginResponse> = UnregisterPluginResponseAdapter
    }
}

private object UnregisterPluginResponseAdapter : ProtoAdapter<UnregisterPluginResponse>(
    FieldEncoding.LENGTH_DELIMITED, UnregisterPluginResponse::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.UnregisterPluginResponse", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: UnregisterPluginResponse): Int = 0
    override fun encode(writer: ProtoWriter, value: UnregisterPluginResponse) {}
    override fun encode(writer: ReverseProtoWriter, value: UnregisterPluginResponse) {}
    override fun decode(reader: ProtoReader): UnregisterPluginResponse = UnregisterPluginResponse()
    override fun redact(value: UnregisterPluginResponse): UnregisterPluginResponse = value
}

/**
 * Discover plugins request
 */
data class DiscoverPluginsRequest(
    val request_id: String = "",
    val capability_filter: List<String> = emptyList(),
    val include_disabled: Boolean = false
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<DiscoverPluginsRequest> = DiscoverPluginsRequestAdapter
    }
}

private object DiscoverPluginsRequestAdapter : ProtoAdapter<DiscoverPluginsRequest>(
    FieldEncoding.LENGTH_DELIMITED, DiscoverPluginsRequest::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.DiscoverPluginsRequest", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: DiscoverPluginsRequest): Int = 0
    override fun encode(writer: ProtoWriter, value: DiscoverPluginsRequest) {}
    override fun encode(writer: ReverseProtoWriter, value: DiscoverPluginsRequest) {}
    override fun decode(reader: ProtoReader): DiscoverPluginsRequest = DiscoverPluginsRequest()
    override fun redact(value: DiscoverPluginsRequest): DiscoverPluginsRequest = value
}

/**
 * Discover plugins response
 */
data class DiscoverPluginsResponse(
    val request_id: String = "",
    val plugins: List<PluginInfo> = emptyList()
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<DiscoverPluginsResponse> = DiscoverPluginsResponseAdapter
    }
}

private object DiscoverPluginsResponseAdapter : ProtoAdapter<DiscoverPluginsResponse>(
    FieldEncoding.LENGTH_DELIMITED, DiscoverPluginsResponse::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.DiscoverPluginsResponse", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: DiscoverPluginsResponse): Int = 0
    override fun encode(writer: ProtoWriter, value: DiscoverPluginsResponse) {}
    override fun encode(writer: ReverseProtoWriter, value: DiscoverPluginsResponse) {}
    override fun decode(reader: ProtoReader): DiscoverPluginsResponse = DiscoverPluginsResponse()
    override fun redact(value: DiscoverPluginsResponse): DiscoverPluginsResponse = value
}

/**
 * Plugin info
 */
data class PluginInfo(
    val plugin_id: String = "",
    val plugin_name: String = "",
    val version: String = "",
    val state: PluginStateProto = PluginStateProto.PLUGIN_STATE_UNKNOWN,
    val capabilities: List<PluginCapabilityProto> = emptyList(),
    val endpoint_address: String = "",
    val registered_at: Long = 0L,
    val last_health_check: Long = 0L
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<PluginInfo> = PluginInfoAdapter
    }
}

private object PluginInfoAdapter : ProtoAdapter<PluginInfo>(
    FieldEncoding.LENGTH_DELIMITED, PluginInfo::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.PluginInfo", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: PluginInfo): Int = 0
    override fun encode(writer: ProtoWriter, value: PluginInfo) {}
    override fun encode(writer: ReverseProtoWriter, value: PluginInfo) {}
    override fun decode(reader: ProtoReader): PluginInfo = PluginInfo()
    override fun redact(value: PluginInfo): PluginInfo = value
}

/**
 * Get plugin info request
 */
data class GetPluginInfoRequest(
    val request_id: String = "",
    val plugin_id: String = ""
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<GetPluginInfoRequest> = GetPluginInfoRequestAdapter
    }
}

private object GetPluginInfoRequestAdapter : ProtoAdapter<GetPluginInfoRequest>(
    FieldEncoding.LENGTH_DELIMITED, GetPluginInfoRequest::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.GetPluginInfoRequest", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: GetPluginInfoRequest): Int = 0
    override fun encode(writer: ProtoWriter, value: GetPluginInfoRequest) {}
    override fun encode(writer: ReverseProtoWriter, value: GetPluginInfoRequest) {}
    override fun decode(reader: ProtoReader): GetPluginInfoRequest = GetPluginInfoRequest()
    override fun redact(value: GetPluginInfoRequest): GetPluginInfoRequest = value
}

/**
 * Lifecycle command
 */
data class LifecycleCommand(
    val request_id: String = "",
    val plugin_id: String = "",
    val action: LifecycleAction = LifecycleAction.LIFECYCLE_ACTIVATE,
    val config: Map<String, String> = emptyMap()
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<LifecycleCommand> = LifecycleCommandAdapter
    }
}

private object LifecycleCommandAdapter : ProtoAdapter<LifecycleCommand>(
    FieldEncoding.LENGTH_DELIMITED, LifecycleCommand::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.LifecycleCommand", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: LifecycleCommand): Int = 0
    override fun encode(writer: ProtoWriter, value: LifecycleCommand) {}
    override fun encode(writer: ReverseProtoWriter, value: LifecycleCommand) {}
    override fun decode(reader: ProtoReader): LifecycleCommand = LifecycleCommand()
    override fun redact(value: LifecycleCommand): LifecycleCommand = value
}

/**
 * Lifecycle response
 */
data class LifecycleResponse(
    val request_id: String = "",
    val success: Boolean = false,
    val message: String = "",
    val new_state: PluginStateProto = PluginStateProto.PLUGIN_STATE_UNKNOWN
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<LifecycleResponse> = LifecycleResponseAdapter
    }
}

private object LifecycleResponseAdapter : ProtoAdapter<LifecycleResponse>(
    FieldEncoding.LENGTH_DELIMITED, LifecycleResponse::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.LifecycleResponse", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: LifecycleResponse): Int = 0
    override fun encode(writer: ProtoWriter, value: LifecycleResponse) {}
    override fun encode(writer: ReverseProtoWriter, value: LifecycleResponse) {}
    override fun decode(reader: ProtoReader): LifecycleResponse = LifecycleResponse()
    override fun redact(value: LifecycleResponse): LifecycleResponse = value
}

/**
 * Plugin event (for event bus)
 */
data class PluginEvent(
    val event_id: String = "",
    val source_plugin_id: String = "",
    val event_type: String = "",
    val timestamp: Long = 0L,
    val payload: Map<String, String> = emptyMap(),
    val payload_json: String = ""
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<PluginEvent> = PluginEventAdapter
    }
}

private object PluginEventAdapter : ProtoAdapter<PluginEvent>(
    FieldEncoding.LENGTH_DELIMITED, PluginEvent::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.PluginEvent", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: PluginEvent): Int = 0
    override fun encode(writer: ProtoWriter, value: PluginEvent) {}
    override fun encode(writer: ReverseProtoWriter, value: PluginEvent) {}
    override fun decode(reader: ProtoReader): PluginEvent = PluginEvent()
    override fun redact(value: PluginEvent): PluginEvent = value
}

/**
 * Subscribe events request
 */
data class SubscribeEventsRequest(
    val request_id: String = "",
    val subscriber_plugin_id: String = "",
    val event_types: List<String> = emptyList(),
    val source_plugins: List<String> = emptyList()
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<SubscribeEventsRequest> = SubscribeEventsRequestAdapter
    }
}

private object SubscribeEventsRequestAdapter : ProtoAdapter<SubscribeEventsRequest>(
    FieldEncoding.LENGTH_DELIMITED, SubscribeEventsRequest::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.SubscribeEventsRequest", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: SubscribeEventsRequest): Int = 0
    override fun encode(writer: ProtoWriter, value: SubscribeEventsRequest) {}
    override fun encode(writer: ReverseProtoWriter, value: SubscribeEventsRequest) {}
    override fun decode(reader: ProtoReader): SubscribeEventsRequest = SubscribeEventsRequest()
    override fun redact(value: SubscribeEventsRequest): SubscribeEventsRequest = value
}

/**
 * Publish event response
 */
data class PublishEventResponse(
    val request_id: String = "",
    val success: Boolean = false,
    val subscribers_notified: Int = 0
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<PublishEventResponse> = PublishEventResponseAdapter
    }
}

private object PublishEventResponseAdapter : ProtoAdapter<PublishEventResponse>(
    FieldEncoding.LENGTH_DELIMITED, PublishEventResponse::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.PublishEventResponse", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: PublishEventResponse): Int = 0
    override fun encode(writer: ProtoWriter, value: PublishEventResponse) {}
    override fun encode(writer: ReverseProtoWriter, value: PublishEventResponse) {}
    override fun decode(reader: ProtoReader): PublishEventResponse = PublishEventResponse()
    override fun redact(value: PublishEventResponse): PublishEventResponse = value
}

/**
 * Health check request
 */
data class HealthCheckRequest(
    val request_id: String = "",
    val plugin_id: String = ""
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<HealthCheckRequest> = HealthCheckRequestAdapter
    }
}

private object HealthCheckRequestAdapter : ProtoAdapter<HealthCheckRequest>(
    FieldEncoding.LENGTH_DELIMITED, HealthCheckRequest::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.HealthCheckRequest", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: HealthCheckRequest): Int = 0
    override fun encode(writer: ProtoWriter, value: HealthCheckRequest) {}
    override fun encode(writer: ReverseProtoWriter, value: HealthCheckRequest) {}
    override fun decode(reader: ProtoReader): HealthCheckRequest = HealthCheckRequest()
    override fun redact(value: HealthCheckRequest): HealthCheckRequest = value
}

/**
 * Health check response
 */
data class HealthCheckResponse(
    val request_id: String = "",
    val healthy: Boolean = false,
    val status_message: String = "",
    val diagnostics: Map<String, String> = emptyMap()
) {
    companion object {
        @JvmField
        val ADAPTER: ProtoAdapter<HealthCheckResponse> = HealthCheckResponseAdapter
    }
}

private object HealthCheckResponseAdapter : ProtoAdapter<HealthCheckResponse>(
    FieldEncoding.LENGTH_DELIMITED, HealthCheckResponse::class, "type.googleapis.com/com.augmentalis.universalrpc.plugin.HealthCheckResponse", PROTO_3, null, "plugin.proto"
) {
    override fun encodedSize(value: HealthCheckResponse): Int = 0
    override fun encode(writer: ProtoWriter, value: HealthCheckResponse) {}
    override fun encode(writer: ReverseProtoWriter, value: HealthCheckResponse) {}
    override fun decode(reader: ProtoReader): HealthCheckResponse = HealthCheckResponse()
    override fun redact(value: HealthCheckResponse): HealthCheckResponse = value
}

// Type aliases for server use
typealias PluginEventProto = PluginEvent
