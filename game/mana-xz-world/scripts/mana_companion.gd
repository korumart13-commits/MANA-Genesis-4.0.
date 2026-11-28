extends Node3D
## One persistent MANA identity; mood-driven wardrobe and autonomous apartment roaming.
signal mood_changed(mood: String)
signal outfit_changed(asset_path: String)

const OUTFITS := {
    "calm": "res://assets/characters/mana_avatar_genesis.gltf",
    "playful": "res://assets/characters/mana_avatar_genesis_casual.gltf",
    "focused": "res://assets/characters/mana_avatar_genesis_no_accessory.gltf",
    "happy": "res://assets/characters/mana_avatar_genesis_evening.gltf"
}
const WAYPOINTS := [
    Vector3(-1.0, 0.0, 0.7), Vector3(-2.4, 0.0, 0.6),
    Vector3(-3.2, 0.0, -0.6), Vector3(-1.0, 0.0, -2.1),
    Vector3(1.4, 0.0, -1.2), Vector3(2.2, 0.0, 0.4),
    Vector3(4.0, 0.0, 0.1), Vector3(4.0, 0.0, 2.9),
    Vector3(0.7, 0.0, 2.4)
]
@export var movement_speed: float = 1.05
@export var mood_interval_seconds: float = 42.0
var _model: Node3D
var _animation_player: AnimationPlayer
var _target := Vector3.ZERO
var _moods := ["calm", "playful", "focused", "happy"]
var _mood_index := 0
var _life_timer: Timer

func _ready() -> void:
    randomize()
    add_to_group("mana_companion")
    _load_outfit(OUTFITS["calm"])
    _choose_target()
    _life_timer = Timer.new()
    _life_timer.wait_time = mood_interval_seconds
    _life_timer.one_shot = false
    _life_timer.timeout.connect(_on_life_cycle)
    add_child(_life_timer)
    _life_timer.start()

func _process(delta: float) -> void:
    var distance := global_position.distance_to(_target)
    if distance > 0.12:
        var direction := (_target - global_position).normalized()
        global_position = global_position.move_toward(_target, movement_speed * delta)
        if direction.length() > 0.01:
            var desired_yaw := atan2(direction.x, direction.z)
            rotation.y = lerp_angle(rotation.y, desired_yaw, minf(delta * 4.0, 1.0))
        _play_mood_animation("playful" if _moods[_mood_index] == "playful" else "calm")
    else:
        _play_mood_animation(_moods[_mood_index])
        if randf() < delta * 0.18:
            _choose_target()

func _on_life_cycle() -> void:
    _mood_index = (_mood_index + 1 + randi_range(0, 1)) % _moods.size()
    var mood: String = _moods[_mood_index]
    mood_changed.emit(mood)
    _load_outfit(OUTFITS[mood])
    _choose_target()

func _choose_target() -> void:
    var candidate: Vector3 = WAYPOINTS[randi_range(0, WAYPOINTS.size() - 1)]
    if candidate.distance_to(global_position) < 0.8:
        candidate = WAYPOINTS[(WAYPOINTS.find(candidate) + 3) % WAYPOINTS.size()]
    _target = candidate

func _load_outfit(asset_path: String) -> void:
    var packed := load(asset_path) as PackedScene
    if packed == null:
        push_warning("MANA outfit asset unavailable: " + asset_path)
        return
    if _model != null:
        _model.queue_free()
    _model = packed.instantiate() as Node3D
    if _model == null:
        push_warning("MANA glTF root is not a Node3D: " + asset_path)
        return
    add_child(_model)
    _animation_player = _model.find_child("AnimationPlayer", true, false) as AnimationPlayer
    outfit_changed.emit(asset_path)
    _play_mood_animation(_moods[_mood_index])

func _play_mood_animation(mood: String) -> void:
    if _animation_player == null:
        return
    var preferred := "Idle"
    if mood == "happy" and _animation_player.has_animation("Happy"):
        preferred = "Happy"
    elif mood == "playful" and _animation_player.has_animation("Playful"):
        preferred = "Playful"
    if _animation_player.has_animation(preferred) and _animation_player.current_animation != preferred:
        _animation_player.play(preferred)
