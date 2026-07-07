extends CharacterBody2D

@export var speed := 300
# 新增：最小跟随距离，小于这个距离就停止跟随
@export var follow_min_distance: float = 40.0
@export var player_path: NodePath = "../Player01"

@onready var player_01: CharacterBody2D = get_node_or_null(player_path) as CharacterBody2D
@onready var navigation_agent_2d: NavigationAgent2D = $Navigation/NavigationAgent2D
# 新增：绑定翻转父节点、动画节点（对应你新建的 Node2D 结构）
@onready var node_2d: Node2D = $Node2D
@onready var animated_sprite_2d: AnimatedSprite2D = $Node2D/AnimatedSprite2D

func _ready() -> void:
	if player_01 == null:
		push_warning("Companion: player_path does not point to Player01.")

func _physics_process(delta: float) -> void:
	if player_01 == null:
		return

	var direction = Vector2.ZERO
	var is_moving = false

	if not navigation_agent_2d.is_navigation_finished():
		direction = global_position.direction_to(navigation_agent_2d.get_next_path_position())
		velocity = direction * speed
		move_and_slide()
		is_moving = true
	else:
		velocity = Vector2.ZERO

	# ============ 新增：朝向翻转 + 动画逻辑（和玩家写法完全统一） ============
	if is_moving:
		# 左右水平翻转
		if direction.x < 0:
			node_2d.scale.x = -1
		else:
			node_2d.scale.x = 1
		# 移动动画，避免重复播放卡顿
		if animated_sprite_2d.animation != "move":
			animated_sprite_2d.play("move")
	else:
		# 停止切换待机
		if animated_sprite_2d.animation != "idle":
			animated_sprite_2d.play("idle")	

func _on_timer_timeout() -> void:
	if player_01 == null:
		return

	# 计算随从与玩家直线距离
	var dist = global_position.distance_to(player_01.global_position)
	# 只有距离大于最小跟随距离，才更新导航目标
	if dist > follow_min_distance:
		navigation_agent_2d.target_position = player_01.global_position
	else:
		# 近身清空目标，导航直接结束，彻底停下
		navigation_agent_2d.target_position = global_position
func _on_navigation_agent_2d_velocity_computed(safe_velocity: Vector2) -> void:
	velocity = safe_velocity
	move_and_slide()
	pass # Replace with function body.
