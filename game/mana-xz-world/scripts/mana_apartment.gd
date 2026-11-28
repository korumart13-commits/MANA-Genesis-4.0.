extends Node3D
## Procedural dark-but-lived-in apartment; original blockout geometry.

const CompanionScript = preload("res://scripts/mana_companion.gd")

func _ready() -> void:
    _build_room()
    _build_lighting()
    _build_camera()
    _spawn_companion()

func _material(color: Color, roughness: float = 0.82, metallic: float = 0.0) -> StandardMaterial3D:
    var material := StandardMaterial3D.new()
    material.albedo_color = color
    material.roughness = roughness
    material.metallic = metallic
    return material

func _box(label: String, position: Vector3, size: Vector3, material: Material) -> MeshInstance3D:
    var instance := MeshInstance3D.new()
    instance.name = label
    var mesh := BoxMesh.new()
    mesh.size = size
    instance.mesh = mesh
    instance.position = position
    instance.material_override = material
    add_child(instance)
    return instance

func _build_room() -> void:
    var floor_mat := _material(Color("#171e2b"))
    var wall_mat := _material(Color("#101827"))
    var trim_mat := _material(Color("#26364a"))
    var fabric := _material(Color("#26364a"))
    var dark_fabric := _material(Color("#151c2a"))
    var metal := _material(Color("#596b80"), 0.34, 0.78)
    var wood := _material(Color("#342b2b"))
    var screen := _material(Color("#101c32"), 0.22, 0.12)
    var glass := _material(Color(0.10, 0.22, 0.34, 0.72), 0.12, 0.18)
    glass.transparency = BaseMaterial3D.TRANSPARENCY_ALPHA
    glass.emission_enabled = true
    glass.emission = Color("#183b5d")
    glass.emission_energy_multiplier = 0.25

    _box("Dark floor", Vector3(0, -0.12, 0), Vector3(14, 0.24, 10), floor_mat)
    _box("Back wall", Vector3(0, 1.8, -5), Vector3(14, 3.6, 0.18), wall_mat)
    _box("Left wall", Vector3(-7, 1.8, 0), Vector3(0.18, 3.6, 10), wall_mat)
    _box("Right wall", Vector3(7, 1.8, 0), Vector3(0.18, 3.6, 10), wall_mat)
    _box("Back skirting", Vector3(0, 0.12, -4.87), Vector3(13.8, 0.24, 0.08), trim_mat)
    _box("Window frame", Vector3(-2.2, 2.25, -4.82), Vector3(3.2, 1.65, 0.12), metal)
    _box("Window glass", Vector3(-2.2, 2.25, -4.74), Vector3(2.94, 1.42, 0.035), glass)
    _box("Window divider", Vector3(-2.2, 2.25, -4.69), Vector3(0.045, 1.42, 0.04), metal)
    _box("Window sill", Vector3(-2.2, 1.40, -4.72), Vector3(3.35, 0.09, 0.24), wood)

    # Bedroom: metal spring-frame bed, mattress, pillow and wardrobe.
    _box("Bed frame", Vector3(-4.3, 0.34, -1.7), Vector3(2.35, 0.20, 3.15), metal)
    _box("Mattress", Vector3(-4.3, 0.57, -1.7), Vector3(2.22, 0.34, 3.0), dark_fabric)
    _box("Bed cover", Vector3(-4.3, 0.77, -1.1), Vector3(2.18, 0.08, 1.65), fabric)
    _box("Pillow", Vector3(-4.3, 0.82, -2.82), Vector3(1.05, 0.16, 0.48), _material(Color("#aeb8c7")))
    _box("Bed headboard", Vector3(-4.3, 0.88, -3.25), Vector3(2.4, 1.0, 0.12), wood)
    _box("Wardrobe", Vector3(-6.05, 1.15, 1.35), Vector3(1.35, 2.3, 0.78), wood)
    _box("Wardrobe handle L", Vector3(-6.39, 1.15, 0.93), Vector3(0.04, 2.15, 0.04), metal)
    _box("Wardrobe handle R", Vector3(-5.72, 1.15, 0.93), Vector3(0.04, 2.15, 0.04), metal)

    # Living room: sofa, coffee table and television.
    _box("Sofa base", Vector3(3.5, 0.38, 1.75), Vector3(3.1, 0.48, 1.2), fabric)
    _box("Sofa back", Vector3(3.5, 0.92, 2.28), Vector3(3.1, 1.0, 0.22), dark_fabric)
    _box("Sofa arm L", Vector3(1.98, 0.68, 1.75), Vector3(0.22, 0.72, 1.2), fabric)
    _box("Sofa arm R", Vector3(5.02, 0.68, 1.75), Vector3(0.22, 0.72, 1.2), fabric)
    _box("Coffee table", Vector3(3.5, 0.34, -0.05), Vector3(2.1, 0.12, 0.9), wood)
    _box("TV cabinet", Vector3(3.5, 0.42, -4.35), Vector3(2.8, 0.72, 0.55), wood)
    _box("Television", Vector3(3.5, 1.55, -4.48), Vector3(2.45, 1.35, 0.12), screen)

    # Computer desk.
    _box("Computer desk", Vector3(0.2, 0.78, -3.7), Vector3(2.35, 0.14, 0.92), wood)
    _box("Monitor", Vector3(0.2, 1.42, -4.0), Vector3(1.12, 0.72, 0.10), screen)
    _box("Keyboard", Vector3(0.2, 0.90, -3.32), Vector3(0.72, 0.035, 0.25), metal)
    _box("Desk chair seat", Vector3(0.2, 0.55, -2.45), Vector3(0.72, 0.14, 0.72), dark_fabric)
    _box("Desk chair back", Vector3(0.2, 1.05, -2.78), Vector3(0.72, 0.85, 0.14), dark_fabric)

    _box("Side table", Vector3(5.55, 0.45, 2.1), Vector3(0.55, 0.9, 0.55), wood)
    _box("Plant pot", Vector3(5.55, 0.95, 2.1), Vector3(0.34, 0.42, 0.34), _material(Color("#485342")))
    _box("Ceiling light", Vector3(0, 3.48, 0), Vector3(2.2, 0.06, 0.6), _material(Color("#c5d9ef"), 0.3))

