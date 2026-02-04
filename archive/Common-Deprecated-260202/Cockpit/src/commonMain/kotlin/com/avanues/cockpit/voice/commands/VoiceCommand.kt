package com.avanues.cockpit.voice.commands

/**
 * Sealed hierarchy of voice commands supported by Cockpit.
 */
sealed class VoiceCommand {
    
    sealed class Window : VoiceCommand() {
        data class Open(val appName: String) : Window()
        data class Close(val target: Target) : Window()
        data class Focus(val target: Target) : Window()
        object ShowAll : Window()
        data class Hide(val target: Target) : Window()
        data class Select(val index: Int) : Window()
        object Next : Window()
        object Previous : Window()
    }

    sealed class Layout : VoiceCommand() {
        object Linear : Layout()
        object Arc : Layout()
        object Grid : Layout()
        object Stack : Layout()
        object Theater : Layout()
        data class Move(val target: Target, val direction: Direction) : Layout()
        data class Resize(val target: Target, val size: SizeChange) : Layout()
        data class Pin(val target: Target) : Layout()
    }

    sealed class Visual : VoiceCommand() {
        object MinimalBorders : Visual()
        object GlassBorders : Visual()
        data class ToggleDock(val show: Boolean) : Visual()
        data class ToggleControls(val show: Boolean) : Visual()
        data class ToggleUtilities(val show: Boolean) : Visual()
    }

    sealed class Workspace : VoiceCommand() {
        data class Save(val name: String) : Workspace()
        data class Load(val name: String) : Workspace()
        object Next : Workspace()
        object DeleteCurrent : Workspace()
    }

    sealed class Content : VoiceCommand() {
        data class Read(val target: Target) : Content()
        data class Click(val elementDescription: String) : Content()
        data class Scroll(val direction: Direction) : Content()
        data class Query(val target: Target, val question: String) : Content()
    }

    // Helper types
    sealed class Target {
        object Active : Target()
        data class ByName(val name: String) : Target()
        data class ByIndex(val index: Int) : Target()
    }

    enum class Direction {
        LEFT, RIGHT, UP, DOWN, FORWARD, BACK
    }

    enum class SizeChange {
        LARGER, SMALLER, MAXIMIZE, MINIMIZE
    }
}
