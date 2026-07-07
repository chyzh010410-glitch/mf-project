extends Node2D

@export var tree_scene: PackedScene
@export var tree_count: int = 30
@export var spawn_area: Rect2 = Rect2(260, 180, 1100, 520)
@export var exclude_areas: Array[Rect2] = []
@export var min_distance: float = 96.0
@export var random_seed: int = 20260622
@export var spawn_on_ready: bool = true

var _rng := RandomNumberGenerator.new()
var _tree_positions: Array[Vector2] = []

func _ready() -> void:
	if spawn_on_ready:
		spawn_trees()

func spawn_trees() -> void:
	clear_trees()
	if tree_scene == null:
		push_warning("TreeSpawner: tree_scene is empty.")
		return

	_rng.seed = random_seed
	_tree_positions.clear()

	var attempts := 0
	var max_attempts := tree_count * 30
	while _tree_positions.size() < tree_count and attempts < max_attempts:
		attempts += 1
		var position := _random_position()
		if not _can_place_tree(position):
			continue

		var tree := tree_scene.instantiate() as Node2D
		tree.name = "Tree_%02d" % _tree_positions.size()
		tree.position = position
		add_child(tree)
		_tree_positions.append(position)

func clear_trees() -> void:
	for child in get_children():
		child.queue_free()

func _random_position() -> Vector2:
	return Vector2(
		_rng.randf_range(spawn_area.position.x, spawn_area.end.x),
		_rng.randf_range(spawn_area.position.y, spawn_area.end.y)
	)

func _can_place_tree(position: Vector2) -> bool:
	for area in exclude_areas:
		if area.has_point(position):
			return false

	for tree_position in _tree_positions:
		if position.distance_to(tree_position) < min_distance:
			return false

	return true
