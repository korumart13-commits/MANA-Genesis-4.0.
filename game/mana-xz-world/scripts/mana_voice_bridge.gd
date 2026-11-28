extends RefCounted
## Adapter contract only. Actual Russian speech is supplied by the native Android MANA TTS.
signal speech_requested(text: String)
func speak(text: String) -> void:
    var clean := text.strip_edges()
    if clean.is_empty():
        return
    speech_requested.emit(clean)
    if Engine.has_singleton("ManaBrainBridge"):
        var bridge = Engine.get_singleton("ManaBrainBridge")
        if bridge.has_method("speak"):
            bridge.speak(clean)
        else:
            push_warning("ManaBrainBridge has no speak(text) method.")
    else:
        push_warning("No native MANA voice bridge is connected in this standalone Godot project.")
