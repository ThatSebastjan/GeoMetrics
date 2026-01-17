attribute vec4 a_position;
attribute float a_intensity;

varying vec2 v_off;
varying vec2 v_dim;
varying float v_intensity;

uniform mat4 u_projTrans;


void main() {
    v_dim = abs(a_position.zw);
    v_off = a_position.zw;

    vec2 pos = a_position.xy + a_position.zw;
    v_intensity = a_intensity;

    gl_Position = u_projTrans * vec4(pos, 0.0, 1.0);
}
