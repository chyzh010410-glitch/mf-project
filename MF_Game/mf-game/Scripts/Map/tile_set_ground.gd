extends Node2D

# Script is attached to TileMap, so these paths are relative to TileMap.
@onready var layer_rock: TileMapLayer = $Node/岩石
@onready var layer_ground: TileMapLayer = $Node/土地

func _input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT and event.pressed:
		print("鼠标左键按下")
		get_tile_map_data()

func get_tile_map_data() -> void:
	var mouse_position = get_global_mouse_position()

	var local_pos = layer_rock.to_local(mouse_position)
	var tile_coordinate = layer_rock.local_to_map(local_pos)
	var cell_data = layer_rock.get_cell_tile_data(tile_coordinate)
	if cell_data:
		var speed = cell_data.get_custom_data_by_layer_id(0)
		print("岩石层速度: ", speed)
		return

	local_pos = layer_ground.to_local(mouse_position)
	tile_coordinate = layer_ground.local_to_map(local_pos)
	cell_data = layer_ground.get_cell_tile_data(tile_coordinate)
	if cell_data:
		var speed = cell_data.get_custom_data_by_layer_id(0)
		print("土地层速度: ", speed)
	else:
		print("点击位置无瓦片数据")
