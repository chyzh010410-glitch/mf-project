extends StaticBody2D

@onready var animated_sprite_2d: AnimatedSprite2D = %AnimatedSprite2D

@export var sway_speed: float = 1.2
@export var sway_degree: float = 2.5 # 放大摆动角度，肉眼明显可见
var _timer: float = 0.0

func _ready():
	if animated_sprite_2d:
		animated_sprite_2d.play("idle")
		print("idle待机动画启动成功")
	else:
		print("错误：找不到AnimatedSprite2D节点")

func _process(delta):
	_timer += delta * sway_speed
	rotation = sin(_timer) * deg_to_rad(sway_degree)