func _build_lighting() -> void:
    var world := WorldEnvironment.new()
    var environment := Environment.new()
    environment.background_mode = Environment.BG_COLOR
    environment.background_color = Color("#080f1c")
    environment.ambient_light_source = Environment.AMBIENT_SOURCE_COLOR
    environment.ambient_light_color = Color("#71829b")
    environment.ambient_light_energy = 0.32
    environment.tonemap_mode = Environment.TONE_MAPPER_FILMIC
    world.environment = environment
    add_child(world)
    var key := DirectionalLight3D.new()
    key.rotation_degrees = Vector3(-42, -28, 0)
    key.light_energy = 0.7
    key.light_color = Color("#b7c9e2")
    add_child(key)
    var warm := OmniLight3D.new()
    warm.position = Vector3(0, 2.8, 0.5)
    warm.light_color = Color("#d6a77a")
    warm.light_energy = 1.1
    warm.omni_range = 8.0
    add_child(warm)

func _build_camera() -> void:
    var camera := Camera3D.new()
    camera.position = Vector3(10.2, 7.4, 12.8)
    camera.look_at(Vector3(0, 1.0, 0), Vector3.UP)
    camera.fov = 52.0
    add_child(camera)
    camera.current = true

func _spawn_companion() -> void:
    var companion := Node3D.new()
    companion.name = "MANA"
    companion.set_script(CompanionScript)
    companion.position = Vector3(0.0, 0.0, 0.8)
    add_child(companion)
