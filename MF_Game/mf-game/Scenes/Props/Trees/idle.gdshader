shader_type canvas_item;

uniform float strength = 1.;
uniform float speed = 1;

void vertex(){
	float offset = sin(TIME * speed);
	if(VERTEX.y < 1.) {
		VERTEX.x += offset * strength;
	}
}