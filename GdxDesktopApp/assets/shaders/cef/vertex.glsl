attribute vec4 a_position;
attribute vec4 a_color;
attribute vec2 a_texCoord;


varying vec4 v_color;
varying vec2 v_texCoords;

void main() {
    v_color = a_color;
    v_texCoords = a_texCoord;
    gl_Position = a_position;
}
