extends CharacterBody2D

@onready var animated_sprite_2d: AnimatedSprite2D = %AnimatedSprite2D
@onready var node_2d: Node2D = $Node2D

func _physics_process(delta: float) -> void:
	var vector = Input.get_vector("ui_left","ui_right","ui_up","ui_down")
	if vector == Vector2.ZERO:
		animated_sprite_2d.play("idle")
	else:
		if vector.x < 0:
			node_2d.scale.x = -1
		else:
			node_2d.scale.x = 1
		animated_sprite_2d.play("move")
	velocity =vector *400
	move_and_slide()
